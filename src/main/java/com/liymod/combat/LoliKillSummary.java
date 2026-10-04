package com.liymod.combat;

import com.liymod.LiyMod;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

/** Observes rewards during execution, without scanning entities or changing their drops. */
public final class LoliKillSummary {
    private static final ThreadLocal<Action> ACTIVE = new ThreadLocal<>();

    private LoliKillSummary() { }

    public static void registerEvents() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item) recordItems(level, item.getItem().getCount());
        });
    }

    /** Nested executions by the same player join the enclosing range action's summary. */
    public static Action begin(Entity attacker) {
        ServerPlayer player = attacker instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        return begin(player, player == null ? null : player.level(), counts -> {
            if (player == null) return;
            try {
                player.sendSystemMessage(message(counts));
            } catch (RuntimeException exception) {
                LiyMod.LOGGER.warn("Could not send Loli kill summary to {}", player.getUUID(), exception);
            }
        });
    }

    private static Component message(Counts counts) {
        return Component.empty()
                .append(Component.translatable("message.liymod.kill_summary.title")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.translatable("message.liymod.kill_summary",
                        number(counts.kills(), ChatFormatting.RED),
                        number(counts.items(), ChatFormatting.AQUA),
                        number(counts.experience(), ChatFormatting.GREEN))
                        .withStyle(ChatFormatting.GRAY));
    }

    private static Component number(long value, ChatFormatting color) {
        return Component.literal(Long.toString(value)).withStyle(color, ChatFormatting.BOLD);
    }

    // A recipient token and sink allow isolated accounting checks without opening a world.
    static Action begin(Object recipient, ServerLevel level, Consumer<Counts> receiver) {
        Action action = new Action(recipient, level, receiver, ACTIVE.get());
        ACTIVE.set(action);
        return action;
    }

    public static void recordItems(ServerLevel level, int count) {
        Target target = activeTarget(level);
        if (target != null && count > 0) target.items += count;
    }

    /** Observes the award once, before vanilla splits or merges it into experience orbs. */
    public static void recordExperience(ServerLevel level, int amount) {
        Target target = activeTarget(level);
        if (target != null && amount > 0) target.experience += amount;
    }

    private static Target activeTarget(ServerLevel level) {
        Action action = ACTIVE.get();
        if (action == null || action.recipient == null || action.level != level) return null;
        Target target = action.target;
        return target != null && target.eligible && !target.closed && !target.committed ? target : null;
    }

    public record Counts(long kills, long items, long experience) { }

    private static final class Totals {
        private final Set<UUID> targets = new HashSet<>();
        private long items;
        private long experience;

        private Counts snapshot() {
            return new Counts(targets.size(), items, experience);
        }
    }

    public static final class Action implements AutoCloseable {
        private final Object recipient;
        private final ServerLevel level;
        private final Consumer<Counts> receiver;
        private final Action parent;
        private final Totals totals;
        private final boolean ownsSummary;
        private Target target;
        private boolean closed;

        private Action(Object recipient, ServerLevel level, Consumer<Counts> receiver, Action parent) {
            this.recipient = recipient;
            this.level = level;
            this.receiver = receiver;
            this.parent = parent;
            ownsSummary = recipient == null || parent == null
                    || parent.recipient != recipient || parent.level != level;
            totals = ownsSummary ? new Totals() : parent.totals;
        }

        Target target(Entity entity) {
            return target(entity.getUUID(), entity instanceof LivingEntity living
                    && !living.isRemoved() && !living.isDeadOrDying());
        }

        Target target(UUID id, boolean eligible) {
            if (closed || ACTIVE.get() != this || target != null) {
                throw new IllegalStateException("Kill summary target requires an active action");
            }
            target = new Target(this, id, eligible);
            return target;
        }

        @Override
        public void close() {
            if (closed) return;
            if (ACTIVE.get() != this || target != null) {
                throw new IllegalStateException("Kill summary scopes must close in order");
            }
            closed = true;
            if (parent == null) ACTIVE.remove();
            else ACTIVE.set(parent);
            if (ownsSummary && recipient != null && !totals.targets.isEmpty()) {
                receiver.accept(totals.snapshot());
            }
        }
    }

    static final class Target implements AutoCloseable {
        private final Action action;
        private final UUID id;
        private final boolean eligible;
        private long items;
        private long experience;
        private boolean committed;
        private boolean closed;

        private Target(Action action, UUID id, boolean eligible) {
            this.action = action;
            this.id = id;
            this.eligible = eligible;
        }

        /** Only a newly completed DEAD_LOCK contributes to the message. */
        void commit() {
            if (closed || committed) return;
            committed = true;
            if (eligible && action.recipient != null && action.totals.targets.add(id)) {
                action.totals.items += items;
                action.totals.experience += experience;
            }
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            action.target = null;
        }
    }
}
