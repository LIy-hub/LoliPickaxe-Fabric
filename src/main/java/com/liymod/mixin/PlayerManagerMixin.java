package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import com.liymod.protection.TrustedPlayerLifecycle;
import net.minecraft.entity.Entity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerManager.class, priority = Integer.MAX_VALUE)
public abstract class PlayerManagerMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void lolipickaxe$beginTrustedDisconnect(ServerPlayerEntity player, CallbackInfo ci) {
        LoliExecutionManager.completeDisconnect(player);
        TrustedPlayerLifecycle.begin(player);
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void lolipickaxe$endTrustedDisconnect(ServerPlayerEntity player, CallbackInfo ci) {
        TrustedPlayerLifecycle.end(player);
    }

    @Inject(method = "respawnPlayer", at = @At("HEAD"))
    private void lolipickaxe$beginTrustedRespawn(
            ServerPlayerEntity player,
            boolean alive,
            Entity.RemovalReason removalReason,
            CallbackInfoReturnable<ServerPlayerEntity> cir
    ) {
        TrustedPlayerLifecycle.begin(player);
    }

    @Inject(method = "respawnPlayer", at = @At("RETURN"))
    private void lolipickaxe$finishTrustedRespawn(
            ServerPlayerEntity oldPlayer,
            boolean alive,
            Entity.RemovalReason removalReason,
            CallbackInfoReturnable<ServerPlayerEntity> cir
    ) {
        TrustedPlayerLifecycle.end(oldPlayer);
        ServerPlayerEntity replacement = cir.getReturnValue();
        if (replacement != null) {
            LoliExecutionManager.completeRespawn(oldPlayer, replacement);
        }
    }
}
