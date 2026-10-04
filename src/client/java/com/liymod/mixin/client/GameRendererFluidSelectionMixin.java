package com.liymod.mixin.client;
import com.liymod.item.LoliFluidMining;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GameRenderer.class)
public abstract class GameRendererFluidSelectionMixin {
    @Inject(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At("RETURN"), cancellable = true)
    private void liymod$selectFluids(Entity camera, double blockRange, double entityRange, float tick, CallbackInfoReturnable<HitResult> cir) {
        var client = Minecraft.getInstance();
        if (client.player == null || camera != client.player || client.player.isSpectator() || !LoliFluidMining.isEnabled(client.player.getMainHandItem())) return;
        var eye = camera.getEyePosition(tick);
        var hit = LoliFluidMining.clip(camera.level(), eye, eye.add(camera.getViewVector(tick).scale(blockRange)), CollisionContext.of(camera), true);
        cir.setReturnValue(LoliFluidMining.preferFluidHit(camera.level(), eye, cir.getReturnValue(), hit));
    }
}
