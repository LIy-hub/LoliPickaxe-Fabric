package com.liymod.item;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
public final class ForgeMiningRegressionTest {
    public static void main(String[] args) throws InterruptedException {
        verifyExperienceCollection();verifyRangeCooldown();
        System.out.println("FORGE_MINING_OK cooldown20ms exceptionRecovery xpOverflow mergedOrb mendingBudget=PASS");
    }
    private static void verifyExperienceCollection() throws InterruptedException {
        // A null level token keeps the accounting fixture isolated from user worlds and game input.
        require(!LoliMiningExperience.tryCollect(null, 7), "Ordinary XP awards must retain their orb behavior");
        var delivered = new ArrayList<Integer>();
        var nestedDelivered = new ArrayList<Integer>();
        var batch = LoliMiningExperience.begin(null, delivered::add);
        try (batch) {
            require(LoliMiningExperience.tryCollect(null, 3) && LoliMiningExperience.tryCollect(null, 25),
                    "Block and smelting XP must both be intercepted before orb creation");
            require(delivered.isEmpty(), "A range action must credit its accumulated XP together");
            require(LoliMiningExperience.tryCollect(null, 0) && LoliMiningExperience.tryCollect(null, -10),
                    "Empty XP awards must not escape an active batch");
            try (var nested = LoliMiningExperience.begin(null, nestedDelivered::add)) {
                LoliMiningExperience.tryCollect(null, 7);
            }
            require(nestedDelivered.equals(java.util.List.of(7)) && delivered.isEmpty(),
                    "Nested collectors must not duplicate or steal their parent's XP");
            LoliMiningExperience.tryCollect(null, 5);
            AtomicBoolean capturedOffThread = new AtomicBoolean(true);
            Thread worker = new Thread(() -> capturedOffThread.set(LoliMiningExperience.tryCollect(null, 100)));
            worker.start();
            worker.join();
            require(!capturedOffThread.get(), "An unrelated server thread's XP must remain unaffected");
        }
        batch.close();
        require(delivered.equals(java.util.List.of(33)), "Ore/smelting XP was lost, duplicated or reduced by a negative award");
        require(!LoliMiningExperience.tryCollect(null, 1), "The completed action left an active XP collector");
        try (var exceptional = LoliMiningExperience.begin(null, delivered::add)) {
            LoliMiningExperience.tryCollect(null, 11);
            throw new IllegalStateException("isolated mining exception");
        } catch (IllegalStateException expected) { }
        require(delivered.equals(java.util.List.of(33, 11)) && !LoliMiningExperience.tryCollect(null, 1),
                "An exceptional mining exit must deliver accepted XP and restore ordinary awards");
        var large = new ArrayList<Integer>();
        try (var overflow = LoliMiningExperience.begin(null, large::add)) {
            LoliMiningExperience.tryCollect(null, Integer.MAX_VALUE);
            LoliMiningExperience.tryCollect(null, Integer.MAX_VALUE);
            LoliMiningExperience.tryCollect(null, 50);
        }
        require(large.size() == 3 && large.stream().allMatch(value -> value > 0)
                        && large.stream().mapToLong(Integer::longValue).sum() == 2L * Integer.MAX_VALUE + 50,
                "Large range XP totals must not wrap, lose points or become negative");
        try (var failedReceiver = LoliMiningExperience.begin(null, amount -> {
            throw new IllegalStateException("isolated player hook failure");
        })) {
            LoliMiningExperience.tryCollect(null, 9);
        } catch (IllegalStateException expected) { }
        require(!LoliMiningExperience.tryCollect(null, 1), "A failing player hook leaked the mining collector");
        require(LoliMiningExperience.stackedPoints(7, 5) == 35
                        && LoliMiningExperience.stackedPoints(Integer.MAX_VALUE, 2) == 2L * Integer.MAX_VALUE
                        && LoliMiningExperience.stackedPoints(7, 0) == 0,
                "Nearby pickup must count every merged XP unit without integer overflow");
        require(LoliMiningExperience.afterRepair(10, 6) == 7
                        && LoliMiningExperience.afterRepair(10, 20) == 0
                        && LoliMiningExperience.afterRepair(10, 0) == 10
                        && LoliMiningExperience.afterRepair(Integer.MAX_VALUE, Integer.MAX_VALUE) == 1_073_741_824,
                "Mending must subtract only spent XP and keep large multiplication safe");
    }

    private static void verifyRangeCooldown() {
        require(LoliMiningExperience.repairBudget(Integer.MAX_VALUE,2.0F)==Integer.MAX_VALUE && LoliMiningExperience.repairBudget(Integer.MAX_VALUE/2+1,2.0F)==Integer.MAX_VALUE,"Large native Mending budget overflowed");
        require(LoliMiningExperience.afterRepair(10,5)==8 && LoliMiningExperience.afterRepair(10,1)==10,"Native odd durability rounding changed");
        require(LoliMiningExperience.afterRepair(Integer.MAX_VALUE,6)==Integer.MAX_VALUE-3,"Large partial repair debited a proportional saturated budget");
        require(LoliMiningExperience.repairBudget(10,3.0F)==30 && LoliMiningExperience.afterRepair(10,6)==7,"Forge custom repair ratio or native debit changed");
        require(LoliMiningExperience.repairBudget(10,0.5F)==5 && LoliMiningExperience.afterRepair(10,5)==8,"Forge reduced ratio changed native rounded debit");
        require(LoliMiningCooldown.INTERVAL_NANOS == 20_000_000L, "The requested range interval must be 20 ms");
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
        // A slow action must still leave 20 ms after completion, even when the old deadline has passed.
        clock.addAndGet(3_000_000_000L);
        gate.finished(player, 2);
        require(!gate.tryAcquire(player, 2), "Queued packets must not repeat immediately after a slow action");
        clock.addAndGet(LoliMiningCooldown.INTERVAL_NANOS - 1);
        require(!gate.tryAcquire(player, 2), "The completion gap ended too early");
        clock.incrementAndGet();
        require(gate.tryAcquire(player, 2), "Range mining must resume at the interval boundary");
        gate.forget(player);
        require(gate.tryAcquire(player, 2), "Rejoining must not inherit the previous connection's gate");
        gate.clear();
        require(gate.tryAcquire(player, 2), "Stopping a server must clear its cooldowns");
        LoliMiningCooldown separateSide = new LoliMiningCooldown(clock::get);
        require(separateSide.tryAcquire(player, 2), "Client and integrated-server gates must be independent");
        clock.set(Long.MAX_VALUE - LoliMiningCooldown.INTERVAL_NANOS / 2);
        gate.clear();
        require(gate.tryAcquire(player, 1), "Wrapped clock fixture failed to start");
        clock.addAndGet(LoliMiningCooldown.INTERVAL_NANOS - 1);
        require(!gate.tryAcquire(player, 1), "Monotonic clock overflow must not bypass the interval");
        clock.incrementAndGet();
        require(gate.tryAcquire(player, 1), "Clock overflow must not permanently block mining");
    }

    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
