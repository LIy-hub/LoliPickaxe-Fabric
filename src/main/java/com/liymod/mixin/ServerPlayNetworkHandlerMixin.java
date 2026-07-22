package com.liymod.mixin;

import com.liymod.combat.LoliAttackResolver;
import com.liymod.protection.LoliProtection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayNetworkHandler.class, priority = Integer.MAX_VALUE)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @ModifyExpressionValue(
            method = "onPlayerInteractEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/packet/c2s/play/PlayerInteractEntityC2SPacket;getEntity(Lnet/minecraft/server/world/ServerWorld;)Lnet/minecraft/entity/Entity;"
            )
    )
    private Entity lolipickaxe$hideProtectedPlayerFromInteractionPacket(Entity target) {
        return LoliProtection.isUntargetable(target) ? null : target;
    }

    @Inject(method = "onHandSwing", at = @At("TAIL"))
    private void lolipickaxe$executeAbsoluteSwing(
            HandSwingC2SPacket packet,
            CallbackInfo ci
    ) {
        if (packet.getHand() == Hand.MAIN_HAND) {
            LoliAttackResolver.executeFromLook(player);
        }
    }
}
