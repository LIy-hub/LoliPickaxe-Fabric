package com.liymod.mixin;

import com.liymod.config.LogicalEnchantments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnchantmentHelper.class, priority = Integer.MAX_VALUE)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "getEnchantmentLevel(Lnet/minecraft/nbt/CompoundTag;)I", at = @At("HEAD"), cancellable = true)
    private static void liymod$readLogicalIntegerLevel(CompoundTag tag, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Mth.clamp(tag.getInt("lvl"), 0, LogicalEnchantments.MAXIMUM_LEVEL));
    }
}
