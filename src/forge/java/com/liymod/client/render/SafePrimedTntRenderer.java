package com.liymod.client.render;

import com.liymod.entity.SafePrimedTntEntity;
import com.liymod.registry.ModContent;
import com.liymod.safe.SafeEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/** Renders the synchronized source block instead of hard-coded vanilla TNT. */
public final class SafePrimedTntRenderer extends EntityRenderer<SafePrimedTntEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public SafePrimedTntRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(SafePrimedTntEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        pose.translate(0.0F, 0.5F, 0.0F);
        int fuse = entity.getFuse();
        if ((float) fuse - partialTick + 1.0F < 10.0F) {
            float pulse = 1.0F - ((float) fuse - partialTick + 1.0F) / 10.0F;
            pulse = Mth.clamp(pulse, 0.0F, 1.0F);
            pulse *= pulse; pulse *= pulse;
            float scale = 1.0F + pulse * 0.3F;
            pose.scale(scale, scale, scale);
        }
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.translate(-0.5F, -0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        TntMinecartRenderer.renderWhiteSolidBlock(blockRenderer, state(entity.effect()), pose, buffers,
                packedLight, fuse / 5 % 2 == 0);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    private static BlockState state(SafeEffect effect) {
        return switch (effect) {
            case EXIT -> ModContent.LOLI_EXIT_TNT.get().defaultBlockState();
            case FAIL_RESPOND -> ModContent.LOLI_FAIL_RESPOND_TNT.get().defaultBlockState();
            default -> ModContent.LOLI_BLUE_SCREEN_TNT.get().defaultBlockState();
        };
    }

    @Override public ResourceLocation getTextureLocation(SafePrimedTntEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
