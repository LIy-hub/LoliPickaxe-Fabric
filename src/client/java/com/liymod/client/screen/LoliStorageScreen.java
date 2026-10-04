package com.liymod.client.screen;

import com.liymod.client.gui.LoliGui;
import com.liymod.menu.StorageMenu;
import com.liymod.network.StorageDropAllPayload;
import com.liymod.network.StoragePagePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Nine-by-nine paged storage backed by the original 240x256 texture. */
public final class LoliStorageScreen extends FittedContainerScreen<StorageMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("liymod", "textures/gui/container/loli_pickaxe_container.png");
    private static final int TEXT_COLOR = LoliGui.TEXT_COLOR;

    private Button previousButton;
    private Button nextButton;
    private LoliGui.TextBlock titleText;
    private LoliGui.TextBlock pageText;
    private int pageY;
    private int displayedPage = -1;
    private int displayedPageCount = -1;

    public LoliStorageScreen(StorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 240, 256);
        titleLabelX = 173;
        titleLabelY = 8;
    }

    @Override
    protected void init() {
        super.init();
        titleText = LoliGui.text(font, title, 60, 2);
        int navigationY = 8 + titleText.height() + 6;
        pageY = navigationY + 26;
        displayedPage = -1;
        displayedPageCount = -1;
        previousButton = addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        button -> changePage(-1))
                .bounds(leftPos + 176, topPos + navigationY, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.previous_page")))
                .build());
        nextButton = addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        button -> changePage(1))
                .bounds(leftPos + 216, topPos + navigationY, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.next_page")))
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.loli_storage.drop_all_short"),
                        button -> ClientPlayNetworking.send(new StorageDropAllPayload()))
                .bounds(leftPos + 176, topPos + pageY + 2 * font.lineHeight + 6, 60, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.drop_all")))
                .build());
        updatePageButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updatePageButtons();
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
        LoliGui.panel(graphics, leftPos + 172, topPos, 68, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        titleText.draw(graphics, font, 176, 8, TEXT_COLOR, mouseX, mouseY, leftPos, topPos);
        pageText.draw(graphics, font, 176, pageY, TEXT_COLOR, mouseX, mouseY, leftPos, topPos);
        // The grid has no free label row between storage and player slots.
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
        int pageCount = menu.getPageCount();
        if (currentPage != displayedPage || pageCount != displayedPageCount) {
            pageText = LoliGui.text(font, Component.translatable("gui.liymod.loli_storage.page", currentPage + 1, pageCount), 60, 2);
            displayedPage = currentPage;
            displayedPageCount = pageCount;
        }
        if (previousButton != null) {
            previousButton.active = currentPage > 0;
        }
        if (nextButton != null) {
            nextButton.active = currentPage + 1 < menu.getPageCount();
        }
    }
}
