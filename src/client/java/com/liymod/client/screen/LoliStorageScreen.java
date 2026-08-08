package com.liymod.client.screen;

import com.liymod.menu.StorageMenu;
import com.liymod.network.StorageDropAllPayload;
import com.liymod.network.StoragePagePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Nine-by-nine paged storage backed by the original 240x256 texture. */
public final class LoliStorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("liymod", "textures/gui/container/loli_pickaxe_container.png");
    private static final int TEXT_COLOR = 0xFFF5F5F5;

    private Button previousButton;
    private Button nextButton;

    public LoliStorageScreen(StorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 240, 256);
        titleLabelX = 173;
        titleLabelY = 8;
    }

    @Override
    protected void init() {
        super.init();
        previousButton = addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        button -> changePage(-1))
                .bounds(leftPos + 173, topPos + 22, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.previous_page")))
                .build());
        nextButton = addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        button -> changePage(1))
                .bounds(leftPos + 213, topPos + 22, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.next_page")))
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.loli_storage.drop_all"),
                        button -> ClientPlayNetworking.send(new StorageDropAllPayload()))
                .bounds(leftPos + 173, topPos + 50, 60, 20)
                .build());
        updatePageButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updatePageButtons();
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
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_COLOR, true);
        Component page = Component.translatable(
                "gui.liymod.loli_storage.page",
                menu.getCurrentPage() + 1,
                menu.getPageCount());
        graphics.text(font, page, 203 - font.width(page) / 2, 29, TEXT_COLOR, true);
    }

    private void changePage(int delta) {
        int currentPage = menu.getCurrentPage();
        int target = Math.clamp(currentPage + Integer.signum(delta), 0, menu.getPageCount() - 1);
        if (target == currentPage) {
            return;
        }
        ClientPlayNetworking.send(new StoragePagePayload(delta));
    }

    private void updatePageButtons() {
        int currentPage = menu.getCurrentPage();
        if (previousButton != null) {
            previousButton.active = currentPage > 0;
        }
        if (nextButton != null) {
            nextButton.active = currentPage + 1 < menu.getPageCount();
        }
    }
}
