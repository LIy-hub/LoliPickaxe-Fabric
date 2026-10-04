package com.liymod.client.card;

import com.liymod.client.gui.EditorLayout;
import com.liymod.client.gui.LoliGui;
import com.liymod.item.LoliCardData;
import com.liymod.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

final class CardOnlineConfigScreen extends Screen {
    private final InteractionHand hand;private final String initial;private EditBox url;
    private EditorLayout layout;private LoliGui.TextBlock heading,errorText;private boolean invalid;private int panelX,panelY;
    private CardOnlineConfigScreen(InteractionHand hand,String initial) { super(Component.translatable("gui.liymod.card.online_config_title"));this.hand=hand;this.initial=initial; }
    static CardOnlineConfigScreen from(String value) {
        int split=value==null?-1:value.indexOf('\n');
        try { return new CardOnlineConfigScreen(InteractionHand.valueOf(split<0?"":value.substring(0,split)),split<0?"":value.substring(split+1)); }
        catch(RuntimeException ignored) { return new CardOnlineConfigScreen(InteractionHand.MAIN_HAND,""); }
    }
    @Override protected void init() {
        String draft=url==null?initial:url.getValue();int panelWidth=EditorLayout.panelWidth(width,300);
        heading=LoliGui.text(font,title,panelWidth-20,2);
        errorText=LoliGui.text(font,Component.translatable("gui.liymod.card.invalid_url"),panelWidth-20,3);
        layout=EditorLayout.create(width,height,panelWidth,heading.height(),font.lineHeight,20,errorText.height(),20);
        panelX=(width-layout.width())/2;panelY=(height-layout.height())/2;
        url=new EditBox(font,panelX+10,panelY+layout.row(2),layout.contentWidth(),20,Component.translatable("gui.liymod.card.url"));
        url.setMaxLength(512);url.setValue(draft);url.setResponder(value->invalid=false);addRenderableWidget(url);
        int half=(layout.contentWidth()-4)/2,y=panelY+layout.row(4);
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.card.save"),b->save()).bounds(panelX+10,y,half,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.card.close"),b->onClose()).bounds(panelX+14+half,y,layout.contentWidth()-half-4,20).build());
        setInitialFocus(url);
    }
    private void save() { String value=url.getValue().strip();if(!value.isEmpty() && !LoliCardData.isSafeHttpsUrl(value)) { invalid=true;return; }ModNetwork.CHANNEL.sendToServer(new ModNetwork.CardUpdatePacket(hand,value));onClose(); }
    @Override public void render(GuiGraphics graphics,int x,int y,float delta) {
        renderBackground(graphics);LoliGui.panel(graphics,panelX,panelY,layout.width(),layout.height());
        heading.draw(graphics,font,panelX+10,panelY+layout.row(0),LoliGui.TEXT_COLOR,x,y,0,0);
        graphics.drawString(font,Component.translatable("gui.liymod.card.url"),panelX+10,panelY+layout.row(1),LoliGui.TEXT_COLOR,false);
        if(invalid) errorText.draw(graphics,font,panelX+10,panelY+layout.row(3),LoliGui.ERROR_COLOR,x,y,0,0);
        super.render(graphics,x,y,delta);
    }
}
