package com.liymod.protection;

import com.liymod.combat.LoliErasureService;
import com.liymod.registry.ModContent;
import com.liymod.config.FinalToolSettings;
import com.liymod.config.LoliServerConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.liymod.entity.LoliEntity;
import com.liymod.combat.LoliExecutionManager;
import net.minecraft.server.level.ServerPlayer;

public final class LoliProtection {
    private LoliProtection() { }

    public static boolean isProtected(Entity entity) {
        return entity instanceof Player player && isProtected(player);
    }

    public static boolean isMainHandProtected(Entity entity) {
        return entity instanceof Player player
                && player.getInventory() != null
                && player.getMainHandItem().is(ModContent.LOLI_PICKAXE.get());
    }

    public static boolean isProtected(Player player) {
        // Entity#setPos runs from the Player superclass constructor before the
        // inventory field is initialized. Protection mixins must be safe there.
        if (player.getInventory() == null) return false;
        if (player.getMainHandItem().is(ModContent.LOLI_PICKAXE.get())) return true;
        if (!LoliServerConfig.bool("inventory_protection")) return false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) if (player.getInventory().getItem(slot).is(ModContent.LOLI_PICKAXE.get())) return true;
        return false;
    }

    public static boolean isExecutionImmune(Entity entity) {
        return entity instanceof LoliEntity || isProtected(entity);
    }

    public static boolean isUntargetable(Entity entity) {
        return isProtected(entity);
    }

    public static boolean blocksRemoval(Entity entity, Entity.RemovalReason reason) {
        if (entity instanceof LoliEntity loli) {
            return !loli.isDispersal()
                    && reason != Entity.RemovalReason.UNLOADED_TO_CHUNK
                    && reason != Entity.RemovalReason.UNLOADED_WITH_PLAYER
                    && reason != Entity.RemovalReason.CHANGED_DIMENSION;
        }
        if (!(entity instanceof Player player) || !isProtected(player)) return false;
        if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER
                || reason == Entity.RemovalReason.CHANGED_DIMENSION || LoliExecutionManager.isTerminal(entity)) return false;
        return !(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection.isAcceptingMessages();
    }

    public static void retaliate(Player player, DamageSource source) {
        Entity attacker = source.getEntity();
        ItemStack tool = protectingStack(player);
        if (FinalToolSettings.thorns(tool) && attacker != null && attacker != player && !isProtected(attacker)) {
            if (FinalToolSettings.forceRemove(tool)) LoliErasureService.executeAbsolute(player, attacker);
            else LoliErasureService.execute(player, attacker);
        }
    }

    public static ItemStack protectingStack(Player player) {
        if (player.getInventory() == null) return ItemStack.EMPTY;
        if (player.getMainHandItem().is(ModContent.LOLI_PICKAXE.get())) return player.getMainHandItem();
        if (!LoliServerConfig.bool("inventory_protection")) return ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModContent.LOLI_PICKAXE.get())) return stack;
        }
        return ItemStack.EMPTY;
    }
}
