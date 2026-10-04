package com.liymod.client.screen;

import com.liymod.client.gui.ContainerViewport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Shrinks only an overflowing container, using one transform for drawing and all mouse input. */
abstract class FittedContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private ContainerViewport viewport;

    protected FittedContainerScreen(T menu, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        super(menu, inventory, title);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    @Override
    protected void init() {
        viewport = ContainerViewport.fit(minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), imageWidth, imageHeight);
        width = viewport.width();
        height = viewport.height();
        super.init();
    }

    @Override
    public final void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float deltaTicks) {
        super.renderBackground(graphics, mouseX, mouseY, deltaTicks);
        pushPanelTransform(graphics);
        try {
            renderPanelBackground(graphics);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    protected abstract void renderPanelBackground(GuiGraphics graphics);

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) { }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float deltaTicks) {
        int layoutMouseX = (int) Math.floor(viewport.layoutX(mouseX));
        int layoutMouseY = (int) Math.floor(viewport.layoutY(mouseY));
        pushPanelTransform(graphics);
        try {
            super.render(graphics, layoutMouseX, layoutMouseY, deltaTicks);
            renderTooltip(graphics, layoutMouseX, layoutMouseY);
            if (viewport.scale() < 1.0F) {
                // These grid screens have no text inputs. Flush their deferred tooltips under the
                // same transform; Screen's later pass sees an already-consumed tooltip.
                graphics.renderDeferredTooltip();
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private void pushPanelTransform(GuiGraphics graphics) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(viewport.offsetX(), viewport.offsetY());
        graphics.pose().scale(viewport.scale());
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClickedInPanel(viewport.layoutX(mouseX), viewport.layoutY(mouseY), button);
    }

    protected boolean mouseClickedInPanel(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(viewport.layoutX(mouseX), viewport.layoutY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(viewport.layoutX(mouseX), viewport.layoutY(mouseY), button,
                deltaX / viewport.scale(), deltaY / viewport.scale());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return super.mouseScrolled(viewport.layoutX(mouseX), viewport.layoutY(mouseY), deltaX, deltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(viewport.layoutX(mouseX), viewport.layoutY(mouseY));
    }
}
