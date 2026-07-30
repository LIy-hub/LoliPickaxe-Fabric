package com.liymod.event;

import com.liymod.LiyMod;
import com.liymod.combat.LoliErasureService;
import com.liymod.item.LoliPickaxeItem;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import static com.liymod.LiyMod.MOD_ID;

public final class AttackEntityEvents {
    private AttackEntityEvents() {
    }

    private static InteractionResult onAttackEntity(
            Player player,
            Level world,
            InteractionHand hand,
            Entity target,
            @Nullable EntityHitResult hitResult
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide() && stack.getItem() instanceof LoliPickaxeItem) {
            LoliErasureService.executeAbsolute(player, target);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering attack entity events for {}", MOD_ID);
        AttackEntityCallback.EVENT.register(AttackEntityEvents::onAttackEntity);
    }
}
