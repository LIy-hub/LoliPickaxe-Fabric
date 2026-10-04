package com.liymod.mixin.client;
import com.liymod.client.mining.LoliRangeMiningClient;
import com.liymod.item.LoliFluidMining;
import com.liymod.item.LoliMiningRange;
import com.liymod.config.LoliItemSettings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMiningMixin {
 @Unique private Set<Long> liymod$sections=Set.of();
 @Inject(method="compileSections",at=@At("HEAD"))
 private void liymod$takeBatch(Camera camera,CallbackInfo ci) { liymod$sections=LoliRangeMiningClient.takeImmediateSections(Minecraft.getInstance().level); }
 @Redirect(method="compileSections",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"))
 private void liymod$batch(SectionRenderDispatcher.RenderSection section, SectionRenderDispatcher dispatcher, RenderRegionCache cache) {
  if(liymod$sections.contains(section.getSectionNode())) section.compileSync(cache); else section.rebuildSectionAsync(dispatcher, cache);
 }
 @Redirect(method="renderHitOutline",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
 private VoxelShape liymod$fluidOutline(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
  var player=Minecraft.getInstance().player;
  return player!=null && !player.isSpectator() && LoliFluidMining.isEnabled(player.getMainHandItem()) && LoliFluidMining.isFluidBlock(state)
   ? state.getFluidState().getShape(level,pos) : state.getShape(level,pos,context);
 }
 @Inject(method="renderHitOutline",at=@At("RETURN"))
 private void liymod$range(PoseStack poses,VertexConsumer vertices,Entity entity,double x,double y,double z,BlockPos pos,BlockState state,int color,CallbackInfo ci) {
  var player=Minecraft.getInstance().player;
  if(player==null || player.isSpectator() || !LoliItemSettings.isFinalPickaxe(player.getMainHandItem())) return;
  var tool=player.getMainHandItem();int radius=LoliItemSettings.getMiningRadius(tool);
  if(radius>0 && LoliFluidMining.canMine(state,LoliFluidMining.isEnabled(tool)))
   ShapeRenderer.renderShape(poses,vertices,LoliMiningRange.outline(radius),pos.getX()-x,pos.getY()-y,pos.getZ()-z,0xFFFFFFFF);
 }
}
