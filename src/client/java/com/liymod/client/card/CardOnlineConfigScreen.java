package com.liymod.client.card;

import com.liymod.item.LoliCardData;
import com.liymod.network.LoliCardOnlineUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/** HTTPS-only editor for the exact online-card hand supplied by the server. */
final class CardOnlineConfigScreen extends Screen {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "liymod",
            "textures/gui/loli_card_online_config.png");
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 116;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private final InteractionHand hand;
    private final String initialUrl;
    private final boolean validHand;
    private EditBox urlBox;
    private boolean invalidUrl;
    private int panelLeft;
    private int panelTop;

    private CardOnlineConfigScreen(InteractionHand hand, String initialUrl, boolean validHand) {
        super(Component.translatable("gui.liymod.card.online_config_title"));
        this.hand = hand;
        this.initialUrl = initialUrl;
        this.validHand = validHand;
        this.invalidUrl = !validHand;
    }

    static CardOnlineConfigScreen fromPayload(String encoded) {
        String value = encoded == null ? "" : encoded;
        int newline = value.indexOf('\n');
        String encodedHand = newline < 0 ? "" : value.substring(0, newline).strip();
        String url = newline < 0 ? "" : value.substring(newline + 1).strip();
        try {
            return new CardOnlineConfigScreen(InteractionHand.valueOf(encodedHand), url, true);
        } catch (IllegalArgumentException exception) {
            return new CardOnlineConfigScreen(InteractionHand.MAIN_HAND, url, false);
        }
    }

    @Override
    protected void init() {
        panelLeft = (width - PANEL_WIDTH) / 2;
        panelTop = (height - PANEL_HEIGHT) / 2;
        urlBox = new EditBox(
                font,
                panelLeft + 10,
                panelTop + 34,
                200,
                20,
                Component.translatable("gui.liymod.card.url"));
        urlBox.setMaxLength(LoliCardData.MAX_URL_LENGTH);
        urlBox.setValue(initialUrl);
        urlBox.setResponder(value -> invalidUrl = false);
        addRenderableWidget(urlBox);
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.card.save"),
                        button -> save())
                .bounds(panelLeft + 10, panelTop + 82, 95, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.card.close"),
                        button -> onClose())
                .bounds(panelLeft + 115, panelTop + 82, 95, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        graphics.fill(0, 0, width, height, 0xD0101010);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                panelLeft,
                panelTop,
                0.0F,
                0.0F,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                220,
                90,
                256,
                256);
        graphics.centeredText(font, title, width / 2, panelTop + 8, TEXT_COLOR);
        graphics.text(
                font,
                Component.translatable("gui.liymod.card.url"),
                panelLeft + 10,
                panelTop + 22,
                TEXT_COLOR);
        if (invalidUrl) {
            graphics.centeredText(
                    font,
                    Component.translatable("gui.liymod.card.invalid_url"),
                    width / 2,
                    panelTop + 61,
                    ERROR_COLOR);
        }
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
    }

    private void save() {
        String normalized = urlBox.getValue().strip();
        if (!validHand || (!normalized.isEmpty() && !LoliCardData.isSafeHttpsUrl(normalized))) {
            invalidUrl = true;
            return;
        }
        ClientPlayNetworking.send(new LoliCardOnlineUpdatePayload(hand, normalized));
        onClose();
    }
}
