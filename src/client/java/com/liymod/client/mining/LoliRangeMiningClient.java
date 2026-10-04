package com.liymod.client.mining;

import com.liymod.network.LoliRangeMiningSyncPayload;
import com.liymod.network.LoliPacketTasks;
import com.liymod.item.LoliFinalMiningEvents;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Applies an entire accepted range-mining result during one client task. */
public final class LoliRangeMiningClient {
    private static boolean registered;
    private static ClientLevel pendingLevel;
    private static final Set<Long> PENDING_SECTIONS = new HashSet<>();

    private LoliRangeMiningClient() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        ClientPlayNetworking.registerGlobalReceiver(
                LoliRangeMiningSyncPayload.TYPE,
                (payload, context) -> LoliPacketTasks.execute(context.client(), () -> {
                    var level = context.client().level;
                    if (level == null) {
                        return;
                    }
                    if (pendingLevel != level) PENDING_SECTIONS.clear();
                    pendingLevel = level;
                    for (var position : payload.positions()) {
                        if (level.hasChunkAt(position)) {
                            level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                        }
                    }
                    PENDING_SECTIONS.addAll(LoliMiningBatchSections.affectedSections(payload.positions()));
                })
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pendingLevel = null;
            PENDING_SECTIONS.clear();
            LoliFinalMiningEvents.resetClientCooldown();
        });
        registered = true;
    }

    /** Called during extraction on the game thread; the renderer receives an immutable snapshot. */
    public static Set<Long> takeImmediateSections(ClientLevel level) {
        Set<Long> sections = level == pendingLevel ? Set.copyOf(PENDING_SECTIONS) : Set.of();
        PENDING_SECTIONS.clear();
        pendingLevel = null;
        return sections;
    }
}
