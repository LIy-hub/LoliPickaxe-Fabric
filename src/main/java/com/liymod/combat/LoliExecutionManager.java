package com.liymod.combat;

import com.liymod.LiyMod;
import com.liymod.mixin.accessor.EntityAccessor;
import com.liymod.mixin.accessor.LivingEntityAccessor;
import com.liymod.protection.LoliProtection;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LoliExecutionManager {
    private static final int PLAYER_RESPAWN_DELAY_TICKS = 1;
    private static final int NON_PLAYER_TICKET_LIFETIME = 20;
    private static final Map<UUID, LoliExecutionTicket> TICKETS = new HashMap<>();

    private LoliExecutionManager() {
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering universal Loli execution enforcement");
        ServerTickEvents.END_SERVER_TICK.register(LoliExecutionManager::enforceAll);
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> completeDisconnect(handler.player)
        );
    }

    @Nullable
    public static LoliExecutionTicket begin(
            Entity target,
            @Nullable Entity attacker,
            DamageSource source,
            ExecutionAuthority authority
    ) {
        if (LoliProtection.isExecutionImmune(target)
                && !authority.piercesExecutionDefense()) {
            return null;
        }

        LoliExecutionTicket existing = TICKETS.get(target.getUuid());
        if (existing != null
                && existing.authority().priority() >= authority.priority()) {
            existing.bind(target);
            return existing;
        }

        LoliExecutionTicket ticket = new LoliExecutionTicket(
                target,
                attacker,
                source,
                authority
        );
        TICKETS.put(target.getUuid(), ticket);
        if (existing != null) {
            LiyMod.LOGGER.info(
                    "Superseded {} execution ticket with {} authority for {}",
                    existing.authority(),
                    authority,
                    target.getUuid()
            );
        }
        return ticket;
    }

    public static boolean abortForDefense(Entity target) {
        if (!LoliProtection.isExecutionImmune(target)) {
            return false;
        }

        LoliExecutionTicket active = TICKETS.get(target.getUuid());
        if (active != null && active.authority().piercesExecutionDefense()) {
            return false;
        }

        LoliExecutionTicket removed = TICKETS.remove(target.getUuid());
        if (removed != null && target instanceof ServerPlayerEntity player) {
            restoreDefendedPlayer(player);
            LiyMod.LOGGER.info(
                    "Loli defense revoked execution ticket for {} in state {}",
                    player.getGameProfile().name(),
                    removed.state()
            );
        }
        return true;
    }

    public static void beginCommit(Entity target) {
        if (abortForDefense(target)) {
            return;
        }

        LoliExecutionTicket ticket = TICKETS.get(target.getUuid());
        if (ticket == null) {
            return;
        }

        ticket.beginCommit();
        forceCommittingState(target);
    }

    public static void lock(Entity target) {
        if (abortForDefense(target)) {
            return;
        }

        LoliExecutionTicket ticket = TICKETS.get(target.getUuid());
        if (ticket == null) {
            return;
        }

        ticket.lock();
        enforce(ticket, target);
    }

    public static boolean isTerminal(Entity entity) {
        LoliExecutionTicket ticket = TICKETS.get(entity.getUuid());
        return ticket != null
                && ticket.isTerminal()
                && !isBlockedByDefense(entity, ticket);
    }

    public static boolean isDeadLocked(Entity entity) {
        LoliExecutionTicket ticket = TICKETS.get(entity.getUuid());
        return ticket != null
                && ticket.state() == LoliExecutionTicket.State.DEAD_LOCK
                && !isBlockedByDefense(entity, ticket);
    }

    private static boolean isBlockedByDefense(
            Entity entity,
            LoliExecutionTicket ticket
    ) {
        return LoliProtection.isExecutionImmune(entity)
                && !ticket.authority().piercesExecutionDefense();
    }

    public static boolean shouldReportRemoved(Entity entity) {
        return !(entity instanceof PlayerEntity) && isDeadLocked(entity);
    }

    public static void markVanillaDeathCommitted(LivingEntity entity) {
        markDeathCommitted(entity);
    }

    public static void markDeathCommitted(Entity entity) {
        if (abortForDefense(entity)) {
            return;
        }

        LoliExecutionTicket ticket = TICKETS.get(entity.getUuid());
        if (ticket != null) {
            ticket.markDeathCommitted();
        }
    }

    public static boolean isDeathCommitted(Entity entity) {
        LoliExecutionTicket ticket = TICKETS.get(entity.getUuid());
        return ticket != null && ticket.isDeathCommitted();
    }

    public static void completeRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        LoliExecutionTicket ticket = TICKETS.get(oldPlayer.getUuid());
        if (ticket != null && ticket.isDeathCommitted()) {
            TICKETS.remove(oldPlayer.getUuid());
            normalizeCompletedPlayer(newPlayer);
            refreshRemotePlayerTracking(newPlayer);
            LiyMod.LOGGER.info(
                    "Completed Loli execution lifecycle for {} (entity {} -> {})",
                    oldPlayer.getGameProfile().name(),
                    oldPlayer.getId(),
                    newPlayer.getId()
            );
        }
    }

    private static void refreshRemotePlayerTracking(ServerPlayerEntity player) {
        EntitiesDestroyS2CPacket removeStaleEntity = new EntitiesDestroyS2CPacket(player.getId());
        for (ServerPlayerEntity observer : player.getEntityWorld().getServer()
                .getPlayerManager().getPlayerList()) {
            if (observer != player) {
                observer.networkHandler.sendPacket(removeStaleEntity);
            }
        }

        ServerWorld world = player.getEntityWorld();
        world.getChunkManager().unloadEntity(player);
        world.getChunkManager().loadEntity(player);
        LiyMod.LOGGER.info(
                "Refreshed remote tracking for respawned player {} (entity {})",
                player.getGameProfile().name(),
                player.getId()
        );
    }

    public static void completeDisconnect(ServerPlayerEntity player) {
        LoliExecutionTicket ticket = TICKETS.remove(player.getUuid());
        if (ticket == null || !ticket.isDeathCommitted()) {
            return;
        }

        normalizeCompletedPlayer(player);
        LiyMod.LOGGER.info(
                "Completed disconnected Loli execution lifecycle for {}",
                player.getGameProfile().name()
        );
    }

    public static void forceRemoval(Entity entity) {
        if (entity instanceof PlayerEntity
                || entity.getRemovalReason() != null) {
            return;
        }

        Entity.RemovalReason reason = Entity.RemovalReason.KILLED;
        try {
            entity.remove(reason);
        } catch (RuntimeException exception) {
            LiyMod.LOGGER.warn("Normal removal failed for executed entity {}", entity.getUuid(), exception);
        }

        if (entity.getRemovalReason() == null) {
            EntityAccessor accessor = (EntityAccessor) entity;
            entity.stopRiding();
            entity.getPassengerList().forEach(Entity::stopRiding);
            accessor.lolipickaxe$setRemovalReason(reason);
            accessor.lolipickaxe$getChangeListener().remove(reason);
        }
    }

    private static void enforceAll(MinecraftServer server) {
        for (LoliExecutionTicket ticket : TICKETS.values().toArray(LoliExecutionTicket[]::new)) {
            Entity target = ticket.target();
            if (target instanceof ServerPlayerEntity) {
                ServerPlayerEntity current = server.getPlayerManager().getPlayer(ticket.targetId());
                if (current == null) {
                    TICKETS.remove(ticket.targetId());
                    continue;
                }
                if (current != target) {
                    ticket.bind(current);
                    target = current;
                }
            }

            if (abortForDefense(target)) {
                continue;
            }

            if (ticket.isTerminal()) {
                enforce(ticket, target);
            }

            if (ticket.state() == LoliExecutionTicket.State.DEAD_LOCK) {
                int lockedTicks = ticket.incrementLockedTicks();
                if (target instanceof ServerPlayerEntity player
                        && ticket.isDeathCommitted()
                        && lockedTicks >= PLAYER_RESPAWN_DELAY_TICKS) {
                    completePlayerLifecycle(server, ticket, player);
                    continue;
                }

                if (!(target instanceof PlayerEntity)
                        && lockedTicks >= NON_PLAYER_TICKET_LIFETIME
                        && target.getRemovalReason() != null) {
                    TICKETS.remove(ticket.targetId());
                }
            }
        }
    }

    private static void completePlayerLifecycle(
            MinecraftServer server,
            LoliExecutionTicket ticket,
            ServerPlayerEntity player
    ) {
        if (abortForDefense(player)) {
            return;
        }

        try {
            ServerPlayerEntity replacement = server.getPlayerManager().respawnPlayer(
                    player,
                    false,
                    Entity.RemovalReason.KILLED
            );
            if (replacement == null) {
                LiyMod.LOGGER.warn(
                        "Server returned no replacement while completing Loli execution for {}",
                        player.getGameProfile().name()
                );
                return;
            }

            completeRespawn(player, replacement);
        } catch (RuntimeException exception) {
            LiyMod.LOGGER.warn(
                    "Server-side respawn failed while completing Loli execution for {} at locked tick {}",
                    player.getGameProfile().name(),
                    ticket.lockedTicks(),
                    exception
            );
        }
    }

    private static void normalizeCompletedPlayer(ServerPlayerEntity player) {
        float maximumHealth = player.getMaxHealth();
        if (!Float.isFinite(maximumHealth) || maximumHealth <= 0.0F) {
            maximumHealth = 20.0F;
        }

        ((LivingEntityAccessor) player).lolipickaxe$setDead(false);
        player.deathTime = 0;
        player.getDataTracker().set(
                LivingEntityAccessor.lolipickaxe$getHealthTrackedData(),
                maximumHealth
        );
        player.setAbsorptionAmount(0.0F);
    }

    private static void restoreDefendedPlayer(ServerPlayerEntity player) {
        float maximumHealth = player.getMaxHealth();
        if (!Float.isFinite(maximumHealth) || maximumHealth <= 0.0F) {
            maximumHealth = 20.0F;
        }

        ((LivingEntityAccessor) player).lolipickaxe$setDead(false);
        player.deathTime = 0;
        player.getDataTracker().set(
                LivingEntityAccessor.lolipickaxe$getHealthTrackedData(),
                maximumHealth
        );
        player.networkHandler.sendPacket(
                new HealthUpdateS2CPacket(
                        maximumHealth,
                        player.getHungerManager().getFoodLevel(),
                        player.getHungerManager().getSaturationLevel()
                )
        );
    }

    private static void forceCommittingState(Entity target) {
        if (target instanceof LivingEntity living) {
            living.getDataTracker().set(LivingEntityAccessor.lolipickaxe$getHealthTrackedData(), 0.0F);
            living.setAbsorptionAmount(0.0F);
        }
    }

    private static void enforce(LoliExecutionTicket ticket, Entity target) {
        forceCommittingState(target);
        if (target instanceof LivingEntity living
                && ticket.state() == LoliExecutionTicket.State.DEAD_LOCK) {
            ((LivingEntityAccessor) living).lolipickaxe$setDead(true);
            living.deathTime = Math.max(living.deathTime, 1);
        }

        if (!(target instanceof PlayerEntity)
                && ticket.state() == LoliExecutionTicket.State.DEAD_LOCK) {
            forceRemoval(target);
        }
    }
}
