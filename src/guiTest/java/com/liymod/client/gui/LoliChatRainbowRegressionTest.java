package com.liymod.client.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** Exercises the same cached sequence at different render times without a game window. */
public final class LoliChatRainbowRegressionTest {
    public static void main(String[] args) {
        AtomicLong clock = new AtomicLong();
        Style titleStyle = Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true).withInsertion("preserve");
        Style statisticStyle = Style.EMPTY.withColor(ChatFormatting.RED).withBold(true);
        Component message = Component.empty().append(Component.translatable("message.liymod.kill_summary.title"));
        String title = "神器🌈";
        FormattedCharSequence line = FormattedCharSequence.composite(
                FormattedCharSequence.forward(title, titleStyle),
                FormattedCharSequence.forward(" · 12", statisticStyle));
        var animated = LoliChatRainbow.wrap(message, line, clock::get);
        require(animated != line, "The title must be animated without replacing its chat entry");
        List<Glyph> original = read(line);
        List<Glyph> first = read(animated);
        List<Glyph> cached = read(animated);
        int titleLength = title.codePointCount(0, title.length());
        var titleColors = new HashSet<Integer>();
        for (int i = 0; i < first.size(); i++) {
            require(first.get(i).codePoint == original.get(i).codePoint,
                    "Animation must preserve Unicode code points, counts and text order");
            if (i < titleLength) {
                require(first.get(i).style.withColor(ChatFormatting.GOLD).equals(titleStyle),
                        "Animation must preserve bold, insertion and every non-color style attribute");
                require(first.get(i).style == cached.get(i).style,
                        "Drawing another frame must reuse cached styles");
                titleColors.add(first.get(i).style.getColor().getValue());
            } else {
                require(first.get(i).style == original.get(i).style, "Statistics must retain their own color");
            }
        }
        require(titleColors.size() > 1, "The title must show several colors at the same time");
        clock.set(LoliChatRainbow.STEP_NANOS * 8);
        List<Glyph> next = read(animated);
        require(!first.get(0).style.getColor().equals(next.get(0).style.getColor()),
                "The same cached message must change color over time");
        clock.set(LoliChatRainbow.STEP_NANOS * 96);
        require(read(animated).equals(first), "The complete rainbow cycle must repeat smoothly");
        clock.set(-LoliChatRainbow.STEP_NANOS);
        require(read(animated).size() == original.size(), "Negative monotonic-clock origins must remain valid");
        require(LoliChatRainbow.wrap(Component.literal("Ordinary gold chat"), line, clock::get) == line,
                "Other chat messages must never receive the divine animation");
        var statsOnly = FormattedCharSequence.forward("12", statisticStyle);
        require(LoliChatRainbow.wrap(message, statsOnly, clock::get) == statsOnly,
                "A wrapped statistics-only line must remain untouched");
        int[] accepted = {0};
        require(!animated.accept((index, style, codePoint) -> ++accepted[0] < 2) && accepted[0] == 2,
                "The native sink must still be able to stop early for clipping or hit testing");
        System.out.println("DIVINE_CHAT_OK flowingGradient timeCycle cachedStyles unicode statisticsIsolation "
                + "ordinaryChatIsolation wrappedLines earlyExit=PASS");
    }

    private static List<Glyph> read(FormattedCharSequence sequence) {
        var glyphs = new ArrayList<Glyph>();
        sequence.accept((index, style, codePoint) -> {
            glyphs.add(new Glyph(codePoint, style));
            return true;
        });
        return glyphs;
    }

    private record Glyph(int codePoint, Style style) { }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
