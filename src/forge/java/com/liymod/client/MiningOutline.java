package com.liymod.client;

import com.liymod.config.FinalToolSettings;
import com.liymod.item.LoliFluidMining;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** One origin-centered cube, including radius zero and selectable fluid blocks. */
public final class MiningOutline {
    @SubscribeEvent public void highlight(RenderHighlightEvent.Block event) {
        Minecraft client=Minecraft.getInstance();
        if(client.player==null || client.level==null) return;
        var tool=client.player.getMainHandItem();
        if(!FinalToolSettings.isFinal(tool)) return;
        var pos=event.getTarget().getBlockPos();
        boolean fluid=LoliFluidMining.isFluidBlock(client.level.getBlockState(pos));
        if(!FinalToolSettings.rangePreview(tool) && !fluid) return;
        event.setCanceled(true);
        int radius=FinalToolSettings.rangePreview(tool)?FinalToolSettings.radius(tool):0;
        var eye=event.getCamera().getPosition();
        LevelRenderer.renderLineBox(event.getPoseStack(),event.getMultiBufferSource().getBuffer(RenderType.lines()),
                new AABB(pos).inflate(radius).move(-eye.x,-eye.y,-eye.z),0.05F,0.05F,0.05F,0.55F);
    }
}
