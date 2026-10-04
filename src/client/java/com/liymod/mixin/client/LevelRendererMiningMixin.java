package com.liymod.mixin.client;
import com.liymod.client.mining.LoliRangeMiningClient;
import com.liymod.item.*;
import com.liymod.config.LoliItemSettings;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Set;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMiningMixin {
 @Unique private Set<Long> liymod$sections=Set.of();
 @Shadow private static void renderShape(PoseStack poses,VertexConsumer vertices,VoxelShape shape,double x,double y,double z,float r,float g,float b,float a) { throw new AssertionError(); }
 @Inject(method="renderHitOutline",at=@At("RETURN"))
 private void liymod$outline(PoseStack poses,VertexConsumer vertices,Entity entity,double x,double y,double z,BlockPos pos,BlockState state,CallbackInfo ci) {
  Minecraft client=Minecraft.getInstance();
  if(client.player==null || client.player.isSpectator() || !LoliItemSettings.isFinalPickaxe(client.player.getMainHandItem())) return;
  var tool=client.player.getMainHandItem(); boolean fluids=LoliFluidMining.isEnabled(tool);
  if(!LoliFluidMining.canMine(state,fluids)) return;
  if(fluids && LoliFluidMining.isFluidBlock(state)) renderShape(poses,vertices,state.getFluidState().getShape(client.level,pos),pos.getX()-x,pos.getY()-y,pos.getZ()-z,0,0,0,0.4F);
  int radius=LoliItemSettings.getMiningRadius(tool);
  if(radius>0) renderShape(poses,vertices,LoliMiningRange.outline(radius),pos.getX()-x,pos.getY()-y,pos.getZ()-z,1,1,1,1);
 }
 @Inject(method="compileSections",at=@At("HEAD"))
 private void liymod$captureBatch(Camera camera,CallbackInfo ci) { liymod$sections=LoliRangeMiningClient.takeImmediateSections(Minecraft.getInstance().level); }
 @Redirect(method="compileSections",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"))
 private void liymod$batchCompile(SectionRenderDispatcher.RenderSection chunk,SectionRenderDispatcher dispatcher,RenderRegionCache regions) {
  if(liymod$sections.contains(SectionPos.asLong(chunk.getOrigin()))) dispatcher.rebuildSectionSync(chunk,regions);
  else chunk.rebuildSectionAsync(dispatcher,regions);
 }
}
