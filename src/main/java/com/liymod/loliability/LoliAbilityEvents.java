package com.liymod.loliability;

import com.liymod.LiyMod;
import com.liymod.protection.LoliProtection;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LoliAbilityEvents {
    private static final Identifier LEGACY_SPEED_BOOST_ID = Identifier.withDefaultNamespace(
            "49959a82-0a2e-4c3d-a8ab-2cfa74bb13d8"
    );
    private static final Set<UUID> FLIGHT_GRANTED = new HashSet<>();
    private static final Set<UUID> INVULNERABILITY_GRANTED = new HashSet<>();

    private LoliAbilityEvents() {
    }

    private static void onServerTick(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (LoliProtection.isProtected(player)) {
                applyAbilities(player);
            } else {
                removeAbilities(player);
            }
        }
    }

    private static void applyAbilities(ServerPlayer player) {
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
                }
        );
    }
}
