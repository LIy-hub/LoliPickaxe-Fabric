package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import com.liymod.protection.LoliProtection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class LivingEntityMixin {
    @ModifyVariable(method = "setAbsorptionAmount", at = @At("HEAD"), argsOnly = true)
    private float lolipickaxe$blockPlayerAbsorptionWhileDead(float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity && LoliExecutionManager.isTerminal(self)) {
            return 0.0F;
        }
        return amount;
    }

    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float lolipickaxe$forceMaximumHealth(float requestedHealth) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliExecutionManager.isTerminal(self)) {
            return 0.0F;
        }
        if (LoliProtection.isProtected(self)) {
            return lolipickaxe$safeMaximumHealth(self);
        }
        return requestedHealth;
    }

    @Inject(method = "getHealth", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$reportMaximumHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliExecutionManager.isTerminal(self)) {
            cir.setReturnValue(0.0F);
        } else if (LoliProtection.isProtected(self)) {
            cir.setReturnValue(lolipickaxe$safeMaximumHealth(self));
        }
    }

    @Inject(method = "isDead", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventDeadState(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliExecutionManager.isTerminal(self)) {
            cir.setReturnValue(true);
        } else if (LoliProtection.isProtected(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$enforceAliveState(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliExecutionManager.isTerminal(self)) {
            cir.setReturnValue(false);
        } else if (LoliProtection.isProtected(self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$excludeProtectedPlayerFromRaycasts(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LoliProtection.isUntargetable((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lolipickaxe$excludeProtectedPlayerFromLivingTargets(
            LivingEntity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LoliProtection.isUntargetable(target)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventExecutionHealing(float amount, CallbackInfo ci) {
        if (LoliExecutionManager.isTerminal((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventKill(CallbackInfo ci) {
        if (LoliProtection.isProtected((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventLivingDeath(DamageSource source, CallbackInfo ci) {
        if (LoliProtection.isProtected((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "onDeath", at = @At("RETURN"))
    private void lolipickaxe$recordLivingDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity)) {
            LoliExecutionManager.markVanillaDeathCommitted(self);
        }
    }

    private static float lolipickaxe$safeMaximumHealth(LivingEntity entity) {
        float maximumHealth = entity.getMaxHealth();
        return Float.isFinite(maximumHealth) && maximumHealth > 0.0F ? maximumHealth : 20.0F;
    }
}
