package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import com.liymod.protection.TrustedPlayerLifecycle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerList.class, priority = Integer.MAX_VALUE)
public abstract class PlayerManagerMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void lolipickaxe$beginTrustedDisconnect(ServerPlayer player, CallbackInfo ci) {
        LoliExecutionManager.completeDisconnect(player);
        TrustedPlayerLifecycle.begin(player);
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void lolipickaxe$endTrustedDisconnect(ServerPlayer player, CallbackInfo ci) {
        TrustedPlayerLifecycle.end(player);
    }

    @Inject(method = "respawn", at = @At("HEAD"))
    private void lolipickaxe$beginTrustedRespawn(
            ServerPlayer player,
            boolean alive,
            Entity.RemovalReason reason,
            CallbackInfoReturnable<ServerPlayer> cir
    ) {
        TrustedPlayerLifecycle.begin(player);
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void lolipickaxe$finishTrustedRespawn(
            ServerPlayer oldPlayer,
            boolean alive,
            Entity.RemovalReason reason,
            CallbackInfoReturnable<ServerPlayer> cir
    ) {
        TrustedPlayerLifecycle.end(oldPlayer);
        ServerPlayer replacement = cir.getReturnValue();
        if (replacement != null) {
            LoliExecutionManager.completeRespawn(oldPlayer, replacement);
        }
    }
}
