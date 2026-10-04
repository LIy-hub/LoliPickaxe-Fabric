package com.liymod.client;

import com.liymod.client.gui.LoliGui;
import com.liymod.menu.StorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class StorageScreen extends FittedContainerScreen<StorageMenu> {
    private Button previous,next,auto;
    private LoliGui.TextBlock heading,pageText;
    private int pageY,shownPage=-1,shownCount=-1;
    public StorageScreen(StorageMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,240,256);
    }
    @Override protected void init() {
        super.init();heading=LoliGui.text(font,title,60,2);int navigation=8+heading.height()+6;pageY=navigation+26;
        previous=addRenderableWidget(Button.builder(Component.literal("<"),b->click(StorageMenu.BUTTON_PREVIOUS)).bounds(leftPos+176,topPos+navigation,20,20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.previous_page"))).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b->click(StorageMenu.BUTTON_NEXT)).bounds(leftPos+216,topPos+navigation,20,20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.next_page"))).build());
        int dropY=pageY+2*font.lineHeight+6;
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.loli_storage.drop_all_short"),b->click(StorageMenu.BUTTON_DROP_ALL))
                .bounds(leftPos+176,topPos+dropY,60,20).tooltip(Tooltip.create(Component.translatable("gui.liymod.loli_storage.drop_all"))).build());
        auto=addRenderableWidget(Button.builder(autoText(),b->click(StorageMenu.BUTTON_AUTO_ACCEPT)).bounds(leftPos+176,topPos+dropY+26,60,20)
                .tooltip(Tooltip.create(Component.translatable("config.liymod.loli.auto_accept"))).build());
        shownPage=shownCount=-1;updateButtons();
    }
    private Component autoText() { return Component.translatable(menu.autoAccept()?"gui.liymod.enabled":"gui.liymod.disabled"); }
    private void click(int id) { if(minecraft!=null && minecraft.gameMode!=null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id); }
    @Override protected void containerTick() { super.containerTick();updateButtons(); }
    private void updateButtons() {
        if(menu.page()!=shownPage || menu.pageCount()!=shownCount) {
            pageText=LoliGui.text(font,Component.translatable("gui.liymod.loli_storage.page",menu.page()+1,menu.pageCount()),60,2);
            shownPage=menu.page();shownCount=menu.pageCount();
        }
        if(previous!=null) previous.active=menu.page()>0;if(next!=null) next.active=menu.page()+1<menu.pageCount();
        if(auto!=null) auto.setMessage(autoText());
    }
    @Override protected void renderBg(GuiGraphics graphics,float delta,int x,int y) {
        LoliGui.panel(graphics,leftPos,topPos,imageWidth,imageHeight);
        for(var slot:menu.slots) {
            int sx=leftPos+slot.x-1,sy=topPos+slot.y-1;
            graphics.fill(sx,sy,sx+18,sy+18,0xFF373737);graphics.fill(sx+1,sy+1,sx+18,sy+18,0xFFFFFFFF);graphics.fill(sx+1,sy+1,sx+17,sy+17,0xFF8B8B8B);
        }
        LoliGui.panel(graphics,leftPos+172,topPos,68,imageHeight);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {
        heading.draw(graphics,font,176,8,LoliGui.TEXT_COLOR,x,y,leftPos,topPos);
        pageText.draw(graphics,font,176,pageY,LoliGui.TEXT_COLOR,x,y,leftPos,topPos);
        // No free label row exists between the storage grid and player slots.
    }
}
