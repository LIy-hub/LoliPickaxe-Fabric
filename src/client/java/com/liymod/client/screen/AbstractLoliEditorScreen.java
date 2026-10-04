package com.liymod.client.screen;

import com.liymod.client.gui.EditorLayout;
import com.liymod.client.gui.LoliGui;
import com.liymod.menu.AbstractFinalToolMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Keeps vanilla menu lifecycle/input while using measured geometry for slotless editors. */
abstract class AbstractLoliEditorScreen<T extends AbstractFinalToolMenu> extends AbstractContainerScreen<T> {
    protected EditorLayout layout;

    protected AbstractLoliEditorScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected final void placePanel(EditorLayout measured) {
        layout = measured;
        leftPos = (width - layout.width()) / 2;
        topPos = (height - layout.height()) / 2;
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) { }

    protected final void drawLabel(GuiGraphics graphics, LoliGui.TextBlock text,
                                   int x, int y, int color, int mouseX, int mouseY) {
        text.draw(graphics, font, x, y, color, mouseX, mouseY, leftPos, topPos);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float deltaTicks) {
        super.renderBackground(graphics, mouseX, mouseY, deltaTicks);
        LoliGui.panel(graphics, leftPos, topPos, layout.width(), layout.height());
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return !layout.contains(mouseX - left, mouseY - top);
    }
}
