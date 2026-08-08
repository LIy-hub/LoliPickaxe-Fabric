package com.liymod.item;

import com.liymod.entity.LoliEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class LoliDispersalItem extends Item {
    public LoliDispersalItem(Properties properties) { super(properties); }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof LoliEntity loli)) return InteractionResult.PASS;
        if (!player.level().isClientSide) loli.disperse();
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
