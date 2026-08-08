package com.liymod.client.card;

import com.liymod.item.LoliCardData;
import com.liymod.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

final class CardOnlineConfigScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("liymod", "textures/gui/loli_card_online_config.png");
    private final InteractionHand hand; private final String initial; private EditBox url; private boolean invalid;
    private CardOnlineConfigScreen(InteractionHand hand, String initial) { super(Component.translatable("gui.liymod.card.online_config_title")); this.hand = hand; this.initial = initial; }
    static CardOnlineConfigScreen from(String value) {
        int split = value == null ? -1 : value.indexOf('\n');
        try { return new CardOnlineConfigScreen(InteractionHand.valueOf(split < 0 ? "" : value.substring(0, split)), split < 0 ? "" : value.substring(split + 1)); }
        catch (RuntimeException ignored) { return new CardOnlineConfigScreen(InteractionHand.MAIN_HAND, ""); }
    }
    @Override protected void init() {
        int x = (width - 220) / 2, y = (height - 116) / 2;
        url = new EditBox(font, x + 10, y + 34, 200, 20, Component.translatable("gui.liymod.card.url")); url.setMaxLength(512); url.setValue(initial); addRenderableWidget(url);
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.card.save"), b -> save()).bounds(x + 10, y + 82, 95, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.card.close"), b -> onClose()).bounds(x + 115, y + 82, 95, 20).build());
    }
    private void save() { String value = url.getValue().strip(); if (!value.isEmpty() && !LoliCardData.isSafeHttpsUrl(value)) { invalid = true; return; } ModNetwork.CHANNEL.sendToServer(new ModNetwork.CardUpdatePacket(hand, value)); onClose(); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); int x = (width - 220) / 2, y = (height - 116) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, 220, 116, 256, 256); graphics.drawCenteredString(font, title, width / 2, y + 8, 0xFFFFFFFF);
        if (invalid) graphics.drawCenteredString(font, Component.translatable("gui.liymod.card.invalid_url"), width / 2, y + 61, 0xFFFF5555);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
