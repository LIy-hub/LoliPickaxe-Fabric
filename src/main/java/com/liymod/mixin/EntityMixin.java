package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import com.liymod.entity.LoliEntity;
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
        if ((Object) this instanceof LoliEntity loli && loli.blocksRemoval(reason)) {
            ci.cancel();
            return;
        }
        if ((Object) this instanceof Player player && LoliProtection.blocksRemoval(player, reason)) {
            ci.cancel();
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventDirectRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof LoliEntity loli && loli.blocksRemoval(reason)) {
            ci.cancel();
            return;
        }
        if ((Object) this instanceof Player player && LoliProtection.blocksRemoval(player, reason)) {
            ci.cancel();
        }
    }

    @Inject(method = "discard", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventDiscard(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LoliEntity loli && !loli.isDispersalRemovalAllowed()) {
            ci.cancel();
            return;
        }
        if (self instanceof Player player
                && !self.level().isClientSide()
                && LoliProtection.isProtected(player)
                && !TrustedPlayerLifecycle.isRemovalAllowed(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "setInvulnerable", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventInvulnerabilityRemoval(boolean invulnerable, CallbackInfo ci) {
        if (!invulnerable && LoliProtection.isExecutionImmune((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "setInvisible", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventForcedInvisibility(boolean invisible, CallbackInfo ci) {
        if (invisible && LoliProtection.isExecutionImmune((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"setPos(DDD)V", "setPosRaw(DDD)V"},
            at = @At("HEAD"),
            cancellable = true
    )
    private void lolipickaxe$preventHostileOutOfWorldMove(
            double x,
            double y,
            double z,
            CallbackInfo ci
    ) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LoliEntity loli) || loli.isDispersalRemovalAllowed()) {
            return;
        }
        if (!Double.isFinite(x)
                || !Double.isFinite(y)
                || !Double.isFinite(z)
                || y < self.level().getMinY() - 64.0D
                || y > self.level().getMaxY() + 64.0D) {
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
