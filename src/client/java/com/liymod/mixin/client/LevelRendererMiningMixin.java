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
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Native outline and synchronous rebuild adapter for accepted mining batches. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMiningMixin {
    @Unique private Set<Long> liymod$immediateSections = Set.of();

    @Inject(method = "compileSections", at = @At("HEAD"))
    private void liymod$takeMiningBatch(Camera camera, CallbackInfo ci) {
        liymod$immediateSections = LoliRangeMiningClient.takeImmediateSections(Minecraft.getInstance().level);
    }

    @Redirect(method = "compileSections", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"))
    private void liymod$compileMiningBatchTogether(SectionRenderDispatcher.RenderSection section,
                                                  RenderRegionCache region) {
        if (liymod$immediateSections.contains(section.getSectionNode())) section.compileSync(region);
        else section.rebuildSectionAsync(region);
    }

    @Redirect(method = "renderHitOutline", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape liymod$outlineFluid(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var player = Minecraft.getInstance().player;
        return player != null && LoliFluidMining.isEnabled(player.getMainHandItem()) && LoliFluidMining.isFluidBlock(state)
                ? state.getFluidState().getShape(level, pos) : state.getShape(level, pos, context);
    }

    @Inject(method = "renderHitOutline", at = @At("RETURN"))
    private void liymod$renderRange(PoseStack poses, VertexConsumer vertices, Entity entity,
                                    double cameraX, double cameraY, double cameraZ, BlockPos pos,
                                    BlockState state, int color, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || !LoliItemSettings.isFinalPickaxe(player.getMainHandItem())
                || !LoliFluidMining.canMine(state, LoliFluidMining.isEnabled(player.getMainHandItem()))) return;
        int radius = LoliItemSettings.getMiningRadius(player.getMainHandItem());
        if (radius > 0) ShapeRenderer.renderShape(poses, vertices, LoliMiningRange.outline(radius),
                pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ, 0xFFFFFFFF);
    }
}
