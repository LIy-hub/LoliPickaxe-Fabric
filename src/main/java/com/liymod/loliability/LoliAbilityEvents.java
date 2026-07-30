package com.liymod.loliability;

import com.liymod.LiyMod;
import com.liymod.protection.LoliProtection;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LoliAbilityEvents {
    private static final Identifier LEGACY_SPEED_BOOST_ID = Identifier.ofVanilla(
            "49959a82-0a2e-4c3d-a8ab-2cfa74bb13d8"
    );
    private static final Set<UUID> FLIGHT_GRANTED = new HashSet<>();
    private static final Set<UUID> INVULNERABILITY_GRANTED = new HashSet<>();

    private LoliAbilityEvents() {
    }

    private static void onServerTick(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (LoliProtection.isProtected(player)) {
                applyAbilities(player);
            } else {
                removeAbilities(player);
            }
        }
    }

    private static void applyAbilities(ServerPlayerEntity player) {
        player.setHealth(player.getMaxHealth());
        player.deathTime = 0;
        player.hurtTime = 0;
        player.timeUntilRegen = 0;
        player.fallDistance = 0.0F;
        player.setFrozenTicks(0);
        player.setAir(player.getMaxAir());
        player.extinguish();

        if (!player.getAbilities().invulnerable) {
            INVULNERABILITY_GRANTED.add(player.getUuid());
            player.getAbilities().invulnerable = true;
            player.sendAbilitiesUpdate();
        }

        if (!player.getAbilities().allowFlying) {
            FLIGHT_GRANTED.add(player.getUuid());
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
        }

        removeLegacySpeedBoost(player);
    }

    private static void removeAbilities(ServerPlayerEntity player) {
        if (INVULNERABILITY_GRANTED.remove(player.getUuid()) && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().invulnerable = false;
            player.sendAbilitiesUpdate();
        }

        if (FLIGHT_GRANTED.remove(player.getUuid()) && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }

        removeLegacySpeedBoost(player);
    }

    private static void removeLegacySpeedBoost(ServerPlayerEntity player) {
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
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
            LoliProtection.retaliate((ServerPlayerEntity) entity, source);
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
                    UUID playerId = handler.player.getUuid();
                    FLIGHT_GRANTED.remove(playerId);
                    INVULNERABILITY_GRANTED.remove(playerId);
                }
        );
    }
}
