package com.liymod.mixin.client;

import com.liymod.item.LoliFluidMining;
import com.liymod.item.LoliMiningRange;
import com.liymod.config.LoliItemSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shows the final pickaxe's range cube, retaining selected-fluid outlines in single mode. */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorFluidOutlineMixin {
    @Inject(method = "extractBlockOutline", at = @At("RETURN"))
    private void liymod$outlineSelectedFluid(Camera camera, LevelRenderState renderState, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
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
        if (radius > 0) {
            renderState.blockOutlineRenderState = new BlockOutlineRenderState(
                    outline.pos(), outline.isTranslucent(), true, LoliMiningRange.outline(radius)
            );
        } else if (selectFluids && LoliFluidMining.isFluidBlock(state)) {
            renderState.blockOutlineRenderState = new BlockOutlineRenderState(
                    outline.pos(), true, outline.highContrast(), state.getFluidState().getShape(client.level, outline.pos())
            );
        }
    }
}
