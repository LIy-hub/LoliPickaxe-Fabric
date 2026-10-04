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
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;

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

    @Unique private VoxelShape liymod$rangeOutline;

    @Inject(method = "extractBlockOutline", at = @At("RETURN"))
    private void liymod$extractFluidAndRange(Camera camera, LevelRenderState renderState, CallbackInfo ci) {
        liymod$rangeOutline = null;
        Minecraft client = Minecraft.getInstance();
        var outline = renderState.blockOutlineRenderState;
        if (outline == null || client.player == null || client.level == null || client.player.isSpectator()
                || !LoliItemSettings.isFinalPickaxe(client.player.getMainHandItem())) return;
        var tool = client.player.getMainHandItem();
        var state = client.level.getBlockState(outline.pos());
        boolean fluids = LoliFluidMining.isEnabled(tool);
        if (!LoliFluidMining.canMine(state, fluids)) return;
        if (fluids && LoliFluidMining.isFluidBlock(state)) {
            renderState.blockOutlineRenderState = new BlockOutlineRenderState(outline.pos(), true,
                    outline.highContrast(), state.getFluidState().getShape(client.level, outline.pos()));
        }
        int radius = LoliItemSettings.getMiningRadius(tool);
        if (radius > 0) liymod$rangeOutline = LoliMiningRange.outline(radius);
    }

    @Inject(method = "renderHitOutline", at = @At("RETURN"))
    private void liymod$renderRange(PoseStack poses, VertexConsumer vertices,
                                    double cameraX, double cameraY, double cameraZ,
                                    BlockOutlineRenderState outline, int color, float lineWidth, CallbackInfo ci) {
        if (liymod$rangeOutline == null) return;
        var pos = outline.pos();
        ShapeRenderer.renderShape(poses, vertices, liymod$rangeOutline,
                pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ, 0xFFFFFFFF, lineWidth);
    }
}
