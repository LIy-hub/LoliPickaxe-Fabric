package com.liymod.mixin.client;
import com.liymod.client.mining.LoliRangeMiningClient;
import com.liymod.config.LoliItemSettings;
import com.liymod.item.LoliFluidMining;
import com.liymod.item.LoliMiningRange;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMiningMixin {
    @Unique private Set<Long> liymod$immediateSections = Set.of();
    @Shadow private static void renderShape(PoseStack poses, VertexConsumer vertices, VoxelShape shape, double x, double y, double z, float red, float green, float blue, float alpha) { throw new AssertionError(); }
    @Inject(method = "compileSections", at = @At("HEAD"))
    private void liymod$beginMiningBatch(Camera camera, CallbackInfo ci) { liymod$immediateSections = LoliRangeMiningClient.takeImmediateSections(Minecraft.getInstance().level); }
    @Redirect(method = "compileSections", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"))
    private void liymod$compileTogether(SectionRenderDispatcher.RenderSection section, SectionRenderDispatcher dispatcher, RenderRegionCache cache) {
        if (liymod$immediateSections.contains(SectionPos.asLong(section.getOrigin()))) section.compileSync(cache);
        else section.rebuildSectionAsync(dispatcher, cache);
    }
    @Redirect(method = "renderHitOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape liymod$fluidOutline(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var player = Minecraft.getInstance().player;
        if (player != null && !player.isSpectator() && LoliFluidMining.isEnabled(player.getMainHandItem()) && LoliFluidMining.isFluidBlock(state)) return state.getFluidState().getShape(level, pos);
        return state.getShape(level, pos, context);
    }
    @Inject(method = "renderHitOutline", at = @At("RETURN"))
    private void liymod$rangeOutline(PoseStack poses, VertexConsumer vertices, Entity camera, double x, double y, double z, BlockPos pos, BlockState state, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || !LoliItemSettings.isFinalPickaxe(player.getMainHandItem()) || !LoliFluidMining.canMine(state, LoliFluidMining.isEnabled(player.getMainHandItem()))) return;
        int radius = LoliItemSettings.getMiningRadius(player.getMainHandItem());
        if (radius > 0) renderShape(poses, vertices, LoliMiningRange.outline(radius), pos.getX()-x, pos.getY()-y, pos.getZ()-z, 1F, 1F, 1F, 0.8F);
    }
}
