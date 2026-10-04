package com.liymod.mixin;

import com.liymod.protection.LoliProtection;
import com.liymod.protection.TrustedPlayerLifecycle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayer.class, priority = Integer.MAX_VALUE)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventServerDamage(
            ServerLevel serverLevel,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (LoliProtection.isProtected(self)) {
            LoliProtection.retaliate(self, source);
            self.setHealth(self.getMaxHealth());
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$preventServerDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (LoliProtection.isProtected(self)) {
            self.setHealth(self.getMaxHealth());
            self.deathTime = 0;
            ci.cancel();
        }
    }

    @Inject(
            method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("HEAD")
    )
    private void lolipickaxe$beginTrustedDimensionMove(
            TeleportTransition transition,
            CallbackInfoReturnable<ServerPlayer> cir
    ) {
        TrustedPlayerLifecycle.begin((ServerPlayer) (Object) this);
    }

    @Inject(
            method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("RETURN")
    )
    private void lolipickaxe$endTrustedDimensionMove(
            TeleportTransition transition,
            CallbackInfoReturnable<ServerPlayer> cir
    ) {
        TrustedPlayerLifecycle.end((ServerPlayer) (Object) this);
    }

    @Inject(
            method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",
            at = @At("HEAD")
    )
    private void lolipickaxe$beginTrustedCrossWorldTeleport(
            ServerLevel destination,
            double x,
            double y,
            double z,
            Set<Relative> relatives,
            float yaw,
            float pitch,
            boolean dismount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        TrustedPlayerLifecycle.begin((ServerPlayer) (Object) this);
    }

    @Inject(
            method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",
            at = @At("RETURN")
    )
    private void lolipickaxe$endTrustedCrossWorldTeleport(
            ServerLevel destination,
            double x,
            double y,
            double z,
            Set<Relative> relatives,
            float yaw,
            float pitch,
            boolean dismount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        TrustedPlayerLifecycle.end((ServerPlayer) (Object) this);
    }
}
