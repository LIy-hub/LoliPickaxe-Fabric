package com.liymod.protection;

import com.liymod.LiyMod;
import com.liymod.combat.LoliErasureService;
import com.liymod.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;

public final class LoliProtection {
    private LoliProtection() {
    }

    public static void registerProtection() {
        LiyMod.LOGGER.info(
                "Loli execution defense is always active while the pickaxe is held"
        );
    }

    public static boolean isProtected(Entity entity) {
        return entity instanceof PlayerEntity player && isProtected(player);
    }

    public static boolean isProtected(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        return inventory != null && inventory.getMainHandStack().isOf(ModItems.LOLI_PICKAXE);
    }

    public static boolean isExecutionImmune(Entity entity) {
        return isProtected(entity);
    }

    public static boolean isUntargetable(Entity entity) {
        return isProtected(entity);
    }

    public static boolean blocksRemoval(PlayerEntity player, Entity.RemovalReason reason) {
        return !player.getWorld().isClient
                && isProtected(player)
                && !TrustedPlayerLifecycle.isRemovalAllowed(player);
    }

    public static void retaliate(PlayerEntity protectedPlayer, DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker == null || attacker == protectedPlayer || isProtected(attacker)) {
            return;
        }

        LoliErasureService.execute(protectedPlayer, attacker);
    }
}
