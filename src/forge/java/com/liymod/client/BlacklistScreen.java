package com.liymod.client;

import com.liymod.client.gui.EditorLayout;
import com.liymod.client.gui.LoliGui;
import com.liymod.network.ModNetwork;
import com.liymod.storage.LoliStorageData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class BlacklistScreen extends Screen {
    private final ItemStack tool;private EditBox id;private EditorLayout layout;private LoliGui.TextBlock heading;
    private int panelX,panelY,offset,listHeight;private boolean invalid;
    public BlacklistScreen(ItemStack tool) { super(Component.translatable("container.liymod.loli_blacklist"));this.tool=tool; }
    @Override protected void init() {
        String draft=id==null?"minecraft:cobblestone":id.getValue();int panelWidth=EditorLayout.panelWidth(width,300);
        heading=LoliGui.text(font,title,panelWidth-20,2);listHeight=Math.max(font.lineHeight*2,Math.min(110,height-150));
        layout=EditorLayout.create(width,height,panelWidth,heading.height(),20,20,font.lineHeight,listHeight,20);
        panelX=(width-layout.width())/2;panelY=(height-layout.height())/2;
        id=new EditBox(font,panelX+10,panelY+layout.row(1),layout.contentWidth(),20,Component.literal("minecraft:item"));id.setMaxLength(128);id.setValue(draft);id.setResponder(value->invalid=false);addRenderableWidget(id);
        int half=(layout.contentWidth()-4)/2,y=panelY+layout.row(2);
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.enchantment.add"),b->update(true)).bounds(panelX+10,y,half,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.enchantment.remove"),b->update(false)).bounds(panelX+14+half,y,layout.contentWidth()-half-4,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(panelX+10,panelY+layout.row(5),layout.contentWidth(),20).build());
    }
    private void update(boolean add) {
        ResourceLocation value=ResourceLocation.tryParse(id.getValue().strip());
        if(value==null || !LoliStorageData.setBlacklisted(tool,value,add)) { invalid=true;return; }
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.BlacklistPacket(value.toString(),add));invalid=false;
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(x>=panelX+10 && x<panelX+layout.width()-10 && y>=panelY+layout.row(4) && y<panelY+layout.row(4)+listHeight) {
            int maximum=Math.max(0,LoliStorageData.blacklist(tool).size()-Math.max(1,listHeight/11));
            offset=Math.max(0,Math.min(maximum,offset-(int)Math.signum(delta)*3));return true;
        }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float delta) {
        renderBackground(graphics);LoliGui.panel(graphics,panelX,panelY,layout.width(),layout.height());
        heading.draw(graphics,font,panelX+10,panelY+layout.row(0),LoliGui.TEXT_COLOR,x,y,0,0);
        if(invalid) graphics.drawString(font,Component.translatable("gui.liymod.config.invalid"),panelX+10,panelY+layout.row(3),LoliGui.ERROR_COLOR,false);
        List<ResourceLocation> entries=new ArrayList<>(LoliStorageData.blacklist(tool));int rows=Math.max(1,listHeight/11);
        offset=Math.min(offset,Math.max(0,entries.size()-rows));int top=panelY+layout.row(4);
        graphics.enableScissor(panelX+10,top,panelX+layout.width()-10,top+listHeight);
        for(int i=offset;i<entries.size() && i<offset+rows;i++) graphics.drawString(font,entries.get(i).toString(),panelX+10,top+(i-offset)*11,LoliGui.TEXT_COLOR,false);
        graphics.disableScissor();super.render(graphics,x,y,delta);
        if(x>=panelX+10 && x<panelX+layout.width()-10 && y>=top && y<top+listHeight) {
            int entry=offset+(y-top)/11;
            if(entry<entries.size() && font.width(entries.get(entry).toString())>layout.contentWidth())
                graphics.renderTooltip(font,Component.literal(entries.get(entry).toString()),x,y);
        }
    }
}
