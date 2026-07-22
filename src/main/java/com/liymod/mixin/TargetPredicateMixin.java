package com.liymod.mixin;

import com.liymod.protection.LoliProtection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TargetPredicate.class, priority = Integer.MAX_VALUE)
public abstract class TargetPredicateMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$excludeProtectedPlayer(
            LivingEntity baseEntity,
            LivingEntity targetEntity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LoliProtection.isUntargetable(targetEntity)) {
            cir.setReturnValue(false);
        }
    }
}
