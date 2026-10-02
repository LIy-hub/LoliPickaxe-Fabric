package com.liymod;

import com.liymod.config.LoliConfigOption;
import com.liymod.safe.SafeTntEffect;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Dependency-free regression checks for server input and safe-effect contracts. */
public final class ConfigurationRegressionTest {
    private static int checks;

    public static void main(String[] args) {
        equal(5, LoliConfigOption.MINING_RADIUS.parse("2147483647"));
        equal(0, LoliConfigOption.MINING_RADIUS.parse("-1"));
        equal(32768, LoliConfigOption.ENCHANTMENT_LEVEL_LIMIT.parse("65536"));
        equal(4096.0D, LoliConfigOption.MAX_TELEPORT_DISTANCE.parse("99999"));
        equal(1.0D, LoliConfigOption.MAX_TELEPORT_DISTANCE.parse("-5"));
        equal(true, LoliConfigOption.AUTO_ACCEPT.parse("TRUE"));
        rejects(() -> LoliConfigOption.AUTO_ACCEPT.parse("1"));
        rejects(() -> LoliConfigOption.BLOCK_REACH_DISTANCE.parse("NaN"));
        rejects(() -> LoliConfigOption.BLOCK_REACH_DISTANCE.parse("Infinity"));
        rejects(() -> LoliConfigOption.KICK_MESSAGE.parse("x".repeat(161)));
        rejects(() -> LoliConfigOption.MINING_RADIUS.parse(null));
        equal(0, LoliConfigOption.MINING_RADIUS.sanitize("not-an-integer"));
        equal(1024.0D, LoliConfigOption.BLOCK_REACH_DISTANCE.sanitize("NaN"));
        equal("Alice,Bob", LoliConfigOption.REINCARNATION_LIST.parse(" Alice,alice,Bob "));
        equal("", LoliConfigOption.REINCARNATION_LIST.parse("   "));
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        equal(uuid, LoliConfigOption.REINCARNATION_LIST.parse(uuid + "," + uuid.toUpperCase()));
        rejects(() -> LoliConfigOption.REINCARNATION_LIST.parse("Alice,,Bob"));
        rejects(() -> LoliConfigOption.REINCARNATION_LIST.parse("not a player"));
        rejects(() -> LoliConfigOption.REINCARNATION_LIST.parse(IntStream.range(0, 25)
                .mapToObj(i -> "Player" + i).collect(Collectors.joining(","))));
        equal(LoliConfigOption.MINING_RADIUS, LoliConfigOption.byId("MINING_RADIUS").orElseThrow());
        equal(true, LoliConfigOption.byId(null).isEmpty());
        equal(true, LoliConfigOption.byId("x".repeat(65)).isEmpty());
        equal(LoliConfigOption.values().length, new HashSet<>(Arrays.stream(LoliConfigOption.values())
                .map(LoliConfigOption::id).toList()).size());
        for (LoliConfigOption option : LoliConfigOption.values()) {
            equal(option.defaultValue(), option.parse(option.encode(option.defaultValue())));
        }
        for (LoliConfigOption option : new LoliConfigOption[] {
                LoliConfigOption.FORCE_REMOVE, LoliConfigOption.CLEAR_INVENTORY,
                LoliConfigOption.DROP_EQUIPMENT, LoliConfigOption.KICK_PLAYER,
                LoliConfigOption.REINCARNATION, LoliConfigOption.SOUL_REDEMPTION,
                LoliConfigOption.SAFE_ATTACK_COMMAND, LoliConfigOption.SAFE_BLUE_SCREEN,
                LoliConfigOption.SAFE_EXIT, LoliConfigOption.SAFE_FAIL_RESPOND}) {
            equal(false, option.defaultValue());
        }
        for (SafeTntEffect effect : SafeTntEffect.values()) {
            equal(effect, SafeTntEffect.fromNetworkId(effect.networkId()).orElseThrow());
        }
        equal(true, SafeTntEffect.fromNetworkId(-1).isEmpty());
        equal(true, SafeTntEffect.fromNetworkId(3).isEmpty());
        equal(120, SafeTntEffect.BLUE_SCREEN.durationTicks());
        equal(0, SafeTntEffect.EXIT.durationTicks());
        equal(160, SafeTntEffect.FAIL_RESPOND.durationTicks());
        System.out.println("CONFIGURATION_REGRESSION_OK checks=" + checks);
    }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void rejects(Runnable action) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected invalid input to be rejected");
    }
}
