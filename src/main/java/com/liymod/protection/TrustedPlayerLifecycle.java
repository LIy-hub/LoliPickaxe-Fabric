package com.liymod.protection;

import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TrustedPlayerLifecycle {
    private static final ThreadLocal<Map<UUID, Integer>> REMOVAL_DEPTH =
            ThreadLocal.withInitial(HashMap::new);

    private TrustedPlayerLifecycle() {
    }

    public static void begin(PlayerEntity player) {
        REMOVAL_DEPTH.get().merge(player.getUuid(), 1, Integer::sum);
    }

    public static void end(PlayerEntity player) {
        Map<UUID, Integer> depths = REMOVAL_DEPTH.get();
        UUID playerId = player.getUuid();
        int remaining = depths.getOrDefault(playerId, 0) - 1;
        if (remaining > 0) {
            depths.put(playerId, remaining);
        } else {
            depths.remove(playerId);
        }
        if (depths.isEmpty()) {
            REMOVAL_DEPTH.remove();
        }
    }

    public static boolean isRemovalAllowed(PlayerEntity player) {
        return REMOVAL_DEPTH.get().getOrDefault(player.getUuid(), 0) > 0;
    }
}
