package com.liymod.mixin;

import com.liymod.combat.LoliAttackResolver;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import com.liymod.protection.LoliProtection;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = Integer.MAX_VALUE)
public abstract class ServerGamePacketListenerMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleAnimate", at = @At("TAIL"))
    private void liymod$executeAbsoluteSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (packet.getHand() == InteractionHand.MAIN_HAND) LoliAttackResolver.executeFromLook(player);
    }

    @Redirect(method = "handleInteract", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;getTarget(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;"))
    private Entity liymod$hideProtectedInteractionTarget(ServerboundInteractPacket packet, ServerLevel level) {
        Entity target = packet.getTarget(level);
        return LoliProtection.isUntargetable(target) ? null : target;
    }
}
