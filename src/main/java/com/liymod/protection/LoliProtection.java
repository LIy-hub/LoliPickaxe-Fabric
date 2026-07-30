package com.liymod.protection;

import com.liymod.LiyMod;
import com.liymod.combat.LoliErasureService;
import com.liymod.item.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

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
        Inventory inventory = player.getInventory();
        return inventory != null && inventory.getSelectedItem().is(ModItems.LOLI_PICKAXE);
    }

    public static boolean isExecutionImmune(Entity entity) {
        return isProtected(entity);
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
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == protectedPlayer || isProtected(attacker)) {
            return;
        }

        LoliErasureService.execute(protectedPlayer, attacker);
    }
}
