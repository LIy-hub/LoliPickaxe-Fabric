package com.liymod.client.input;

import com.liymod.item.ModItems;
import com.liymod.network.LoliMenuOpenPayload;
import com.liymod.network.StorageDropAllPayload;
import com.liymod.network.StorageOpenPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Safe in-game-only B/Shift+B/U and final-pickaxe N/M/P/K bindings from the upstream mod. */
public final class LoliKeyMappings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("liymod", "general"));
    private static final KeyMapping STORAGE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.liymod.loli_container",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            CATEGORY));
    private static final KeyMapping BLACKLIST = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.liymod.loli_container_blacklist",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_U,
            CATEGORY));
    private static final KeyMapping FINAL_CONFIG = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.liymod.loli_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_N,
            CATEGORY));
    private static final KeyMapping FINAL_ENCHANTMENT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.liymod.loli_enchantment",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_M,
            CATEGORY));
    private static final KeyMapping FINAL_EFFECT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.liymod.loli_potion",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_P,
            CATEGORY));
    private static final KeyMapping FINAL_TELEPORT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.liymod.loli_space_folding",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            CATEGORY));

    private static boolean registered;

    private LoliKeyMappings() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(LoliKeyMappings::handleKeys);
        registered = true;
    }

    private static void handleKeys(Minecraft client) {
        boolean storagePressed = consumeAllClicks(STORAGE);
        boolean blacklistPressed = consumeAllClicks(BLACKLIST);
        boolean configPressed = consumeAllClicks(FINAL_CONFIG);
        boolean enchantmentPressed = consumeAllClicks(FINAL_ENCHANTMENT);
        boolean effectPressed = consumeAllClicks(FINAL_EFFECT);
        boolean teleportPressed = consumeAllClicks(FINAL_TELEPORT);
        if (client.player == null || client.gui.screen() != null) {
            return;
        }

        if ((storagePressed || blacklistPressed) && holdsLoliTool(client)) {
            if (storagePressed) {
                if (client.hasShiftDown()) {
                    ClientPlayNetworking.send(new StorageDropAllPayload());
                } else {
                    ClientPlayNetworking.send(new StorageOpenPayload(StorageOpenPayload.Mode.STORAGE));
                }
            }
            if (blacklistPressed) {
                ClientPlayNetworking.send(new StorageOpenPayload(StorageOpenPayload.Mode.BLACKLIST));
            }
        }

        if (!holdsFinalLoliPickaxe(client)) {
            return;
        }
        if (configPressed) {
            ClientPlayNetworking.send(new LoliMenuOpenPayload(LoliMenuOpenPayload.Mode.CONFIG));
        } else if (enchantmentPressed) {
            ClientPlayNetworking.send(new LoliMenuOpenPayload(LoliMenuOpenPayload.Mode.ENCHANTMENT));
        } else if (effectPressed) {
            ClientPlayNetworking.send(new LoliMenuOpenPayload(LoliMenuOpenPayload.Mode.EFFECT));
        } else if (teleportPressed) {
            ClientPlayNetworking.send(new LoliMenuOpenPayload(LoliMenuOpenPayload.Mode.TELEPORT));
        }
    }

    private static boolean consumeAllClicks(KeyMapping mapping) {
        boolean pressed = false;
        while (mapping.consumeClick()) {
            pressed = true;
        }
        return pressed;
    }

    private static boolean holdsLoliTool(Minecraft client) {
        return isLoliTool(client.player.getMainHandItem()) || isLoliTool(client.player.getOffhandItem());
    }

    private static boolean holdsFinalLoliPickaxe(Minecraft client) {
        return client.player.getMainHandItem().is(ModItems.LOLI_PICKAXE)
                || client.player.getOffhandItem().is(ModItems.LOLI_PICKAXE);
    }

    private static boolean isLoliTool(ItemStack stack) {
        return stack.is(ModItems.LOLI_PICKAXE) || stack.is(ModItems.SMALL_LOLI_PICKAXE);
    }
}
