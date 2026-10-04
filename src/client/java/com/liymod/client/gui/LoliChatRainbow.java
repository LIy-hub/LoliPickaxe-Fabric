package com.liymod.client.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

/** Animates only the divine summary's title; cached chat layout and statistics stay intact. */
public final class LoliChatRainbow {
    static final long STEP_NANOS = 45_000_000L;
    private static final int STEPS_PER_COLOR = 16;
    private static final int[] ANCHORS = {0xFF6060, 0xFFCC55, 0xE8FF66, 0x66FFD4, 0x66BBFF, 0xC688FF};
    private static final int[] COLORS = palette();
    private static final int TITLE_COLOR = Style.EMPTY.withColor(ChatFormatting.GOLD).getColor().getValue();

    private LoliChatRainbow() { }

    public static FormattedCharSequence wrap(Component message, FormattedCharSequence line) {
        return wrap(message, line, System::nanoTime);
    }

    static FormattedCharSequence wrap(Component message, FormattedCharSequence line, LongSupplier clock) {
        if (!isDivineSummary(message)) return line;
        Map<Style, Style[]> styles = new HashMap<>();
        line.accept((index, style, codePoint) -> {
            if (style.isBold() && style.getColor() != null
                    && style.getColor().getValue() == TITLE_COLOR) {
                styles.computeIfAbsent(style, LoliChatRainbow::coloredStyles);
            }
            return true;
        });
        if (styles.isEmpty()) return line;
        // Only visible text is visited by vanilla. Reuse styles instead of refreshing chat or sending packets.
        return sink -> {
            int phase = (int) Math.floorMod(clock.getAsLong() / STEP_NANOS, COLORS.length);
            return line.accept((index, style, codePoint) -> {
                Style[] rainbow = styles.get(style);
                Style rendered = rainbow == null ? style : rainbow[Math.floorMod(phase + index * 3, COLORS.length)];
                return sink.accept(index, rendered, codePoint);
            });
        };
    }

    private static boolean isDivineSummary(Component message) {
        if (message.getContents() instanceof TranslatableContents translated
                && translated.getKey().equals("message.liymod.kill_summary.title")) return true;
        for (Component sibling : message.getSiblings()) {
            if (isDivineSummary(sibling)) return true;
        }
        return false;
    }

    private static Style[] coloredStyles(Style original) {
        Style[] styles = new Style[COLORS.length];
        for (int i = 0; i < styles.length; i++) styles[i] = original.withColor(COLORS[i]);
        return styles;
    }

    private static int[] palette() {
        int[] colors = new int[ANCHORS.length * STEPS_PER_COLOR];
        for (int i = 0; i < colors.length; i++) {
            int from = ANCHORS[i / STEPS_PER_COLOR];
            int to = ANCHORS[(i / STEPS_PER_COLOR + 1) % ANCHORS.length];
            int step = i % STEPS_PER_COLOR;
            int red = interpolate(from >> 16 & 255, to >> 16 & 255, step);
            int green = interpolate(from >> 8 & 255, to >> 8 & 255, step);
            int blue = interpolate(from & 255, to & 255, step);
            colors[i] = red << 16 | green << 8 | blue;
        }
        return colors;
    }

    private static int interpolate(int from, int to, int step) {
        return from + (to - from) * step / STEPS_PER_COLOR;
    }
}
