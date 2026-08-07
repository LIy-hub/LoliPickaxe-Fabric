package com.liymod.client.safe;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Six-second game-only blue overlay. It never touches the operating system. */
final class BlueScreenEffectScreen extends TimedSafeTntEffectScreen {
    private static final int BLUE = 0xFF0754A6;
    private static final int WHITE = 0xFFFFFFFF;

    BlueScreenEffectScreen(Screen previousScreen, int durationTicks) {
        super(Component.translatable("screen.liymod.safe_tnt.blue_screen.title"), previousScreen, durationTicks);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        graphics.fill(0, 0, width, height, BLUE);
        int centerX = width / 2;
        int centerY = height / 2;
        graphics.centeredText(font, title, centerX, centerY - 32, WHITE);
        graphics.centeredText(
                font,
                Component.translatable("screen.liymod.safe_tnt.blue_screen.message"),
                centerX,
                centerY - 8,
                WHITE);
        graphics.centeredText(font, countdownText(), centerX, centerY + 16, WHITE);
        graphics.centeredText(
                font,
                Component.translatable("screen.liymod.safe_tnt.escape"),
                centerX,
                centerY + 34,
                WHITE);
    }
}
