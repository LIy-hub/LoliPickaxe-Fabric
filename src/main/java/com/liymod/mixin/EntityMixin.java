package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import com.liymod.protection.LoliProtection;
import com.liymod.protection.TrustedPlayerLifecycle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public abstract class EntityMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof Player player && LoliProtection.blocksRemoval(player, reason)) {
            ci.cancel();
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventDirectRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof Player player && LoliProtection.blocksRemoval(player, reason)) {
            ci.cancel();
        }
    }

    @Inject(method = "discard", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventDiscard(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player
                && !self.level().isClientSide()
                && LoliProtection.isProtected(player)
                && !TrustedPlayerLifecycle.isRemovalAllowed(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "setInvulnerable", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventInvulnerabilityRemoval(boolean invulnerable, CallbackInfo ci) {
        if (!invulnerable && LoliProtection.isProtected((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "setInvisible", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventForcedInvisibility(boolean invisible, CallbackInfo ci) {
        if (invisible && LoliProtection.isProtected((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "isAttackable", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$excludeProtectedPlayerFromAttackTargets(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LoliProtection.isUntargetable((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$reportNonLivingExecutionState(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof net.minecraft.world.entity.LivingEntity)
                && LoliExecutionManager.isDeadLocked(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isRemoved", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$reportForcedRemoval(CallbackInfoReturnable<Boolean> cir) {
        if (LoliExecutionManager.shouldReportRemoved((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "unsetRemoved", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventExecutionRollback(CallbackInfo ci) {
        if (LoliExecutionManager.isDeadLocked((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}
