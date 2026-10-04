package com.liymod.client.gui;

import java.util.ArrayList;
import java.util.List;

/** Measured vertical rows shared by the slotless editors. Coordinates are panel-relative. */
public record EditorLayout(int width, int height, List<Integer> rowTops) {
    public static final int PADDING = 10;
    public static final int BUTTON_HEIGHT = 20;

    public EditorLayout {
        rowTops = List.copyOf(rowTops);
    }

    public static int panelWidth(int viewportWidth, int preferredWidth) {
        return Math.max(2 * PADDING + 1, Math.min(preferredWidth, viewportWidth - 16));
    }

    public static EditorLayout create(int viewportWidth, int viewportHeight, int preferredWidth, int... heights) {
        int contentHeight = 0;
        for (int height : heights) {
            if (height <= 0) {
                throw new IllegalArgumentException("Rows must have a positive height");
            }
            contentHeight += height;
        }
        int gap = heights.length < 2 ? 0 : Math.clamp(
                (viewportHeight - 16 - 2 * PADDING - contentHeight) / (heights.length - 1), 2, 6);
        List<Integer> tops = new ArrayList<>(heights.length);
        int y = PADDING;
        for (int height : heights) {
            tops.add(y);
            y += height + gap;
        }
        return new EditorLayout(panelWidth(viewportWidth, preferredWidth), y - gap + PADDING, tops);
    }

    public int row(int index) {
        return rowTops.get(index);
    }

    public int contentWidth() {
        return width - 2 * PADDING;
    }

    public boolean contains(double x, double y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
