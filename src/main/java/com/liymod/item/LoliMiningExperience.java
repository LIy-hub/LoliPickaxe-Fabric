package com.liymod.item;

import com.liymod.mixin.accessor.ExperienceOrbAccessor;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Collects only this synchronous mining action's XP before any world orbs are created. */
public final class LoliMiningExperience {
    private static final ThreadLocal<Batch> ACTIVE = new ThreadLocal<>();

    private LoliMiningExperience() { }

    public static Batch begin(ServerPlayer player) {
        return begin(player.level(), amount -> giveDirectly(player, amount));
    }

    static Batch begin(ServerLevel level, IntConsumer receiver) {
        Batch batch = new Batch(level, receiver, ACTIVE.get());
        ACTIVE.set(batch);
        return batch;
    }

    /** The orb-spawning hook falls through for every award outside the active mining world/thread. */
    public static boolean tryCollect(ServerLevel level, int amount) {
        Batch batch = ACTIVE.get();
        if (batch == null || batch.level != level) return false;
        if (amount > 0) batch.points += amount;
        return true;
    }

    /** Picks up existing nearby stacks too; a merged orb represents count times its displayed value. */
    public static void collectNearby(ServerPlayer player, List<ExperienceOrb> orbs) {
        if (orbs.isEmpty()) return;
        try (var batch = begin(player)) {
            for (var orb : orbs) {
                if (!orb.isAlive() || orb.level() != player.level()) continue;
                long points = stackedPoints(orb.getValue(), ((ExperienceOrbAccessor) orb).liymod$getCount());
                if (points <= 0) continue;
                orb.discard();
                if (orb.isRemoved()) batch.points += points;
            }
        }
    }

    static long stackedPoints(int value, int count) {
        return value > 0 && count > 0 ? (long) value * count : 0L;
    }

    private static void giveDirectly(ServerPlayer player, int amount) {
        int remaining = amount;
        // Match vanilla pickup: use XP for damaged Mending equipment before crediting the player.
        while (remaining > 0) {
            var selected = EnchantmentHelper.getRandomItemWith(
                    EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
            if (selected.isEmpty()) break;
            var stack = selected.get().itemStack();
            int repairBudget = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.level(), stack, remaining);
            if (repairBudget <= 0) break;
            int repaired = Math.min(repairBudget, stack.getDamageValue());
            stack.setDamageValue(stack.getDamageValue() - repaired);
            remaining = afterRepair(remaining, repairBudget, repaired);
        }
        if (remaining > 0) player.giveExperiencePoints(remaining);
    }

    static int afterRepair(int experience, int repairBudget, int repaired) {
        return experience - (int) ((long) repaired * experience / repairBudget);
    }

    public static final class Batch implements AutoCloseable {
        private final ServerLevel level;
        private final IntConsumer receiver;
        private final Batch parent;
        private long points;
        private boolean closed;

        private Batch(ServerLevel level, IntConsumer receiver, Batch parent) {
            this.level = level;
            this.receiver = receiver;
            this.parent = parent;
        }

        @Override
        public void close() {
            if (closed) return;
            if (ACTIVE.get() != this) throw new IllegalStateException("Mining experience batches must close in order");
            closed = true;
            if (parent == null) ACTIVE.remove();
            else ACTIVE.set(parent);
            // Restore the previous scope before calling player hooks, including exceptional exits.
            while (points > 0) {
                int part = (int) Math.min(Integer.MAX_VALUE, points);
                points -= part;
                receiver.accept(part);
            }
        }
    }
}
