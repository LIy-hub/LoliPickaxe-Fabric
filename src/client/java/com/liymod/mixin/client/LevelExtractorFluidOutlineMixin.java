package com.liymod.mixin.client;

import com.liymod.item.LoliFluidMining;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pure fluid blocks have no vanilla outline; show the actual selected fluid volume. */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorFluidOutlineMixin {
    @Inject(method = "extractBlockOutline", at = @At("RETURN"))
    private void liymod$outlineSelectedFluid(Camera camera, LevelRenderState renderState, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        var outline = renderState.blockOutlineRenderState;
        if (outline == null || client.player == null || client.level == null
                || !LoliFluidMining.isEnabled(client.player.getMainHandItem())) {
            return;
        }
        var state = client.level.getBlockState(outline.pos());
        if (LoliFluidMining.isFluidBlock(state)) {
            renderState.blockOutlineRenderState = new BlockOutlineRenderState(
                    outline.pos(), true, outline.highContrast(), state.getFluidState().getShape(client.level, outline.pos())
            );
        }
    }
}
