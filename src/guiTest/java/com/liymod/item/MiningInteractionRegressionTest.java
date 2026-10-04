package com.liymod.item;

import com.liymod.network.LoliPacketTasks;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
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
        System.out.println("MINING_INTERACTIONS_OK single range preview instantDispatch saveBeforeClose offThread=PASS");
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
