package com.liymod.mixin;

import com.liymod.protection.LoliProtection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void liymod$blockForeignRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (LoliProtection.blocksRemoval(self, reason)) {
            ci.cancel();
        }
    }

    @Inject(method = "isRemoved", at = @At("HEAD"), cancellable = true)
    private void liymod$keepProtectedIndexed(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (LoliProtection.isProtected(self) && !com.liymod.combat.LoliExecutionManager.isDeadLocked(self)) cir.setReturnValue(false);
    }

    @Inject(method = {"setPos(DDD)V", "setPosRaw(DDD)V"}, at = @At("HEAD"), cancellable = true)
    private void liymod$blockInvalidPosition(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (LoliProtection.isProtected(self)
                && (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 29_999_984.0D || Math.abs(z) > 29_999_984.0D
                || y < self.level().getMinBuildHeight() - 64.0D
                || y > self.level().getMaxBuildHeight() + 64.0D)) {
            ci.cancel();
        }
    }

    @Inject(method = "setInvulnerable", at = @At("HEAD"), cancellable = true)
    private void liymod$keepProtectedInvulnerable(boolean invulnerable, CallbackInfo ci) {
        if (!invulnerable && LoliProtection.isExecutionImmune((Entity) (Object) this)) ci.cancel();
    }

    @Inject(method = "setInvisible", at = @At("HEAD"), cancellable = true)
    private void liymod$preventForcedInvisibility(boolean invisible, CallbackInfo ci) {
        if (invisible && LoliProtection.isExecutionImmune((Entity) (Object) this)) ci.cancel();
    }

    @Inject(method = "isAttackable", at = @At("HEAD"), cancellable = true)
    private void liymod$excludeProtectedAttackTarget(CallbackInfoReturnable<Boolean> cir) {
        if (LoliProtection.isUntargetable((Entity) (Object) this)) cir.setReturnValue(false);
    }
}
