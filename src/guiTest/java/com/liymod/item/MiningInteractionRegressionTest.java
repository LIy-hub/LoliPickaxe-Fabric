package com.liymod.item;

import com.liymod.network.LoliPacketTasks;
import com.liymod.client.mining.LoliMiningBatchSections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;

/** Native block shapes and event-loop ordering; never opens or edits a user world. */
public final class MiningInteractionRegressionTest {
    public static void main(String[] args) throws InterruptedException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        verifyClientAction();
        verifyRangeAndPreview();
        verifyModeLabels();
        verifySaveBeforeClose();
        verifyRangeCooldown();
        verifyBatchSections();
        System.out.println("MINING_INTERACTIONS_OK single range preview instantDispatch saveBeforeClose offThread cooldown batchSections=PASS");
    }

    private static void verifyRangeCooldown() {
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        LoliMiningCooldown gate = new LoliMiningCooldown(clock::get);
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        require(gate.tryAcquire(player, 2), "The first range action must be immediate");
        for (int index = 0; index < 20; index++) {
            require(!gate.tryAcquire(player, 5), "A held-click packet burst must not repeat range mining");
        }
        require(gate.tryAcquire(otherPlayer, 2), "One player's interval must not block another player");
        require(gate.tryAcquire(player, 0) && gate.tryAcquire(player, 0),
                "Switching to single-block mode must be immediate and remain unrestricted");
        require(!gate.tryAcquire(player, 2), "Switching tools/modes must not erase a range interval");
        // A slow action must still leave 100 ms after completion, even when the old deadline has passed.
        clock.addAndGet(3_000_000_000L);
        gate.finished(player, 2);
        require(!gate.tryAcquire(player, 2), "Queued packets must not repeat immediately after a slow action");
        clock.addAndGet(99_999_999L);
        require(!gate.tryAcquire(player, 2), "The completion gap ended too early");
        clock.incrementAndGet();
        require(gate.tryAcquire(player, 2), "Range mining must resume at the interval boundary");
        gate.forget(player);
        require(gate.tryAcquire(player, 2), "Rejoining must not inherit the previous connection's gate");
        gate.clear();
        require(gate.tryAcquire(player, 2), "Stopping a server must clear its cooldowns");
        LoliMiningCooldown separateSide = new LoliMiningCooldown(clock::get);
        require(separateSide.tryAcquire(player, 2), "Client and integrated-server gates must be independent");
        clock.set(Long.MAX_VALUE - 50_000_000L);
        gate.clear();
        require(gate.tryAcquire(player, 1), "Wrapped clock fixture failed to start");
        clock.addAndGet(99_999_999L);
        require(!gate.tryAcquire(player, 1), "Monotonic clock overflow must not bypass the interval");
        clock.incrementAndGet();
        require(gate.tryAcquire(player, 1), "Clock overflow must not permanently block mining");
    }

    private static void verifyBatchSections() {
        require(LoliMiningBatchSections.affectedSections(java.util.List.of()).isEmpty(),
                "An empty update must not force any meshes to rebuild");
        for (int coordinate : new int[]{-17, -16, -1, 0, 15, 16}) {
            var origin = new BlockPos(coordinate, coordinate, coordinate);
            for (int radius = 0; radius <= 5; radius++) {
                var affected = LoliMiningBatchSections.affectedSections(LoliMiningRange.positions(origin, radius));
                require(affected.size() <= 8, "A final-pickaxe batch must only rebuild its bounded local sections");
                for (var pos : LoliMiningRange.positions(origin, radius)) {
                    require(affected.contains(SectionPos.asLong(pos)), "A mined section was left asynchronous");
                    for (var direction : net.minecraft.core.Direction.values()) {
                        require(affected.contains(SectionPos.asLong(pos.relative(direction))),
                                "A neighboring section's newly exposed face was left asynchronous");
                    }
                }
                try {
                    affected.clear();
                    throw new AssertionError("Extracted mesh update batches must be immutable across render threads");
                } catch (UnsupportedOperationException expected) { }
            }
        }
    }

    private static void verifyClientAction() {
        for (var state : new net.minecraft.world.level.block.state.BlockState[]{
                Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.BEDROCK.defaultBlockState()
        }) {
            require(LoliFinalMiningEvents.clientAttackResult(state, false) == InteractionResult.SUCCESS,
                    "Every solid must send one immediate action instead of vanilla progressive mining");
        }
        require(LoliFinalMiningEvents.clientAttackResult(Blocks.AIR.defaultBlockState(), true) == InteractionResult.FAIL,
                "Air must not send a mining action");
        for (var fluid : new net.minecraft.world.level.block.state.BlockState[]{
                Blocks.WATER.defaultBlockState(), Blocks.LAVA.defaultBlockState()
        }) {
            require(LoliFinalMiningEvents.clientAttackResult(fluid, false) == InteractionResult.FAIL,
                    "Disabled fluid selection must not consume a fluid as a mineable target");
            require(LoliFinalMiningEvents.clientAttackResult(fluid, true) == InteractionResult.SUCCESS,
                    "Enabled fluid selection must send the same immediate action");
        }
    }

    private static void verifyRangeAndPreview() {
        BlockPos origin = new BlockPos(-17, 68, 31);
        for (int radius = 0; radius <= 5; radius++) {
            var bounds = LoliMiningRange.outline(radius).bounds();
            Set<BlockPos> positions = new HashSet<>();
            for (BlockPos position : LoliMiningRange.positions(origin, radius)) {
                require(positions.add(position.immutable()), "Duplicate server mining candidate");
                BlockPos relative = position.subtract(origin);
                require(relative.getX() >= bounds.minX && relative.getX() + 1 <= bounds.maxX
                                && relative.getY() >= bounds.minY && relative.getY() + 1 <= bounds.maxY
                                && relative.getZ() >= bounds.minZ && relative.getZ() + 1 <= bounds.maxZ,
                        "The preview must enclose every complete candidate block");
            }
            int width = radius * 2 + 1;
            require(positions.size() == width * width * width && positions.contains(origin),
                    "Wrong centered cube volume");
            require(bounds.minX == -radius && bounds.minY == -radius && bounds.minZ == -radius
                            && bounds.maxX == radius + 1 && bounds.maxY == radius + 1 && bounds.maxZ == radius + 1,
                    "Preview has a half-block or radius/diameter offset");
            if (radius == 0) {
                require(positions.equals(Set.of(origin)), "Radius zero must disable all neighboring candidates");
            }
        }
    }

    private static void verifyModeLabels() {
        var single = (TranslatableContents) LoliPickaxeItem.miningModeDescription(0).getContents();
        require(single.getKey().equals("liymod.loli_pickaxe.mining.single"), "Zero must show single-block mining");
        var range = (TranslatableContents) LoliPickaxeItem.miningModeDescription(2).getContents();
        require(range.getKey().equals("liymod.loli_pickaxe.mining.range") && range.getArgs()[0].equals(5),
                "Range mode must display the side length, not the radius");
    }

    private static void verifySaveBeforeClose() throws InterruptedException {
        NativeLoop loop = new NativeLoop();
        boolean[] menuOpen = {true};
        int[] radius = {2};
        loop.schedule(() -> loop.execute(() -> {
            if (menuOpen[0]) radius[0] = 0;
        }));
        loop.schedule(() -> menuOpen[0] = false);
        loop.drain();
        require(radius[0] == 2, "Fixture must reproduce the old reentrant save/close race");

        for (int requested : new int[]{0, 3, 0}) {
            menuOpen[0] = true;
            loop.schedule(() -> LoliPacketTasks.execute(loop, () -> {
                if (menuOpen[0]) radius[0] = requested;
            }));
            loop.schedule(() -> menuOpen[0] = false);
            loop.drain();
            require(radius[0] == requested, "Save must apply before close, including switching back to zero");
        }
        boolean[] applied = {false};
        Thread worker = new Thread(() -> LoliPacketTasks.execute(loop, () -> applied[0] = true));
        worker.start();
        worker.join();
        require(!applied[0] && loop.getPendingTasksCount() == 1, "Off-thread mutation must remain queued");
        loop.drain();
        require(applied[0], "Queued off-thread mutation never reached the server thread");
    }

    private static final class NativeLoop extends ReentrantBlockableEventLoop<Runnable> {
        private final Thread owner = Thread.currentThread();

        NativeLoop() {
            super("mining-packet-regression", false);
        }

        @Override
        public Runnable wrapRunnable(Runnable runnable) {
            return runnable;
        }

        @Override
        protected boolean shouldRun(Runnable runnable) {
            return true;
        }

        @Override
        protected Thread getRunningThread() {
            return owner;
        }

        void drain() {
            runAllTasks();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
