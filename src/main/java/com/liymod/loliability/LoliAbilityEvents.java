package com.liymod.loliability;

import com.liymod.LiyMod;
import com.liymod.protection.LoliProtection;
import com.liymod.combat.LoliErasureService;
import com.liymod.combat.LoliLegacyExecutionPolicy;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import com.liymod.item.LoliFinalEffects;
import com.liymod.item.LoliPickaxeItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LoliAbilityEvents {
    private static final Identifier LEGACY_SPEED_BOOST_ID = Identifier.withDefaultNamespace(
            "49959a82-0a2e-4c3d-a8ab-2cfa74bb13d8"
    );
    private static final Identifier BLOCK_REACH_ID = Identifier.fromNamespaceAndPath(
            LiyMod.MOD_ID,
            "loli_block_reach"
    );
    private static final Identifier ENTITY_REACH_ID = Identifier.fromNamespaceAndPath(
            LiyMod.MOD_ID,
            "loli_entity_reach"
    );
    private static final Set<UUID> FLIGHT_GRANTED = new HashSet<>();
    private static final Set<UUID> INVULNERABILITY_GRANTED = new HashSet<>();
    private static int tick;

    private LoliAbilityEvents() {
    }

    private static void onServerTick(net.minecraft.server.MinecraftServer server) {
        tick++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack protectingPickaxe = LoliProtection.getProtectingPickaxe(player);
            if (!protectingPickaxe.isEmpty()) {
                applyAbilities(player, protectingPickaxe);
            } else {
                removeAbilities(player);
            }
            synchronizeReach(player);
            if (tick % 5 == 0) {
                autoExecute(player);
            }
        }
    }

    private static void applyAbilities(ServerPlayer player, ItemStack pickaxe) {
        player.setHealth(player.getMaxHealth());
        player.deathTime = 0;
        player.hurtTime = 0;
        player.invulnerableTime = 0;
        player.fallDistance = 0.0F;
        player.setTicksFrozen(0);
        player.setAirSupply(player.getMaxAirSupply());
        player.clearFire();

        if (!player.getAbilities().invulnerable) {
            INVULNERABILITY_GRANTED.add(player.getUUID());
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
        }

        if (!player.getAbilities().mayfly) {
            FLIGHT_GRANTED.add(player.getUUID());
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }

        removeLegacySpeedBoost(player);
        if (tick % 100 == 0) {
            applyConfiguredEffects(player, pickaxe);
        }
        player.getFoodData().eat(20, 1.0F);
    }

    private static void removeAbilities(ServerPlayer player) {
        if (INVULNERABILITY_GRANTED.remove(player.getUUID()) && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().invulnerable = false;
            player.onUpdateAbilities();
        }

        if (FLIGHT_GRANTED.remove(player.getUUID()) && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        removeLegacySpeedBoost(player);
        removeReach(player);
    }

    private static void synchronizeReach(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof LoliPickaxeItem)) {
            removeReach(player);
            return;
        }
        double configured = LoliItemSettings.getDouble(stack, LoliConfigOption.BLOCK_REACH_DISTANCE);
        synchronizeReachAttribute(
                player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE),
                BLOCK_REACH_ID,
                configured
        );
        synchronizeReachAttribute(
                player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE),
                ENTITY_REACH_ID,
                configured
        );
    }

    private static void removeReach(ServerPlayer player) {
        removeReachModifier(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_REACH_ID);
        removeReachModifier(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_REACH_ID);
    }

    private static void synchronizeReachAttribute(
            AttributeInstance attribute,
            Identifier modifierId,
            double configured
    ) {
        if (attribute == null) {
            return;
        }
        if (configured <= 0.0D) {
            attribute.removeModifier(modifierId);
            return;
        }
        double target = attribute.getAttribute().value().sanitizeValue(configured);
        attribute.addOrUpdateTransientModifier(new AttributeModifier(
                modifierId,
                target - attribute.getBaseValue(),
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static void removeReachModifier(AttributeInstance attribute, Identifier modifierId) {
        if (attribute != null) {
            attribute.removeModifier(modifierId);
        }
    }

    private static void applyConfiguredEffects(ServerPlayer player, ItemStack pickaxe) {
        Registry<MobEffect> registry = player.level().registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        int maximum = com.liymod.config.LoliServerConfig.getInt(LoliConfigOption.EFFECT_LEVEL_LIMIT);
        if (maximum <= 0) {
            return;
        }
        LoliFinalEffects.get(pickaxe).forEach((id, level) -> registry.get(id).ifPresent(holder -> {
            int validatedLevel = Math.clamp(level, 1, maximum);
            player.addEffect(new MobEffectInstance(
                    holder,
                    410,
                    validatedLevel - 1,
                    false,
                    false,
                    true
            ));
        }));
    }

    private static void autoExecute(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof LoliPickaxeItem)
                || !LoliItemSettings.getBoolean(stack, LoliConfigOption.AUTO_KILL_RANGE_ENTITY)) {
            return;
        }
        int radius = LoliItemSettings.getInt(stack, LoliConfigOption.AUTO_KILL_RANGE);
        if (radius <= 0) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(radius);
        for (Entity target : player.level().getEntities(
                player,
                area,
                target -> LoliLegacyExecutionPolicy.permitsAutomaticRangeTarget(stack, target)
        )) {
            LoliErasureService.executeAbsolute(player, target);
        }
    }

    private static void removeLegacySpeedBoost(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(LEGACY_SPEED_BOOST_ID);
        }
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering Loli Pickaxe ability events for {}", LiyMod.MOD_ID);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!LoliProtection.isProtected(entity)) {
                return true;
            }
            LoliProtection.retaliate((ServerPlayer) entity, source);
            return false;
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!LoliProtection.isProtected(entity)) {
                return true;
            }
            entity.setHealth(entity.getMaxHealth());
            entity.deathTime = 0;
            return false;
        });
        ServerTickEvents.END_SERVER_TICK.register(LoliAbilityEvents::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> {
                    UUID playerId = handler.player.getUUID();
                    FLIGHT_GRANTED.remove(playerId);
                    INVULNERABILITY_GRANTED.remove(playerId);
                    removeReach(handler.player);
                }
        );
    }
}
