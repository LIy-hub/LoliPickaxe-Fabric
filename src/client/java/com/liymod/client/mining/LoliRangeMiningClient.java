package com.liymod.client.mining;

import com.liymod.network.LoliRangeMiningSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Applies an entire accepted range-mining result during one client task. */
public final class LoliRangeMiningClient {
    private static boolean registered;

    private LoliRangeMiningClient() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        ClientPlayNetworking.registerGlobalReceiver(
                LoliRangeMiningSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    var level = context.client().level;
                    if (level == null) {
                        return;
                    }
                    for (var position : payload.positions()) {
                        if (level.hasChunkAt(position)) {
                            level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                        }
                    }
                })
        );
        registered = true;
    }
}
