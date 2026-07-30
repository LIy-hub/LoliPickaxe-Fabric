package com.liymod.mixin;

import com.liymod.combat.LoliAttackResolver;
import com.liymod.protection.LoliProtection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = Integer.MAX_VALUE)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayer player;

    @ModifyExpressionValue(
            method = {"handleAttack", "handleInteract"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getEntityOrPart(I)Lnet/minecraft/world/entity/Entity;"
            )
    )
    private Entity lolipickaxe$hideProtectedPlayerFromInteractionPacket(Entity target) {
        return LoliProtection.isUntargetable(target) ? null : target;
    }

    @Inject(method = "handleAnimate", at = @At("TAIL"))
    private void lolipickaxe$executeAbsoluteSwing(
            ServerboundSwingPacket packet,
            CallbackInfo ci
    ) {
        if (packet.getHand() == InteractionHand.MAIN_HAND) {
            LoliAttackResolver.executeFromLook(player);
        }
    }
}
