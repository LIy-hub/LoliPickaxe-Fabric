package com.liymod.mixin.client;

import com.liymod.client.mining.LoliMiningRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMiningMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private LevelRenderState levelRenderState;

    @Inject(method = "renderBlockOutline", at = @At("RETURN"))
    private void liymod$submitRangeOutline(MultiBufferSource.BufferSource buffers, PoseStack poses, boolean translucent,
                                          LevelRenderState state, CallbackInfo ci) {
        var range = ((LoliMiningRenderState) state).liymod$getMiningOutline();
        var center = state.blockOutlineRenderState;
        if (range == null || center == null || center.isTranslucent() != translucent) return;
        var camera = state.cameraRenderState.pos;
        poses.pushPose();
        poses.translate(center.pos().getX() - camera.x, center.pos().getY() - camera.y,
                center.pos().getZ() - camera.z);
        ShapeRenderer.renderShape(poses, buffers.getBuffer(RenderTypes.lines()), range, 0, 0, 0, 0xFFFFFFFF,
                minecraft.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth);
        poses.popPose();
        buffers.endLastBatch();
    }

    @Redirect(method = "compileSections", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"))
    private void liymod$compileMiningBatchTogether(SectionRenderDispatcher.RenderSection section,
                                                  RenderRegionCache region) {
        if (((LoliMiningRenderState) levelRenderState).liymod$getImmediateMiningSections()
                .contains(section.getSectionNode())) {
            section.compileSync(region);
        } else {
            section.rebuildSectionAsync(region);
        }
    }
}
