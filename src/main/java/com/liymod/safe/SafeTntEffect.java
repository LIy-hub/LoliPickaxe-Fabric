package com.liymod.safe;

import java.util.Arrays;
import java.util.Optional;

/** Safe, game-scoped replacements for the three legacy special TNT effects. */
public enum SafeTntEffect {
    BLUE_SCREEN(0, 120),
    EXIT(1, 0),
    FAIL_RESPOND(2, 160);

    private final int networkId;
    private final int durationTicks;

    SafeTntEffect(int networkId, int durationTicks) {
        this.networkId = networkId;
        this.durationTicks = durationTicks;
    }

    public int networkId() {
        return networkId;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public static Optional<SafeTntEffect> fromNetworkId(int networkId) {
        return Arrays.stream(values())
                .filter(effect -> effect.networkId == networkId)
                .findFirst();
    }
}
