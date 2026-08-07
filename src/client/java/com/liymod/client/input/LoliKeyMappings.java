package com.liymod.client.input;

import com.liymod.item.ModItems;
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

/** Safe in-game-only B/Shift+B/U bindings from the upstream mod. */
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
        if ((!storagePressed && !blacklistPressed)
                || client.player == null
                || client.gui.screen() != null
                || !holdsLoliTool(client)) {
            return;
        }

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

    private static boolean isLoliTool(ItemStack stack) {
        return stack.is(ModItems.LOLI_PICKAXE) || stack.is(ModItems.SMALL_LOLI_PICKAXE);
    }
}
