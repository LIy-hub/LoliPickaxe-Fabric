package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import com.liymod.protection.LoliProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerEntity.class, priority = Integer.MAX_VALUE)
public abstract class PlayerMixin extends LivingEntity {
    @Shadow
    public int experienceLevel;

    @Shadow
    public abstract void sendAbilitiesUpdate();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void liymod$preventDamageAndRetaliate(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (getWorld().isClient || !LoliProtection.isProtected((PlayerEntity) (Object) this)) {
            return;
        }

        LoliProtection.retaliate((PlayerEntity) (Object) this, source);
        cir.setReturnValue(false);
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void liymod$preventDeath(DamageSource source, CallbackInfo ci) {
        if (!getWorld().isClient && LoliProtection.isProtected((PlayerEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventPlayerRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (LoliProtection.blocksRemoval(self, reason)) {
            ci.cancel();
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$blockAttackWhileDead(Entity target, CallbackInfo ci) {
        if (LoliExecutionManager.isDeadLocked((PlayerEntity) (Object) this)
                || LoliProtection.isUntargetable(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "isCreativeLevelTwoOp", at = @At("HEAD"), cancellable = true)
    private void liymod$grantOperatorPrivileges(CallbackInfoReturnable<Boolean> cir) {
        if (LoliProtection.isProtected((PlayerEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getLuck", at = @At("HEAD"), cancellable = true)
    private void liymod$boostLuck(CallbackInfoReturnable<Float> cir) {
        if (LoliProtection.isProtected((PlayerEntity) (Object) this)) {
            cir.setReturnValue(16384.0F);
        }
    }

    @Inject(method = "addExperienceLevels", at = @At("HEAD"), cancellable = true)
    private void liymod$boostExperience(int levels, CallbackInfo ci) {
        if (LoliProtection.isProtected((PlayerEntity) (Object) this)) {
            experienceLevel = 142857;
            sendAbilitiesUpdate();
            ci.cancel();
        }
    }

}
