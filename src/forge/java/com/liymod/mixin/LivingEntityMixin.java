package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import com.liymod.protection.LoliProtection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyVariable(method = "setAbsorptionAmount", at = @At("HEAD"), argsOnly = true)
    private float liymod$blockTerminalAbsorption(float amount) {
        return LoliExecutionManager.isTerminal((LivingEntity) (Object) this) ? 0.0F : amount;
    }

    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float liymod$forceMaximumHealth(float health) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliExecutionManager.isTerminal(self)) return 0.0F;
        return LoliProtection.isExecutionImmune(self) ? liymod$safeMaximumHealth(self) : health;
    }

    @Inject(method = "getHealth", at = @At("HEAD"), cancellable = true)
    private void liymod$reportProtectedHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliExecutionManager.isTerminal(self)) cir.setReturnValue(0.0F);
        else if (LoliProtection.isExecutionImmune(self)) cir.setReturnValue(liymod$safeMaximumHealth(self));
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void liymod$blockDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliProtection.isExecutionImmune(self) && !LoliExecutionManager.isTerminal(self)) ci.cancel();
    }

    @Inject(method = "isDeadOrDying", at = @At("HEAD"), cancellable = true)
    private void liymod$keepAlive(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliExecutionManager.isTerminal(self)) cir.setReturnValue(true);
        else if (LoliProtection.isExecutionImmune(self)) cir.setReturnValue(false);
    }

    @Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
    private void liymod$keepExecutionImmuneAlive(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliExecutionManager.isTerminal(self)) cir.setReturnValue(false);
        else if (LoliProtection.isExecutionImmune(self)) cir.setReturnValue(true);
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void liymod$excludeProtectedPlayersFromRaycasts(CallbackInfoReturnable<Boolean> cir) {
        if (LoliProtection.isUntargetable((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void liymod$excludeProtectedLivingTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (LoliProtection.isUntargetable(target)) cir.setReturnValue(false);
    }

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void liymod$blockTerminalHealing(float amount, CallbackInfo ci) {
        if (LoliExecutionManager.isTerminal((LivingEntity) (Object) this)) ci.cancel();
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void liymod$blockForeignKill(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (LoliProtection.isExecutionImmune(self) && !LoliExecutionManager.isTerminal(self)) ci.cancel();
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void liymod$recordCommittedDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer)) LoliExecutionManager.markDeathCommitted(self);
    }

    private static float liymod$safeMaximumHealth(LivingEntity entity) {
        float maximum = entity.getMaxHealth();
        return Float.isFinite(maximum) && maximum > 0.0F ? maximum : 20.0F;
    }
}
