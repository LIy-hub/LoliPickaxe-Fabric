package com.liymod.mixin;

import com.liymod.item.LoliCardData;
import com.liymod.item.LoliCardItem;
import com.liymod.item.SmallLoliPickaxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores stack-sensitive tool and card behavior absent from pre-component Item APIs. */
@Mixin(ItemStack.class)
public abstract class LegacyItemStackMixin {
    @Inject(method="getMaxStackSize",at=@At("HEAD"),cancellable=true)
    private void liymod$cardStackSize(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack=(ItemStack)(Object)this;
        if(stack.getItem() instanceof LoliCardItem
                && (LoliCardData.art(stack).isPresent() || LoliCardData.group(stack).isPresent())) cir.setReturnValue(64);
    }
    @Inject(method="isCorrectToolForDrops",at=@At("HEAD"),cancellable=true)
    private void liymod$toolDrops(BlockState state,CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack=(ItemStack)(Object)this;
        if(stack.getItem() instanceof SmallLoliPickaxeItem item) cir.setReturnValue(item.isCorrectToolForDrops(stack,state));
    }
}
