package com.liymod.combat;

import com.liymod.LiyMod;
import com.liymod.protection.LoliProtection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** One-way execution lifecycle shared by every Forge attack entry point. */
public final class LoliExecutionManager {
    private static final int PLAYER_RESPAWN_DELAY_TICKS = 1;
    private static final int NON_PLAYER_TICKET_LIFETIME = 20;
    private static final Map<UUID, LoliExecutionTicket> TICKETS = new HashMap<>();

    private LoliExecutionManager() { }

    @Nullable
    public static synchronized LoliExecutionTicket begin(Entity target, @Nullable Entity attacker,
            DamageSource source, ExecutionAuthority authority) {
        if (LoliProtection.isExecutionImmune(target) && !authority.piercesExecutionDefense()) return null;
        LoliExecutionTicket existing = TICKETS.get(target.getUUID());
        if (existing != null && existing.authority().priority() >= authority.priority()) {
            existing.bind(target);
            return existing;
        }
        LoliExecutionTicket ticket = new LoliExecutionTicket(target, attacker, source, authority);
        TICKETS.put(target.getUUID(), ticket);
        return ticket;
    }

    public static synchronized boolean abortForDefense(Entity target) {
        if (!LoliProtection.isExecutionImmune(target)) return false;
        LoliExecutionTicket active = TICKETS.get(target.getUUID());
        if (active != null && active.authority().piercesExecutionDefense()) return false;
        TICKETS.remove(target.getUUID());
        if (target instanceof ServerPlayer player) restore(player);
        return true;
    }

    public static synchronized void beginCommit(Entity target) {
        if (abortForDefense(target)) return;
        LoliExecutionTicket ticket = TICKETS.get(target.getUUID());
        if (ticket != null) ticket.beginCommit();
    }

    public static synchronized void lock(Entity target) {
        if (abortForDefense(target)) return;
        LoliExecutionTicket ticket = TICKETS.get(target.getUUID());
        if (ticket == null) return;
        ticket.lock();
        enforce(ticket, target);
    }

    public static synchronized void markDeathCommitted(Entity target) {
        LoliExecutionTicket ticket = TICKETS.get(target.getUUID());
        if (ticket != null) ticket.markDeathCommitted();
    }

    public static synchronized boolean isDeathCommitted(Entity target) {
        LoliExecutionTicket ticket = TICKETS.get(target.getUUID());
        return ticket != null && ticket.isDeathCommitted();
    }

    public static synchronized boolean isTerminal(Entity target) {
        LoliExecutionTicket ticket = TICKETS.get(target.getUUID());
        return ticket != null && ticket.isTerminal();
    }

    public static synchronized boolean isDeadLocked(Entity target) {
        LoliExecutionTicket ticket = TICKETS.get(target.getUUID());
        return ticket != null && ticket.state() == LoliExecutionTicket.State.DEAD_LOCK;
    }

    public static synchronized void completeDisconnect(ServerPlayer player) {
        LoliExecutionTicket ticket = TICKETS.remove(player.getUUID());
        if (ticket != null && ticket.isDeathCommitted()) restore(player);
    }

    public static synchronized void tick(MinecraftServer server) {
        for (LoliExecutionTicket ticket : TICKETS.values().toArray(LoliExecutionTicket[]::new)) {
            Entity target = ticket.target();
            if (target instanceof ServerPlayer) {
                ServerPlayer current = server.getPlayerList().getPlayer(ticket.targetId());
                if (current == null) { TICKETS.remove(ticket.targetId()); continue; }
                if (current != target) { ticket.bind(current); target = current; }
            }
            if (abortForDefense(target)) continue;
            if (ticket.isTerminal()) enforce(ticket, target);
            if (ticket.state() != LoliExecutionTicket.State.DEAD_LOCK) continue;
            int ticks = ticket.incrementLockedTicks();
            if (target instanceof ServerPlayer player && ticket.isDeathCommitted()
                    && ticks >= PLAYER_RESPAWN_DELAY_TICKS) {
                try {
                    ServerPlayer replacement = server.getPlayerList().respawn(player, false);
                    TICKETS.remove(ticket.targetId());
                    restore(replacement);
                } catch (RuntimeException exception) {
                    LiyMod.LOGGER.warn("Could not finish Loli respawn for {}", player.getUUID(), exception);
                }
            } else if (!(target instanceof Player) && ticks >= NON_PLAYER_TICKET_LIFETIME
                    && target.getRemovalReason() != null) {
                TICKETS.remove(ticket.targetId());
            }
        }
    }

    private static void enforce(LoliExecutionTicket ticket, Entity target) {
        if (target instanceof LivingEntity living) {
            living.setAbsorptionAmount(0.0F);
            if (ticket.state() == LoliExecutionTicket.State.DEAD_LOCK) living.deathTime = Math.max(1, living.deathTime);
        }
        if (!(target instanceof Player) && ticket.state() == LoliExecutionTicket.State.DEAD_LOCK
                && target.getRemovalReason() == null) {
            try { target.kill(); } catch (RuntimeException ignored) { }
            if (target.getRemovalReason() == null) {
                try { target.setRemoved(Entity.RemovalReason.KILLED); } catch (RuntimeException ignored) { }
            }
        }
    }

    private static void restore(ServerPlayer player) {
        float maximum = player.getMaxHealth();
        if (!Float.isFinite(maximum) || maximum <= 0.0F) maximum = 20.0F;
        player.deathTime = 0;
        player.setHealth(maximum);
        player.setAbsorptionAmount(0.0F);
    }
}
