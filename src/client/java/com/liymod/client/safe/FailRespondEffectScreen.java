package com.liymod.client.safe;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Eight-second responsive imitation of a not-responding dialog. */
final class FailRespondEffectScreen extends TimedSafeTntEffectScreen {
    private static final int BACKGROUND = 0xE0181818;
    private static final int WINDOW = 0xFFF1F1F1;
    private static final int TITLE_BAR = 0xFFCCCCCC;
    private static final int TEXT = 0xFF202020;

    FailRespondEffectScreen(Screen previousScreen, int durationTicks) {
        super(Component.translatable("screen.liymod.safe_tnt.fail_respond.title"), previousScreen, durationTicks);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        graphics.fill(0, 0, width, height, BACKGROUND);

        int panelWidth = Math.min(360, Math.max(220, width - 40));
        int left = (width - panelWidth) / 2;
        int top = Math.max(20, height / 2 - 62);
        int right = left + panelWidth;
        int bottom = top + 124;
        graphics.fill(left, top, right, bottom, WINDOW);
        graphics.fill(left, top, right, top + 24, TITLE_BAR);
        graphics.text(font, title, left + 8, top + 8, TEXT);
        graphics.centeredText(
                font,
                Component.translatable("screen.liymod.safe_tnt.fail_respond.message"),
                width / 2,
                top + 48,
                TEXT);
        graphics.centeredText(font, countdownText(), width / 2, top + 72, TEXT);
        graphics.centeredText(
                font,
                Component.translatable("screen.liymod.safe_tnt.escape"),
                width / 2,
                top + 94,
                TEXT);
    }
}
