package com.liymod.item;

import com.liymod.LiyMod;
import com.liymod.protection.LoliProtection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative effects supplied by upgraded Small Loli Pickaxes anywhere in player inventory. */
public final class SmallLoliGameplayEvents {
    private static final Set<UUID> FLIGHT_GRANTED = new HashSet<>();

    private SmallLoliGameplayEvents() {
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering Small Loli Pickaxe gameplay events for {}", LiyMod.MOD_ID);
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers().forEach(SmallLoliGameplayEvents::synchronize));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                FLIGHT_GRANTED.remove(handler.player.getUUID()));
    }

    private static void synchronize(ServerPlayer player) {
        boolean grantsFlight = false;
        int buffLevel = 0;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof SmallLoliPickaxeItem) {
                SmallLoliPickaxeItem.refreshDerivedComponents(stack);
                grantsFlight |= SmallLoliPickaxeItem.canFly(stack);
                buffLevel = Math.max(buffLevel, SmallLoliPickaxeItem.getBuffLevel(stack));
            }
        }

        synchronizeFlight(player, grantsFlight);
        applyBuffs(player, buffLevel);
    }

    private static void synchronizeFlight(ServerPlayer player, boolean grantsFlight) {
        UUID playerId = player.getUUID();
        if (grantsFlight) {
            if (!player.getAbilities().mayfly) {
                FLIGHT_GRANTED.add(playerId);
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            return;
        }

        if (FLIGHT_GRANTED.remove(playerId)
                && !player.isCreative()
                && !player.isSpectator()
                && !LoliProtection.isProtected(player)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static void applyBuffs(ServerPlayer player, int buffLevel) {
        if (buffLevel >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 420, 0, false, false, true));
        }
        if (buffLevel >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 420, 0, false, false, true));
        }
        if (buffLevel >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 0, false, false, true));
        }
    }
}
