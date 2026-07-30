package com.liymod.mixin;

import com.liymod.protection.LoliProtection;
import com.liymod.protection.TrustedPlayerLifecycle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(value = ServerPlayerEntity.class, priority = Integer.MAX_VALUE)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventServerDamage(
            ServerWorld world,
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

    @Inject(method = "teleportTo", at = @At("HEAD"))
    private void lolipickaxe$beginTrustedDimensionMove(
            TeleportTarget target,
            CallbackInfoReturnable<Entity> cir
    ) {
        TrustedPlayerLifecycle.begin((ServerPlayerEntity) (Object) this);
    }

    @Inject(method = "teleportTo", at = @At("RETURN"))
    private void lolipickaxe$endTrustedDimensionMove(
            TeleportTarget target,
            CallbackInfoReturnable<Entity> cir
    ) {
        TrustedPlayerLifecycle.end((ServerPlayerEntity) (Object) this);
    }

    @Inject(
            method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FFZ)Z",
            at = @At("HEAD")
    )
    private void lolipickaxe$beginTrustedCrossWorldTeleport(
            ServerWorld destination,
            double x,
            double y,
            double z,
            Set<PositionFlag> positionFlags,
            float yaw,
            float pitch,
            boolean resetCamera,
            CallbackInfoReturnable<Boolean> cir
    ) {
        TrustedPlayerLifecycle.begin((ServerPlayerEntity) (Object) this);
    }

    @Inject(
            method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FFZ)Z",
            at = @At("RETURN")
    )
    private void lolipickaxe$endTrustedCrossWorldTeleport(
            ServerWorld destination,
            double x,
            double y,
            double z,
            Set<PositionFlag> positionFlags,
            float yaw,
            float pitch,
            boolean resetCamera,
            CallbackInfoReturnable<Boolean> cir
    ) {
        TrustedPlayerLifecycle.end((ServerPlayerEntity) (Object) this);
    }
}
