package com.liymod.client;

import com.liymod.LiyMod;
import com.liymod.menu.ModMenus;
import com.liymod.network.ModNetwork;
import com.liymod.registry.ModContent;
import com.liymod.storage.LoliStorageData;
import com.liymod.client.render.LoliEntityModel;
import com.liymod.client.render.LoliEntityRenderer;
import com.liymod.client.render.SafePrimedTntRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import com.liymod.network.ModNetwork.BlockChange;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.glfw.GLFW;

public final class ClientBootstrap {
    private static final String CATEGORY = "key.categories.liymod.general";
    private static final KeyMapping STORAGE = new KeyMapping("key.liymod.loli_container", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    private static final KeyMapping CONFIG = new KeyMapping("key.liymod.loli_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    private static final KeyMapping ENCHANTMENT = new KeyMapping("key.liymod.loli_enchantment", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    private static final KeyMapping EFFECT = new KeyMapping("key.liymod.loli_potion", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY);
    private static final KeyMapping TELEPORT = new KeyMapping("key.liymod.loli_space_folding", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    private static final KeyMapping BLACKLIST = new KeyMapping("key.liymod.loli_container_blacklist", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY);

    private ClientBootstrap() { }

    /** Applies every accepted block mutation in one render-thread task. */
    public static void applyRangeMining(List<BlockChange> changes) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        for (BlockChange change : changes) {
            if (client.level.hasChunkAt(change.pos())) {
                client.level.setBlock(change.pos(), change.state(), Block.UPDATE_ALL_IMMEDIATE);
            }
        }
    }

    public static void init() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(ClientBootstrap::registerKeys);
        modBus.addListener(ClientBootstrap::registerScreens);
        modBus.addListener(ClientBootstrap::registerLayers);
        modBus.addListener(ClientBootstrap::registerRenderers);
        MinecraftForge.EVENT_BUS.register(new ClientBootstrapEvents());
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(STORAGE); event.register(CONFIG); event.register(ENCHANTMENT); event.register(EFFECT); event.register(TELEPORT); event.register(BLACKLIST);
    }
    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LoliEntityModel.LAYER, LoliEntityModel::createBodyLayer);
    }
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.LOLI_ENTITY.get(), LoliEntityRenderer::new);
        event.registerEntityRenderer(ModContent.SAFE_PRIMED_TNT.get(), SafePrimedTntRenderer::new);
    }
    private static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.STORAGE.get(), StorageScreen::new);
            MenuScreens.register(ModMenus.PASSWORD_WORKBENCH.get(), PasswordWorkbenchScreen::new);
            MenuScreens.register(ModMenus.FINAL_CONFIG.get(), FinalToolScreen::new);
            MenuScreens.register(ModMenus.FINAL_ENCHANTMENT.get(), FinalToolScreen::new);
            MenuScreens.register(ModMenus.FINAL_EFFECT.get(), FinalToolScreen::new);
            MenuScreens.register(ModMenus.FINAL_TELEPORT.get(), FinalToolScreen::new);
        });
    }

    private static final class ClientBootstrapEvents {
        @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft client = Minecraft.getInstance();
            boolean pressed = false;
            while (STORAGE.consumeClick()) pressed = true;
            if (client.player == null || client.screen != null) return;
            if (pressed && (LoliStorageData.supports(client.player.getMainHandItem()) || LoliStorageData.supports(client.player.getOffhandItem()))) {
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.StorageActionPacket(
                        Screen.hasShiftDown() ? ModNetwork.StorageActionPacket.Action.DROP_ALL : ModNetwork.StorageActionPacket.Action.OPEN));
            }
            open(CONFIG, com.liymod.menu.FinalToolMenu.Mode.CONFIG);
            open(ENCHANTMENT, com.liymod.menu.FinalToolMenu.Mode.ENCHANTMENT);
            open(EFFECT, com.liymod.menu.FinalToolMenu.Mode.EFFECT);
            open(TELEPORT, com.liymod.menu.FinalToolMenu.Mode.TELEPORT);
            boolean blacklist = false; while (BLACKLIST.consumeClick()) blacklist = true;
            if (blacklist) {
                ItemStack tool = LoliStorageData.supports(client.player.getMainHandItem()) ? client.player.getMainHandItem() : client.player.getOffhandItem();
                if (LoliStorageData.supports(tool)) client.setScreen(new BlacklistScreen(tool));
            }
        }
        private static void open(KeyMapping key, com.liymod.menu.FinalToolMenu.Mode mode) {
            boolean clicked = false; while (key.consumeClick()) clicked = true;
            if (clicked) ModNetwork.CHANNEL.sendToServer(new ModNetwork.OpenFinalMenuPacket(mode));
        }
    }
}
