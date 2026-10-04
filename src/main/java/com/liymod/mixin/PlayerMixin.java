package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import com.liymod.protection.LoliProtection;
import com.liymod.storage.LoliStorageEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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

    @Inject(
            method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN")
    )
    private void liymod$keepManualDropsOutsideStorage(
            ItemStack stack,
            boolean throwRandomly,
            CallbackInfoReturnable<ItemEntity> cir
    ) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer serverPlayer
                && LoliStorageEvents.hasHeldStorage(serverPlayer)) {
            LoliStorageEvents.markManualEjection(cir.getReturnValue());
        }
    }

    @Inject(method = "blockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void liymod$extendLoliBlockReach(CallbackInfoReturnable<Double> cir) {
        Player self = (Player) (Object) this;
        ItemStack stack = self.getMainHandItem();
        if (LoliItemSettings.isFinalPickaxe(stack)) {
            double configured = LoliItemSettings.getDouble(stack, LoliConfigOption.BLOCK_REACH_DISTANCE);
            if (configured > 0.0D) {
                cir.setReturnValue(configured);
            }
        }
    }

    @Inject(method = "entityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void liymod$extendLoliEntityReach(CallbackInfoReturnable<Double> cir) {
        Player self = (Player) (Object) this;
        ItemStack stack = self.getMainHandItem();
        if (LoliItemSettings.isFinalPickaxe(stack)) {
            double configured = LoliItemSettings.getDouble(stack, LoliConfigOption.BLOCK_REACH_DISTANCE);
            if (configured > 0.0D) {
                cir.setReturnValue(configured);
            }
        }
    }

}
