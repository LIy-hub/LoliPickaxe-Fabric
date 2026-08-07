package com.liymod.client.screen;

import com.liymod.menu.BlacklistMenu;
import com.liymod.network.BlacklistUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Nine-by-nine ghost-slot blacklist editor backed by the original texture. */
public final class LoliBlacklistScreen extends AbstractContainerScreen<BlacklistMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "liymod",
            "textures/gui/container/loli_pickaxe_container_blacklist.png");

    public LoliBlacklistScreen(BlacklistMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int blacklistSlot = blacklistSlotAt(event.x(), event.y());
        if (blacklistSlot >= 0 && (event.button() == 0 || event.button() == 1)) {
            ClientPlayNetworking.send(new BlacklistUpdatePayload(
                    blacklistSlot,
                    menu.getCarried().isEmpty()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractBackground(graphics, mouseX, mouseY, deltaTicks);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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
