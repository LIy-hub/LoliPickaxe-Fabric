package com.liymod.client;

import com.liymod.client.safe.SafeTntEffectClient;
import com.liymod.client.render.LoliEntityModel;
import com.liymod.client.render.LoliEntityRenderer;
import com.liymod.client.input.LoliKeyMappings;
import com.liymod.client.screen.FinalConfigScreen;
import com.liymod.client.screen.FinalEffectScreen;
import com.liymod.client.screen.FinalEnchantmentScreen;
import com.liymod.client.screen.FinalTeleportScreen;
import com.liymod.client.screen.LoliBlacklistScreen;
import com.liymod.client.screen.LoliStorageScreen;
import com.liymod.client.screen.PasswordWorkbenchScreen;
import com.liymod.client.storage.LoliStorageClient;
import com.liymod.entity.ModEntities;
import com.liymod.menu.ModMenus;
import java.util.Objects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.gui.screens.MenuScreens;

/** Client entrypoint for safe presentation and renderer bindings. */
public final class LiyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SafeTntEffectClient.register();
        MenuScreens.register(ModMenus.PASSWORD_WORKBENCH, PasswordWorkbenchScreen::new);
        MenuScreens.register(ModMenus.STORAGE, LoliStorageScreen::new);
        MenuScreens.register(ModMenus.BLACKLIST, LoliBlacklistScreen::new);
        MenuScreens.register(ModMenus.FINAL_CONFIG, FinalConfigScreen::new);
        MenuScreens.register(ModMenus.FINAL_ENCHANTMENT, FinalEnchantmentScreen::new);
        MenuScreens.register(ModMenus.FINAL_EFFECT, FinalEffectScreen::new);
        MenuScreens.register(ModMenus.FINAL_TELEPORT, FinalTeleportScreen::new);
        LoliKeyMappings.register();
        LoliStorageClient.register();
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
