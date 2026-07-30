package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import com.liymod.protection.LoliProtection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = Integer.MAX_VALUE)
public abstract class PlayerMixin extends LivingEntity {
    @Shadow
    public int experienceLevel;

    @Shadow
    public abstract void onUpdateAbilities();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void liymod$preventDamageAndRetaliate(
            ServerLevel serverLevel,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (level().isClientSide() || !LoliProtection.isProtected((Player) (Object) this)) {
            return;
        }

        LoliProtection.retaliate((Player) (Object) this, source);
        cir.setReturnValue(false);
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void liymod$preventDeath(DamageSource source, CallbackInfo ci) {
        if (!level().isClientSide() && LoliProtection.isProtected((Player) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventPlayerRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (LoliProtection.blocksRemoval(self, reason)) {
            ci.cancel();
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$blockAttackWhileDead(Entity target, CallbackInfo ci) {
        if (LoliExecutionManager.isDeadLocked((Player) (Object) this)
                || LoliProtection.isUntargetable(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "canUseGameMasterBlocks", at = @At("HEAD"), cancellable = true)
    private void liymod$grantOperatorPrivileges(CallbackInfoReturnable<Boolean> cir) {
        if (LoliProtection.isProtected((Player) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getLuck", at = @At("HEAD"), cancellable = true)
    private void liymod$boostLuck(CallbackInfoReturnable<Float> cir) {
        if (LoliProtection.isProtected((Player) (Object) this)) {
            cir.setReturnValue(16384.0F);
        }
    }

    @Inject(method = "giveExperienceLevels", at = @At("HEAD"), cancellable = true)
    private void liymod$boostExperience(int levels, CallbackInfo ci) {
        if (LoliProtection.isProtected((Player) (Object) this)) {
            experienceLevel = 142857;
            onUpdateAbilities();
            ci.cancel();
        }
    }

}
