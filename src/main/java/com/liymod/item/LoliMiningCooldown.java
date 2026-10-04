package com.liymod.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Range actions leave a short gap; single-block actions never consume this gate. */
final class LoliMiningCooldown {
    static final long INTERVAL_NANOS = 20_000_000L;
    private final Map<UUID, Long> nextAllowed = new HashMap<>();
    private final LongSupplier clock;

    LoliMiningCooldown() {
        this(System::nanoTime);
    }

    LoliMiningCooldown(LongSupplier clock) {
        this.clock = clock;
    }

    boolean tryAcquire(UUID player, int radius) {
        if (radius == 0) return true;
        long now = clock.getAsLong();
        Long deadline = nextAllowed.get(player);
        if (deadline != null && now - deadline < 0) return false;
        nextAllowed.put(player, now + INTERVAL_NANOS);
        return true;
    }

    void finished(UUID player, int radius) {
        if (radius > 0) nextAllowed.put(player, clock.getAsLong() + INTERVAL_NANOS);
    }

    void forget(UUID player) {
        nextAllowed.remove(player);
    }

    void clear() {
        nextAllowed.clear();
    }
}
