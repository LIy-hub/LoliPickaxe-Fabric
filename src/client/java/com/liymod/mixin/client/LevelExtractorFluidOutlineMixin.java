package com.liymod.mixin.client;

import com.liymod.client.mining.LoliMiningRenderState;
import com.liymod.client.mining.LoliRangeMiningClient;
import com.liymod.item.LoliFluidMining;
import com.liymod.item.LoliMiningRange;
import com.liymod.config.LoliItemSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves the selected block and extracts the range as a separate outline. */
@Mixin(LevelRenderer.class)
public abstract class LevelExtractorFluidOutlineMixin {
    @Shadow @Final private LevelRenderState levelRenderState;

    @Inject(method = "extractLevel", at = @At("RETURN"))
    private void liymod$extractMiningBatch(DeltaTracker delta, Camera camera, float partialTick, CallbackInfo ci) {
        ((LoliMiningRenderState) levelRenderState).liymod$setImmediateMiningSections(
                LoliRangeMiningClient.takeImmediateSections(Minecraft.getInstance().level));
    }

    @Inject(method = "extractBlockOutline", at = @At("RETURN"))
    private void liymod$outlineSelectedFluid(Camera camera, LevelRenderState renderState, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        var miningState = (LoliMiningRenderState) renderState;
        miningState.liymod$setMiningOutline(null);
        var outline = renderState.blockOutlineRenderState;
        if (outline == null || client.player == null || client.level == null || client.player.isSpectator()
                || !LoliItemSettings.isFinalPickaxe(client.player.getMainHandItem())) {
            return;
        }
        var tool = client.player.getMainHandItem();
        var state = client.level.getBlockState(outline.pos());
        boolean selectFluids = LoliFluidMining.isEnabled(tool);
        if (!LoliFluidMining.canMine(state, selectFluids)) {
            return;
        }
        int radius = LoliItemSettings.getMiningRadius(tool);
        if (selectFluids && LoliFluidMining.isFluidBlock(state)) {
            renderState.blockOutlineRenderState = new BlockOutlineRenderState(
                    outline.pos(), true, outline.highContrast(), state.getFluidState().getShape(client.level, outline.pos())
            );
        }
        if (radius > 0) miningState.liymod$setMiningOutline(LoliMiningRange.outline(radius));
    }
}
