package com.liymod.client;

import com.liymod.LiyMod;
import com.liymod.menu.StorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LiyMod.MOD_ID, "textures/gui/container/loli_pickaxe_container.png");
    private static final int TEXT_COLOR = 0xFFF5F5F5;
    private Button previous;
    private Button next;
    private Button auto;

    public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 240;
        imageHeight = 256;
        titleLabelX = 173;
        titleLabelY = 8;
        inventoryLabelX = 8;
        inventoryLabelY = 172;
    }

    @Override protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> click(StorageMenu.BUTTON_PREVIOUS))
                .bounds(leftPos + 173, topPos + 22, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.previous_page"))).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> click(StorageMenu.BUTTON_NEXT))
                .bounds(leftPos + 213, topPos + 22, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.next_page"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.loli_storage.drop_all"), b -> click(StorageMenu.BUTTON_DROP_ALL))
                .bounds(leftPos + 173, topPos + 50, 60, 20).build());
        auto = addRenderableWidget(Button.builder(autoText(), b -> click(StorageMenu.BUTTON_AUTO_ACCEPT))
                .bounds(leftPos + 173, topPos + 76, 60, 20).build());
        updateButtons();
    }

    private Component autoText() {
        return Component.translatable(menu.autoAccept() ? "gui.liymod.enabled" : "gui.liymod.disabled");
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override protected void containerTick() { super.containerTick(); updateButtons(); }

    private void updateButtons() {
        if (previous != null) previous.active = menu.page() > 0;
        if (next != null) next.active = menu.page() + 1 < menu.pageCount();
        if (auto != null) auto.setMessage(autoText());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT_COLOR, true);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_COLOR, true);
        Component page = Component.translatable("gui.liymod.loli_storage.page", menu.page() + 1, menu.pageCount());
        graphics.drawString(font, page, 203 - font.width(page) / 2, 29, TEXT_COLOR, true);
        graphics.drawString(font, Component.translatable("config.liymod.loli.auto_accept"), 173, 100, TEXT_COLOR, true);
    }
}
