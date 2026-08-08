package com.liymod.item;

import com.liymod.network.LoliRangeMiningSyncPayload;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Sends one bounded visual update after a complete server-side range-mining transaction. */
public final class LoliRangeMiningSync {
    private static final double OBSERVER_RANGE_SQUARED = 192.0D * 192.0D;

    private LoliRangeMiningSync() {
    }

    public static void send(ServerLevel level, BlockPos origin, List<BlockPos> changedPositions) {
        if (changedPositions.isEmpty()) {
            return;
        }
        LoliRangeMiningSyncPayload payload = new LoliRangeMiningSyncPayload(changedPositions);
        double centerX = origin.getX() + 0.5D;
        double centerY = origin.getY() + 0.5D;
        double centerZ = origin.getZ() + 0.5D;
        for (ServerPlayer observer : level.players()) {
            if (observer.distanceToSqr(centerX, centerY, centerZ) <= OBSERVER_RANGE_SQUARED
                    && ServerPlayNetworking.canSend(observer, LoliRangeMiningSyncPayload.TYPE)) {
                ServerPlayNetworking.send(observer, payload);
            }
        }
    }
}
