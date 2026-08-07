package com.liymod.protection;

import com.liymod.LiyMod;
import com.liymod.combat.LoliErasureService;
import com.liymod.entity.LoliEntity;
import com.liymod.item.ModItems;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import com.liymod.config.LoliServerConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LoliProtection {
    private LoliProtection() {
    }

    public static void registerProtection() {
        LiyMod.LOGGER.info(
                "Loli execution defense is always active while the pickaxe is held"
        );
    }

    public static boolean isProtected(Entity entity) {
        return entity instanceof Player player && isProtected(player);
    }

    public static boolean isProtected(Player player) {
        return !getProtectingPickaxe(player).isEmpty();
    }

    public static boolean isMainHandProtected(Player player) {
        Inventory inventory = player.getInventory();
        return inventory != null && inventory.getSelectedItem().is(ModItems.LOLI_PICKAXE);
    }

    public static ItemStack getProtectingPickaxe(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return ItemStack.EMPTY;
        }
        ItemStack selected = inventory.getSelectedItem();
        if (selected.is(ModItems.LOLI_PICKAXE)) {
            return selected;
        }
        if (!LoliServerConfig.getBoolean(LoliConfigOption.INVENTORY_PROTECTION)) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.LOLI_PICKAXE)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isExecutionImmune(Entity entity) {
        return entity instanceof LoliEntity || isProtected(entity);
    }

    public static boolean isUntargetable(Entity entity) {
        return isProtected(entity);
    }

    public static boolean blocksRemoval(Player player, Entity.RemovalReason reason) {
        return !player.level().isClientSide()
                && isProtected(player)
                && !TrustedPlayerLifecycle.isRemovalAllowed(player);
    }

    public static void retaliate(Player protectedPlayer, DamageSource source) {
        ItemStack pickaxe = getProtectingPickaxe(protectedPlayer);
        if (pickaxe.isEmpty()
                || !LoliItemSettings.getBoolean(pickaxe, LoliConfigOption.THORNS)) {
            return;
        }
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == protectedPlayer || isProtected(attacker)) {
            return;
        }

        if (LoliItemSettings.getBoolean(pickaxe, LoliConfigOption.FORCE_REMOVE)) {
            LoliErasureService.executeAbsolute(protectedPlayer, attacker);
        } else {
            LoliErasureService.execute(protectedPlayer, attacker);
        }
    }
}
