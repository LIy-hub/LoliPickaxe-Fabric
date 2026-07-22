package com.liymod.mixin;

import com.liymod.protection.LoliProtection;
import com.liymod.protection.TrustedPlayerLifecycle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayerEntity.class, priority = Integer.MAX_VALUE)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventServerDamage(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (LoliProtection.isProtected(self)) {
            LoliProtection.retaliate(self, source);
            self.setHealth(self.getMaxHealth());
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventServerDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (LoliProtection.isProtected(self)) {
            self.setHealth(self.getMaxHealth());
            self.deathTime = 0;
            ci.cancel();
        }
    }

    @Inject(method = "moveToWorld", at = @At("HEAD"))
    private void lolipickaxe$beginTrustedDimensionMove(
            ServerWorld destination,
            CallbackInfoReturnable<Entity> cir
    ) {
        TrustedPlayerLifecycle.begin((ServerPlayerEntity) (Object) this);
    }

    @Inject(method = "moveToWorld", at = @At("RETURN"))
    private void lolipickaxe$endTrustedDimensionMove(
            ServerWorld destination,
            CallbackInfoReturnable<Entity> cir
    ) {
        TrustedPlayerLifecycle.end((ServerPlayerEntity) (Object) this);
    }

    @Inject(
            method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDFF)V",
            at = @At("HEAD")
    )
    private void lolipickaxe$beginTrustedCrossWorldTeleport(
            ServerWorld destination,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            CallbackInfo ci
    ) {
        TrustedPlayerLifecycle.begin((ServerPlayerEntity) (Object) this);
    }

    @Inject(
            method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDFF)V",
            at = @At("RETURN")
    )
    private void lolipickaxe$endTrustedCrossWorldTeleport(
            ServerWorld destination,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            CallbackInfo ci
    ) {
        TrustedPlayerLifecycle.end((ServerPlayerEntity) (Object) this);
    }
}
