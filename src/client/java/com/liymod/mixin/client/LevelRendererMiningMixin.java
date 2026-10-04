package com.liymod.mixin.client;

import com.liymod.client.mining.LoliMiningRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
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
    @Shadow @Final private GameRenderer gameRenderer;
    @Shadow @Final private LevelRenderState levelRenderState;

    @Inject(method = "submitBlockOutline", at = @At("RETURN"))
    private void liymod$submitRangeOutline(PoseStack poses, SubmitNodeCollector collector,
                                          LevelRenderState state, CallbackInfo ci) {
        var range = ((LoliMiningRenderState) state).liymod$getMiningOutline();
        var center = state.blockOutlineRenderState;
        if (range == null || center == null) return;
        var camera = state.cameraRenderState.pos;
        poses.pushPose();
        poses.translate(center.pos().getX() - camera.x, center.pos().getY() - camera.y,
                center.pos().getZ() - camera.z);
        collector.submitShapeOutline(poses, range, RenderTypes.lines(), 0xFFFFFFFF,
                gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth, center.isTranslucent());
        poses.popPose();
    }

    @Redirect(method = "compileSections", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;compileAsync(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;)V"))
    private void liymod$compileMiningBatchTogether(SectionRenderDispatcher.RenderSection section,
                                                  RenderSectionRegion region) {
        if (((LoliMiningRenderState) levelRenderState).liymod$getImmediateMiningSections()
                .contains(section.getSectionNode())) {
            section.compileSync(region);
        } else {
            section.compileAsync(region);
        }
    }
}
