package com.liymod.client.card;

import com.liymod.client.gui.EditorLayout;
import com.liymod.client.gui.LoliGui;
import com.liymod.item.LoliCardData;
import com.liymod.network.LoliCardOnlineUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

/** HTTPS-only editor for the exact online-card hand supplied by the server. */
final class CardOnlineConfigScreen extends Screen {
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private final InteractionHand hand;
    private final String initialUrl;
    private final boolean validHand;
    private EditBox urlBox;
    private boolean invalidUrl;
    private int panelLeft;
    private int panelTop;
    private EditorLayout layout;
    private LoliGui.TextBlock titleText;
    private LoliGui.TextBlock errorText;

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
        String previousUrl = urlBox == null ? initialUrl : urlBox.getValue();
        int contentWidth = EditorLayout.panelWidth(width, 300) - 20;
        titleText = LoliGui.text(font, title, contentWidth, 2);
        errorText = LoliGui.text(font, Component.translatable("gui.liymod.card.invalid_url"), contentWidth, 2);
        layout = EditorLayout.create(width, height, 300, titleText.height(), font.lineHeight, 20, errorText.height(), 20);
        panelLeft = (width - layout.width()) / 2;
        panelTop = (height - layout.height()) / 2;
        urlBox = new EditBox(
                font,
                panelLeft + 10,
                panelTop + layout.row(2),
                layout.contentWidth(),
                20,
                Component.translatable("gui.liymod.card.url"));
        urlBox.setMaxLength(LoliCardData.MAX_URL_LENGTH);
        urlBox.setValue(previousUrl);
        urlBox.setResponder(value -> invalidUrl = false);
        addRenderableWidget(urlBox);
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.card.save"),
                        button -> save())
                .bounds(panelLeft + 10, panelTop + layout.row(4), (layout.contentWidth() - 6) / 2, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.card.close"),
                        button -> onClose())
                .bounds(panelLeft + 16 + (layout.contentWidth() - 6) / 2, panelTop + layout.row(4), (layout.contentWidth() - 6) / 2, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        graphics.fill(0, 0, width, height, 0xD0101010);
        LoliGui.panel(graphics, panelLeft, panelTop, layout.width(), layout.height());
        titleText.draw(graphics, font, panelLeft + 10, panelTop + layout.row(0), TEXT_COLOR, mouseX, mouseY, 0, 0);
        graphics.text(
                font,
                Component.translatable("gui.liymod.card.url"),
                panelLeft + 10,
                panelTop + layout.row(1),
                TEXT_COLOR);
        if (invalidUrl) {
            errorText.draw(graphics, font, panelLeft + 10, panelTop + layout.row(3), ERROR_COLOR, mouseX, mouseY, 0, 0);
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
