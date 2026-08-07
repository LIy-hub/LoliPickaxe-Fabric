package com.liymod.client.safe;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A responsive, non-pausing overlay with a hard tick limit and normal Escape handling. */
abstract class TimedSafeTntEffectScreen extends Screen {
    private final Screen previousScreen;
    private int remainingTicks;

    protected TimedSafeTntEffectScreen(Component title, Screen previousScreen, int durationTicks) {
        super(title);
        this.previousScreen = previousScreen;
        this.remainingTicks = Math.max(1, durationTicks);
    }

    final Screen previousScreen() {
        return previousScreen;
    }

    protected final Component countdownText() {
        int seconds = Math.max(1, (remainingTicks + 19) / 20);
        return Component.translatable("screen.liymod.safe_tnt.countdown", seconds);
    }

    @Override
    public void tick() {
        if (--remainingTicks <= 0) {
            onClose();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(previousScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean canInterruptWithAnotherScreen() {
        return true;
    }
}
