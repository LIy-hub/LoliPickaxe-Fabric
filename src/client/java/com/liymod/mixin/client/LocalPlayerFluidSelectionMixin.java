package com.liymod.mixin.client;

import com.liymod.item.LoliFluidMining;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extend the completed native pick, preserving nearer entity and block hits. */
@Mixin(GameRenderer.class)
public abstract class LocalPlayerFluidSelectionMixin {
    @Inject(method = "pick(F)V", at = @At("RETURN"))
    private void liymod$selectFluids(float partialTick, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || client.hitResult == null || client.getCameraEntity() != player
                || player.isSpectator() || !LoliFluidMining.isEnabled(player.getMainHandItem())) return;
        var eye = player.getEyePosition(partialTick);
        var end = eye.add(player.getViewVector(partialTick).scale(player.blockInteractionRange()));
        var fluidHit = LoliFluidMining.clip(player.level(), eye, end, CollisionContext.of(player), true);
        client.hitResult = LoliFluidMining.preferFluidHit(player.level(), eye, client.hitResult, fluidHit);
        if (client.hitResult.getType() != HitResult.Type.ENTITY) client.crosshairPickEntity = null;
    }
}
