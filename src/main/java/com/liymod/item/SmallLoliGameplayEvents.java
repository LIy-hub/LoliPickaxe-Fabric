package com.liymod.item;

import com.liymod.LiyMod;
import com.liymod.protection.LoliProtection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative effects supplied by upgraded Small Loli Pickaxes anywhere in player inventory. */
public final class SmallLoliGameplayEvents {
    private static final Set<UUID> FLIGHT_GRANTED = new HashSet<>();
    private static final Set<UUID> FLIGHT_ACTIVE = new HashSet<>();
    private static final Set<UUID> COUNTERING = new HashSet<>();
    private static final Map<UUID, Double> DODGE_CHANCE = new HashMap<>();
    private static final Map<UUID, Double> ANTI_INJURY_CHANCE = new HashMap<>();

    private SmallLoliGameplayEvents() {
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering Small Loli Pickaxe gameplay events for {}", LiyMod.MOD_ID);
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers().forEach(SmallLoliGameplayEvents::synchronize));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(SmallLoliGameplayEvents::allowDamage);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player.getUUID()));
    }

    private static void synchronize(ServerPlayer player) {
        boolean grantsFlight = false;
        int buffLevel = 0;
        double dodgeChance = 0.0D;
        double antiInjuryChance = 0.0D;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof SmallLoliPickaxeItem) {
                SmallLoliPickaxeItem.refreshDerivedComponents(stack);
                SmallLoliPickaxeItem.refreshEnchantments(stack, player.level());
                grantsFlight |= SmallLoliPickaxeItem.canFly(stack);
                buffLevel = Math.max(buffLevel, SmallLoliPickaxeItem.getBuffLevel(stack));
                dodgeChance = Math.max(dodgeChance, SmallLoliPickaxeItem.getDodgeChance(stack));
                antiInjuryChance = Math.max(
                        antiInjuryChance,
                        SmallLoliPickaxeItem.getDamageReturnChance(stack)
                );
            }
        }

        synchronizeFlight(player, grantsFlight);
        applyBuffs(player, buffLevel);
        updateChance(DODGE_CHANCE, player.getUUID(), dodgeChance);
        updateChance(ANTI_INJURY_CHANCE, player.getUUID(), antiInjuryChance);
    }

    private static void synchronizeFlight(ServerPlayer player, boolean grantsFlight) {
        UUID playerId = player.getUUID();
        if (grantsFlight) {
            FLIGHT_ACTIVE.add(playerId);
            if (!player.getAbilities().mayfly) {
                FLIGHT_GRANTED.add(playerId);
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            return;
        }

        FLIGHT_ACTIVE.remove(playerId);
        if (FLIGHT_GRANTED.remove(playerId)
                && !player.isCreative()
                && !player.isSpectator()
                && !LoliProtection.isProtected(player)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static boolean allowDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player) || LoliProtection.isProtected(player)) {
            return true;
        }

        UUID playerId = player.getUUID();
        double antiInjury = ANTI_INJURY_CHANCE.getOrDefault(playerId, 0.0D);
        if (antiInjury > 0.0D && player.getRandom().nextDouble() < antiInjury) {
            counterAttack(player, source.getEntity());
        }

        if (FLIGHT_ACTIVE.contains(playerId) && source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        double dodge = DODGE_CHANCE.getOrDefault(playerId, 0.0D);
        return dodge <= 0.0D || player.getRandom().nextDouble() >= dodge;
    }

    private static void counterAttack(ServerPlayer player, Entity sourceEntity) {
        if (!(sourceEntity instanceof LivingEntity attacker)
                || sourceEntity == player
                || !attacker.isAlive()
                || !COUNTERING.add(player.getUUID())) {
            return;
        }
        try {
            player.attack(attacker);
            float healing = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5D);
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healing));
        } finally {
            COUNTERING.remove(player.getUUID());
        }
    }

    private static void updateChance(Map<UUID, Double> values, UUID playerId, double value) {
        if (value > 0.0D) {
            values.put(playerId, value);
        } else {
            values.remove(playerId);
        }
    }

    private static void clear(UUID playerId) {
        FLIGHT_GRANTED.remove(playerId);
        FLIGHT_ACTIVE.remove(playerId);
        COUNTERING.remove(playerId);
        DODGE_CHANCE.remove(playerId);
        ANTI_INJURY_CHANCE.remove(playerId);
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
