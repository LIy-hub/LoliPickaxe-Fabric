package com.liymod.client;

import com.liymod.client.gui.ContainerViewport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** One local transform for the overflowing storage panel and its mouse coordinates. */
abstract class FittedContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private ContainerViewport viewport;
    protected FittedContainerScreen(T menu,Inventory inventory,Component title,int panelWidth,int panelHeight) {
        super(menu,inventory,title);imageWidth=panelWidth;imageHeight=panelHeight;
    }
    @Override protected void init() {
        viewport=ContainerViewport.fit(minecraft.getWindow().getGuiScaledWidth(),minecraft.getWindow().getGuiScaledHeight(),imageWidth,imageHeight);
        width=viewport.width();height=viewport.height();super.init();
    }
    @Override public final void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics);
        int x=(int)Math.floor(viewport.layoutX(mouseX)),y=(int)Math.floor(viewport.layoutY(mouseY));
        graphics.pose().pushPose();
        graphics.pose().translate(viewport.offsetX(),viewport.offsetY(),0);
        graphics.pose().scale(viewport.scale(),viewport.scale(),1);
        try { super.render(graphics,x,y,partialTick);renderTooltip(graphics,x,y);graphics.flush(); }
        finally { graphics.pose().popPose(); }
    }
    @Override public boolean mouseClicked(double x,double y,int button) { return super.mouseClicked(viewport.layoutX(x),viewport.layoutY(y),button); }
    @Override public boolean mouseReleased(double x,double y,int button) { return super.mouseReleased(viewport.layoutX(x),viewport.layoutY(y),button); }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        return super.mouseDragged(viewport.layoutX(x),viewport.layoutY(y),button,dx/viewport.scale(),dy/viewport.scale());
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) { return super.mouseScrolled(viewport.layoutX(x),viewport.layoutY(y),delta); }
    @Override public void mouseMoved(double x,double y) { super.mouseMoved(viewport.layoutX(x),viewport.layoutY(y)); }
}
