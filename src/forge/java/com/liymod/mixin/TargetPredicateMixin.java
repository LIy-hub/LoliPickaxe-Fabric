package com.liymod.mixin;

import com.liymod.protection.LoliProtection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TargetingConditions.class, priority = Integer.MAX_VALUE)
public abstract class TargetPredicateMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void liymod$excludeProtectedTarget(LivingEntity source, LivingEntity target,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (LoliProtection.isUntargetable(target)) cir.setReturnValue(false);
    }
}
