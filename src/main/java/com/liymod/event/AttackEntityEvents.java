package com.liymod.event;

import com.liymod.LiyMod;
import com.liymod.combat.LoliErasureService;
import com.liymod.item.LoliPickaxeItem;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static com.liymod.LiyMod.MOD_ID;

public final class AttackEntityEvents {
    private AttackEntityEvents() {
    }

    private static ActionResult onAttackEntity(
            PlayerEntity player,
            World world,
            Hand hand,
            Entity target,
            @Nullable EntityHitResult hitResult
    ) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient && stack.getItem() instanceof LoliPickaxeItem) {
            LoliErasureService.executeAbsolute(player, target);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering attack entity events for {}", MOD_ID);
        AttackEntityCallback.EVENT.register(AttackEntityEvents::onAttackEntity);
    }
}
