package com.liymod.network;

import com.liymod.safe.SafeTntEffectPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.liymod.menu.PasswordWorkbenchMenu;
import com.liymod.menu.BlacklistMenu;
import com.liymod.menu.LoliStorageMenus;
import com.liymod.menu.StorageMenu;
import com.liymod.menu.FinalConfigMenu;
import com.liymod.menu.FinalToolMenus;
import com.liymod.menu.FinalEnchantmentMenu;
import com.liymod.menu.FinalEffectMenu;
import com.liymod.item.LoliFinalEnchantments;
import com.liymod.item.LoliFinalEffects;
import com.liymod.menu.FinalTeleportMenu;
import com.liymod.item.LoliTeleportService;
import com.liymod.item.LoliCardData;
import com.liymod.item.ModItems;
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
        PayloadTypeRegistry.clientboundPlay().register(
                LoliRangeMiningSyncPayload.TYPE,
                LoliRangeMiningSyncPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(StorageDropAllPayload.TYPE, StorageDropAllPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BlacklistUpdatePayload.TYPE, BlacklistUpdatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LoliMenuOpenPayload.TYPE, LoliMenuOpenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LoliItemSettingPayload.TYPE, LoliItemSettingPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LoliEnchantmentUpdatePayload.TYPE, LoliEnchantmentUpdatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LoliEffectUpdatePayload.TYPE, LoliEffectUpdatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LoliTeleportPayload.TYPE, LoliTeleportPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LoliCardOpenPayload.TYPE, LoliCardOpenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                LoliCardOnlineUpdatePayload.TYPE,
                LoliCardOnlineUpdatePayload.CODEC
        );
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
        ServerPlayNetworking.registerGlobalReceiver(LoliMenuOpenPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (!FinalToolMenus.open(context.player(), InteractionHand.MAIN_HAND, payload.mode())) {
                        FinalToolMenus.open(context.player(), InteractionHand.OFF_HAND, payload.mode());
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(LoliItemSettingPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof FinalConfigMenu menu
                            && menu.stillValid(context.player())) {
                        menu.update(payload.optionId(), payload.encodedValue());
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(LoliEnchantmentUpdatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof FinalEnchantmentMenu menu
                            && menu.stillValid(context.player())) {
                        LoliFinalEnchantments.update(
                                context.player().level(),
                                menu.getOwnerStack(),
                                payload.enchantmentId(),
                                payload.level()
                        );
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(LoliEffectUpdatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof FinalEffectMenu menu
                            && menu.stillValid(context.player())) {
                        LoliFinalEffects.update(
                                context.player().level(),
                                menu.getOwnerStack(),
                                payload.effectId(),
                                payload.level()
                        );
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(LoliTeleportPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof FinalTeleportMenu menu
                            && menu.stillValid(context.player())) {
                        LoliTeleportService.teleportRelative(
                                context.player(),
                                payload.dimensionId(),
                                payload.offsetX(),
                                payload.offsetY(),
                                payload.offsetZ()
                        );
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(LoliCardOnlineUpdatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    var stack = context.player().getItemInHand(payload.hand());
                    if (stack.is(ModItems.LOLI_CARD_ONLINE)) {
                        LoliCardData.setUrl(stack, payload.url());
                    }
                }));
        registered = true;
    }
}
