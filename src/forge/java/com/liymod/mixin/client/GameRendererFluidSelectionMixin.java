package com.liymod.mixin.client;

import com.liymod.item.LoliFluidMining;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererFluidSelectionMixin {
    @Inject(method="pick",at=@At("TAIL"))
    private void liymod$selectFluids(float partialTick, CallbackInfo ci) {
        Minecraft client=Minecraft.getInstance();
        if(client.player==null || client.level==null || client.hitResult==null || !LoliFluidMining.isEnabled(client.player.getMainHandItem())) return;
        var eye=client.player.getEyePosition(partialTick);
        double reach=client.player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());
        var hit=client.level.clip(new ClipContext(eye,eye.add(client.player.getViewVector(partialTick).scale(reach)),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,client.player));
        client.hitResult=LoliFluidMining.preferFluidHit(client.level,eye,client.hitResult,hit);
    }
}
