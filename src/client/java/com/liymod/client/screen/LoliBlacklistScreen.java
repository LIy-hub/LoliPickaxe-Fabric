package com.liymod.client.screen;

import com.liymod.menu.BlacklistMenu;
import com.liymod.network.BlacklistUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Nine-by-nine ghost-slot blacklist editor backed by the original texture. */
public final class LoliBlacklistScreen extends FittedContainerScreen<BlacklistMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "liymod",
            "textures/gui/container/loli_pickaxe_container_blacklist.png");

    public LoliBlacklistScreen(BlacklistMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
    }

    @Override
    protected boolean mouseClickedInPanel(double mouseX, double mouseY, int button) {
        int blacklistSlot = blacklistSlotAt(mouseX, mouseY);
        if (blacklistSlot >= 0 && (button == 0 || button == 1)) {
            ClientPlayNetworking.send(new BlacklistUpdatePayload(
                    blacklistSlot,
                    menu.getCarried().isEmpty()));
            return true;
        }
        return super.mouseClickedInPanel(mouseX, mouseY, button);
    }

    @Override
    protected void renderPanelBackground(GuiGraphics graphics) {
        graphics.blit(
                TEXTURE,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The original 9x9 texture dedicates every foreground pixel to slots; keep labels in narration.
    }

    @Override
    public Component getNarrationMessage() {
        return Component.translatable("gui.liymod.loli_blacklist.hint");
    }

    private int blacklistSlotAt(double mouseX, double mouseY) {
        int relativeX = (int) Math.floor(mouseX) - leftPos - 8;
        int relativeY = (int) Math.floor(mouseY) - topPos - 8;
        if (relativeX < 0 || relativeY < 0 || relativeX >= 162 || relativeY >= 162) {
            return -1;
        }
        int withinX = relativeX % 18;
        int withinY = relativeY % 18;
        if (withinX >= 16 || withinY >= 16) {
            return -1;
        }
        return (relativeY / 18) * 9 + relativeX / 18;
    }
}
