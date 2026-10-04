package com.liymod.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Checks action boundaries and reward accounting without touching a user world. */
public final class LoliKillSummaryRegressionTest {
    public static void main(String[] args) throws InterruptedException {
        verifyRangeSummary();
        verifyRejectedTargets();
        verifyIsolation();
        verifyExceptionalExit();
        verifyLargeRewards();
        System.out.println("KILL_SUMMARY_OK rangeAggregation itemQuantities xpPoints duplicateTargets rejectedTargets "
                + "zeroRewards noEmptySpam recipientIsolation threadIsolation exceptionRecovery largeRewards=PASS");
    }

    private static void verifyRangeSummary() {
        Object player = new Object();
        var messages = new ArrayList<LoliKillSummary.Counts>();
        var unexpected = new ArrayList<LoliKillSummary.Counts>();
        UUID first = UUID.randomUUID();
        var action = LoliKillSummary.begin(player, null, messages::add);
        try (action) {
            // Rewards generated outside a target's death must not be attributed to the kill.
            LoliKillSummary.recordItems(null, 999);
            LoliKillSummary.recordExperience(null, 999);
            try (var execution = LoliKillSummary.begin(player, null, unexpected::add)) {
                try (var target = execution.target(first, true)) {
                    LoliKillSummary.recordItems(null, 64);
                    LoliKillSummary.recordItems(null, 64);
                    LoliKillSummary.recordExperience(null, 5);
                    target.commit();
                    target.commit();
                    LoliKillSummary.recordItems(null, 999);
                }
            }
            require(messages.isEmpty() && unexpected.isEmpty(), "Each target must wait for the range action");
            try (var execution = LoliKillSummary.begin(player, null, unexpected::add)) {
                recordKill(execution, UUID.randomUUID(), true, true, 3, 10);
                recordKill(execution, first, true, true, 64, 5);
            }
        }
        action.close();
        require(messages.equals(List.of(new LoliKillSummary.Counts(2, 131, 15))),
                "Range must report once, count each item and exclude repeated targets/commits");
        require(unexpected.isEmpty(), "Nested executions must not send separate messages");
        try (var next = LoliKillSummary.begin(player, null, messages::add)) {
            recordKill(next, UUID.randomUUID(), true, true, 0, 0);
        }
        require(messages.size() == 2 && messages.get(1).equals(new LoliKillSummary.Counts(1, 0, 0)),
                "A valid kill with no loot must still report, without previous action's rewards");
    }

    private static void verifyRejectedTargets() {
        var messages = new ArrayList<LoliKillSummary.Counts>();
        Object player = new Object();
        try (var action = LoliKillSummary.begin(player, null, messages::add)) {
            recordKill(action, UUID.randomUUID(), true, false, 64, 500);
            recordKill(action, UUID.randomUUID(), false, true, 64, 500);
        }
        try (var empty = LoliKillSummary.begin(player, null, messages::add)) { }
        require(messages.isEmpty(), "Rejected, dead/non-living and empty actions must not send chat spam");
        try (var action = LoliKillSummary.begin(player, null, messages::add)) {
            recordKill(action, UUID.randomUUID(), true, false, 64, 500);
            recordKill(action, UUID.randomUUID(), true, true, -64, -500);
        }
        require(messages.equals(List.of(new LoliKillSummary.Counts(1, 0, 0))),
                "Rejected rewards must not bleed into a later committed kill; negatives must not subtract");
    }

    private static void verifyIsolation() throws InterruptedException {
        Object player = new Object();
        var messages = new ArrayList<LoliKillSummary.Counts>();
        var otherMessages = new ArrayList<LoliKillSummary.Counts>();
        try (var action = LoliKillSummary.begin(player, null, messages::add)) {
            try (var target = action.target(UUID.randomUUID(), true)) {
                Thread otherThread = new Thread(() -> {
                    LoliKillSummary.recordItems(null, 999);
                    LoliKillSummary.recordExperience(null, 999);
                });
                otherThread.start();
                otherThread.join();
                try (var other = LoliKillSummary.begin(new Object(), null, otherMessages::add)) {
                    recordKill(other, UUID.randomUUID(), true, true, 2, 8);
                }
                try (var unowned = LoliKillSummary.begin(null, null, otherMessages::add)) {
                    recordKill(unowned, UUID.randomUUID(), true, true, 999, 999);
                }
                LoliKillSummary.recordItems(null, 3);
                LoliKillSummary.recordExperience(null, 4);
                target.commit();
            }
        }
        require(messages.equals(List.of(new LoliKillSummary.Counts(1, 3, 4)))
                        && otherMessages.equals(List.of(new LoliKillSummary.Counts(1, 2, 8))),
                "Another thread, attacker or unowned execution must not change this player's summary");
    }

    private static void verifyExceptionalExit() {
        Object player = new Object();
        var messages = new ArrayList<LoliKillSummary.Counts>();
        try {
            try (var action = LoliKillSummary.begin(player, null, messages::add)) {
                recordKill(action, UUID.randomUUID(), true, true, 1, 5);
                try (var failed = action.target(UUID.randomUUID(), true)) {
                    LoliKillSummary.recordItems(null, 999);
                    throw new FixtureFailure();
                }
            }
        } catch (FixtureFailure expected) { }
        require(messages.equals(List.of(new LoliKillSummary.Counts(1, 1, 5))),
                "An exception must retain earlier committed kills and discard the unfinished target");
        try {
            try (var action = LoliKillSummary.begin(player, null, counts -> { throw new FixtureFailure(); })) {
                recordKill(action, UUID.randomUUID(), true, true, 1, 1);
            }
        } catch (FixtureFailure expected) { }
        LoliKillSummary.recordItems(null, 999);
        LoliKillSummary.recordExperience(null, 999);
        try (var next = LoliKillSummary.begin(player, null, messages::add)) {
            recordKill(next, UUID.randomUUID(), true, true, 2, 6);
        }
        require(messages.size() == 2 && messages.get(1).equals(new LoliKillSummary.Counts(1, 2, 6)),
                "A throwing message receiver must restore the scope before the next action");
    }

    private static void verifyLargeRewards() {
        var messages = new ArrayList<LoliKillSummary.Counts>();
        try (var action = LoliKillSummary.begin(new Object(), null, messages::add)) {
            for (int i = 0; i < 3; i++) {
                recordKill(action, UUID.randomUUID(), true, true, Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        }
        long total = 3L * Integer.MAX_VALUE;
        require(messages.equals(List.of(new LoliKillSummary.Counts(3, total, total))),
                "Large range rewards must not wrap into negative item/XP counts");
    }

    private static void recordKill(LoliKillSummary.Action action, UUID id, boolean eligible,
                                   boolean committed, int items, int xp) {
        try (var target = action.target(id, eligible)) {
            LoliKillSummary.recordItems(null, items);
            LoliKillSummary.recordExperience(null, xp);
            if (committed) target.commit();
        }
    }

    private static final class FixtureFailure extends RuntimeException { }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
