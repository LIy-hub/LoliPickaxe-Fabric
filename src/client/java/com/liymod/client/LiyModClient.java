package com.liymod.client;

import com.liymod.client.safe.SafeTntEffectClient;
import com.liymod.client.render.LoliEntityModel;
import com.liymod.client.render.LoliEntityRenderer;
import com.liymod.entity.ModEntities;
import java.util.Objects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.TntRenderer;

/** Client entrypoint for safe presentation and renderer bindings. */
public final class LiyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SafeTntEffectClient.register();
        registerRendererBindings(
                () -> ModelLayerRegistry.registerModelLayer(
                        LoliEntityModel.LAYER,
                        LoliEntityModel::createBodyLayer),
                () -> EntityRendererRegistry.register(ModEntities.LOLI, LoliEntityRenderer::new),
                () -> EntityRendererRegistry.register(ModEntities.LOLI_PRIMED_TNT, TntRenderer::new));
    }

    /**
     * Runs client-only entity renderer bindings without making this entrypoint depend on entity
     * classes that may be supplied by a later porting batch.
     */
    public static void registerRendererBindings(Runnable... registrations) {
        Objects.requireNonNull(registrations, "registrations");
        for (Runnable registration : registrations) {
            Objects.requireNonNull(registration, "registration").run();
        }
    }
}
