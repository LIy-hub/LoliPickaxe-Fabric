package com.liymod.client;

import com.liymod.config.FinalToolSettings;
import com.liymod.item.LoliFluidMining;
import com.liymod.item.LoliMiningRange;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Keeps the selected shape and adds a separate white mining-range outline. */
public final class MiningOutline {
    @SubscribeEvent public void highlight(RenderHighlightEvent.Block event) {
        Minecraft client=Minecraft.getInstance();
        if(client.player==null || client.level==null) return;
        var tool=client.player.getMainHandItem();
        if(!FinalToolSettings.isFinal(tool)) return;
        var pos=event.getTarget().getBlockPos();
        var plan=plan(client.level,pos,client.level.getBlockState(pos),LoliFluidMining.isEnabled(tool),
                FinalToolSettings.rangePreview(tool),FinalToolSettings.radius(tool));
        var eye=event.getCamera().getPosition();
        if(plan.selectedFluid()!=null) {
            // Vanilla fluid block outlines are empty. Replace only that selected outline, even with no range preview.
            event.setCanceled(true);
            var poses=event.getPoseStack();poses.pushPose();
            poses.translate(pos.getX()-eye.x,pos.getY()-eye.y,pos.getZ()-eye.z);
            drawSelectedFluid(poses,event.getMultiBufferSource().getBuffer(RenderType.lines()),plan.selectedFluid());
            poses.popPose();
        }
        // Solid/slab/stair selection stays with vanilla; the range is an independent full-alpha white cube.
        if(plan.range()!=null) LevelRenderer.renderLineBox(event.getPoseStack(),event.getMultiBufferSource().getBuffer(RenderType.lines()),
                plan.range().bounds().move(pos.getX()-eye.x,pos.getY()-eye.y,pos.getZ()-eye.z),1.0F,1.0F,1.0F,1.0F);
    }

    public record OutlinePlan(VoxelShape selectedFluid,VoxelShape range) { }

    /** Native-state fixture seam for selection and preview independence; no world mutation or drawing. */
    public static OutlinePlan plan(BlockGetter level,BlockPos pos,BlockState state,boolean selectFluids,boolean preview,int radius) {
        if(!LoliFluidMining.canMine(state,selectFluids)) return new OutlinePlan(null,null);
        VoxelShape selected=selectFluids && LoliFluidMining.isFluidBlock(state)?state.getFluidState().getShape(level,pos):null;
        return new OutlinePlan(selected,preview && radius>0?LoliMiningRange.outline(radius):null);
    }

    private static void drawSelectedFluid(PoseStack poses,VertexConsumer lines,VoxelShape shape) {
        var pose=poses.last();
        shape.forAllEdges((x0,y0,z0,x1,y1,z1)-> {
            var normal=new net.minecraft.world.phys.Vec3(x1-x0,y1-y0,z1-z0).normalize();
            lines.vertex(pose.pose(),(float)x0,(float)y0,(float)z0).color(0.0F,0.0F,0.0F,0.4F)
                    .normal(pose.normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
            lines.vertex(pose.pose(),(float)x1,(float)y1,(float)z1).color(0.0F,0.0F,0.0F,0.4F)
                    .normal(pose.normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
        });
    }
}
