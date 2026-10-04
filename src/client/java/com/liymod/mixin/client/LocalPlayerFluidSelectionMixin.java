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
public abstract class LocalPlayerFluidSelectionMixin {
 @Inject(method="pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",at=@At("RETURN"),cancellable=true)
 private void liymod$select(Entity cameraEntity,double blockRange,double entityRange,float partialTick,CallbackInfoReturnable<HitResult> cir) {
  var player=Minecraft.getInstance().player;
  if(player==null || cameraEntity!=player || player.isSpectator() || !LoliFluidMining.isEnabled(player.getMainHandItem())) return;
  var eye=player.getEyePosition(partialTick);
  var end=eye.add(player.getViewVector(partialTick).scale(blockRange));
  var fluid=LoliFluidMining.clip(player.level(),eye,end,CollisionContext.of(player),true);
  cir.setReturnValue(LoliFluidMining.preferFluidHit(player.level(),eye,cir.getReturnValue(),fluid));
 }
}
