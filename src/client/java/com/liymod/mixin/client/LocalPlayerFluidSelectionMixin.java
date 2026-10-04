package com.liymod.mixin.client;

import com.liymod.item.LoliFluidMining;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerFluidSelectionMixin {
    @Inject(method = "raycastHitResult", at = @At("RETURN"), cancellable = true)
    private void liymod$selectFluids(
            float partialTick, Entity cameraEntity, CallbackInfoReturnable<HitResult> cir
    ) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (cameraEntity != player || player.isSpectator() || !LoliFluidMining.isEnabled(player.getMainHandItem())) {
            return;
        }
        var eye = player.getEyePosition(partialTick);
        var end = eye.add(player.getViewVector(partialTick).scale(player.blockInteractionRange()));
        var fluidHit = LoliFluidMining.clip(player.level(), eye, end, CollisionContext.of(player), true);
        cir.setReturnValue(LoliFluidMining.preferFluidHit(player.level(), eye, cir.getReturnValue(), fluidHit));
    }
}
