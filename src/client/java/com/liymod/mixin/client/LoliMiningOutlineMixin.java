package com.liymod.mixin.client;
import com.liymod.config.LoliItemSettings;
import com.liymod.item.LoliFluidMining;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Append the range preview while preserving vanilla's selected-block outline. */
@Mixin(LevelRenderer.class)
public abstract class LoliMiningOutlineMixin {
    @Inject(method = "renderHitOutline", at = @At("RETURN"))
    private void liymod$outlineMining(PoseStack poses, VertexConsumer lines, Entity camera,
            double cameraX, double cameraY, double cameraZ, BlockPos pos, BlockState state, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.player.isSpectator()) return;
        var tool = client.player.getMainHandItem();
        if (!LoliItemSettings.isFinalPickaxe(tool) || !LoliFluidMining.canMine(state, LoliFluidMining.isEnabled(tool))) return;
        double x = pos.getX() - cameraX, y = pos.getY() - cameraY, z = pos.getZ() - cameraZ;
        if (LoliFluidMining.isEnabled(tool) && LoliFluidMining.isFluidBlock(state)) {
            var shape = state.getFluidState().getShape(client.level, pos);
            if (!shape.isEmpty()) LevelRenderer.renderLineBox(poses, lines, shape.bounds().move(x, y, z), 0F, 0F, 0F, 0.4F);
        }
        int radius = LoliItemSettings.getMiningRadius(tool);
        if (radius > 0) LevelRenderer.renderLineBox(poses, lines,
                new AABB(x - radius, y - radius, z - radius, x + radius + 1, y + radius + 1, z + radius + 1),
                1F, 1F, 1F, 1F);
    }
}
