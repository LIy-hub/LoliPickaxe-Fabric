package com.liymod.client.render;

import com.liymod.LiyMod;
import com.liymod.entity.LoliEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class LoliEntityRenderer extends MobRenderer<LoliEntity, LoliEntityModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LiyMod.MOD_ID, "textures/entity/loli.png");

    public LoliEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LoliEntityModel(context.bakeLayer(LoliEntityModel.LAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(LoliEntity entity) {
        return TEXTURE;
    }
}
