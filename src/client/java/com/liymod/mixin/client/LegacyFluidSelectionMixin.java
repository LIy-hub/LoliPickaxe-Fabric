package com.liymod.mixin.client;
import com.liymod.item.LoliFluidMining;
import com.liymod.compat.LegacyReach;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class LegacyFluidSelectionMixin {
 @Inject(method="pick",at=@At("RETURN"))
 private void liymod$selectFluid(float partialTick,CallbackInfo ci) {
  Minecraft client=Minecraft.getInstance();
  if(client.player==null || client.level==null || client.hitResult==null || client.player.isSpectator()
    || client.getCameraEntity()!=client.player || !LoliFluidMining.isEnabled(client.player.getMainHandItem())) return;
  var eye=client.player.getEyePosition(partialTick);
  double configured=LegacyReach.configured(client.player);
  var end=eye.add(client.player.getViewVector(partialTick).scale(configured>0?configured:client.gameMode.getPickRange()));
  var candidate=LoliFluidMining.clip(client.level,eye,end,client.player,true);
  client.hitResult=LoliFluidMining.preferFluidHit(client.level,eye,client.hitResult,candidate);
 }
}
