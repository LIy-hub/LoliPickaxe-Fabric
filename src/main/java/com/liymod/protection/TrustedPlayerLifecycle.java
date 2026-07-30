package com.liymod.protection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

public final class TrustedPlayerLifecycle {
    private static final ThreadLocal<Map<UUID, Integer>> REMOVAL_DEPTH =
            ThreadLocal.withInitial(HashMap::new);

    private TrustedPlayerLifecycle() {
    }

    public static void begin(Player player) {
        REMOVAL_DEPTH.get().merge(player.getUUID(), 1, Integer::sum);
    }

    public static void end(Player player) {
        Map<UUID, Integer> depths = REMOVAL_DEPTH.get();
        UUID playerId = player.getUUID();
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

    public static boolean isRemovalAllowed(Player player) {
        return REMOVAL_DEPTH.get().getOrDefault(player.getUUID(), 0) > 0;
    }
}
