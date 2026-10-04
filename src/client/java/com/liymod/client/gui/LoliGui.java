package com.liymod.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/** Vanilla-style pixel panels and font-measured labels; no pre-sized background image is needed. */
public final class LoliGui {
    public static final int TEXT_COLOR = 0xFF404040;
    public static final int ERROR_COLOR = 0xFFB02020;
    public static final int PANEL_COLOR = 0xFFC6C6C6;

    private LoliGui() {
    }

    public static void centeredText(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 2, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 3, y + height - 2, 0xFFFFFFFF);
        graphics.fill(x + 2, y + height - 3, x + width - 1, y + height - 1, 0xFF555555);
        graphics.fill(x + width - 3, y + 2, x + width - 1, y + height - 1, 0xFF555555);
    }

    public static TextBlock text(Font font, Component source, int width, int maxLines) {
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(source, Math.max(1, width)));
        if (lines.size() > maxLines) {
            FormattedText lastLine = font.getSplitter().splitLines(source, Math.max(1, width), net.minecraft.network.chat.Style.EMPTY).get(maxLines - 1);
            FormattedText shortened = FormattedText.composite(
                    font.substrByWidth(lastLine, Math.max(1, width - font.width("…"))), FormattedText.of("…"));
            lines.set(maxLines - 1, Language.getInstance().getVisualOrder(shortened));
        }
        return new TextBlock(source, lines, width, maxLines, font.lineHeight);
    }

    public record TextBlock(Component source, List<FormattedCharSequence> lines, int width, int maxLines, int lineHeight) {
        public TextBlock {
            lines = List.copyOf(lines);
        }

        public int height() {
            return Math.max(1, Math.min(maxLines, lines.size())) * lineHeight;
        }

        public void draw(GuiGraphics graphics, Font font, int x, int y, int color,
                         int mouseX, int mouseY, int originX, int originY) {
            int count = Math.min(maxLines, lines.size());
            for (int i = 0; i < count; i++) {
                FormattedCharSequence line = lines.get(i);
                graphics.drawString(font, line, x + (width - font.width(line)) / 2, y + i * lineHeight, color, false);
            }
            if (lines.size() > maxLines) {
                // Show the complete styled/localized label without allowing it to overlap another row.
                if (mouseX >= originX + x && mouseX < originX + x + width
                        && mouseY >= originY + y && mouseY < originY + y + height()) {
                    graphics.renderTooltip(font, source, mouseX, mouseY);
                }
            }
        }
    }
}
