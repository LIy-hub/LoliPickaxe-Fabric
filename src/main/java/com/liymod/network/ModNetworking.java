package com.liymod.network;

import com.liymod.safe.SafeTntEffectPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.liymod.menu.PasswordWorkbenchMenu;
import com.liymod.menu.BlacklistMenu;
import com.liymod.menu.LoliStorageMenus;
import com.liymod.menu.StorageMenu;
import net.minecraft.world.InteractionHand;

/** Common payload registration. Called once from the main mod initializer. */
public final class ModNetworking {
    private static boolean registered;

    private ModNetworking() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.clientboundPlay().register(SafeTntEffectPayload.TYPE, SafeTntEffectPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PasswordUpdatePayload.TYPE, PasswordUpdatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StorageOpenPayload.TYPE, StorageOpenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StoragePagePayload.TYPE, StoragePagePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StoragePageSyncPayload.TYPE, StoragePageSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StorageDropAllPayload.TYPE, StorageDropAllPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BlacklistUpdatePayload.TYPE, BlacklistUpdatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PasswordUpdatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof PasswordWorkbenchMenu menu
                            && menu.stillValid(context.player())) {
                        menu.setPassword(payload.password());
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(StorageOpenPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (!LoliStorageMenus.open(context.player(), InteractionHand.MAIN_HAND, payload.mode())) {
                        LoliStorageMenus.open(context.player(), InteractionHand.OFF_HAND, payload.mode());
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(StoragePagePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof StorageMenu menu
                            && menu.stillValid(context.player())) {
                        menu.changePage(context.player(), payload.delta());
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(StorageDropAllPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof StorageMenu menu
                            && menu.stillValid(context.player())) {
                        menu.dropAll(context.player());
                    } else if (!LoliStorageMenus.dropAll(context.player(), InteractionHand.MAIN_HAND)) {
                        LoliStorageMenus.dropAll(context.player(), InteractionHand.OFF_HAND);
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(BlacklistUpdatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof BlacklistMenu menu
                            && menu.stillValid(context.player())) {
                        menu.updateEntry(payload.slot(), payload.clear());
                    }
                }));
        registered = true;
    }
}
