package com.liymod.client.render;

import com.liymod.LiyMod;
import com.liymod.entity.LoliEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Visible default renderer backed by the upstream Loli texture and geometry. */
public final class LoliEntityRenderer
        extends MobRenderer<LoliEntity, LivingEntityRenderState, LoliEntityModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "textures/entity/loli.png");

    public LoliEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LoliEntityModel(context.bakeLayer(LoliEntityModel.LAYER)), 0.3F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
