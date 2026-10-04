package com.liymod.client.gui;

/** Regression scenarios for wrapped labels, minimum GUI size, resizing and outside-click geometry. */
public final class EditorLayoutRegressionTest {
    public static void main(String[] args) {
        int[][] viewports = {{320, 240}, {426, 240}, {640, 360}, {960, 540}};
        // Includes two-line titles, long localized names and error/help rows that used to leave the background.
        int[][] rows = {
                {18, 20, 20, 9, 20, 18},
                {18, 9, 20, 9, 20, 20, 9, 18, 20},
                {18, 9, 20, 20, 20, 9, 9, 20},
                {18, 9, 20, 18, 9, 9, 20, 18, 18, 20},
                {18, 9, 20, 18, 20}
        };
        int count = 0;
        for (int[] viewport : viewports) {
            for (int[] heights : rows) {
                EditorLayout layout = EditorLayout.create(viewport[0], viewport[1], 320, heights);
                require(layout.width() <= viewport[0] - 16, "Panel leaves the viewport horizontally");
                require(layout.height() <= viewport[1] - 16, "Wrapped text/control rows leave the viewport vertically");
                for (int i = 0; i < heights.length; i++) {
                    require(layout.row(i) >= EditorLayout.PADDING, "Missing top padding");
                    require(layout.row(i) + heights[i] <= layout.height() - EditorLayout.PADDING,
                            "Label/control extends past the panel bottom");
                    if (i > 0) {
                        require(layout.row(i) >= layout.row(i - 1) + heights[i - 1] + 2,
                                "Text overlaps the next row");
                    }
                }
                require(layout.contains(0, 0) && layout.contains(layout.width() - 0.5, layout.height() - 0.5),
                        "Visible panel is treated as an outside click");
                require(!layout.contains(layout.width(), 0) && !layout.contains(0, layout.height())
                                && !layout.contains(-0.5, 0) && !layout.contains(0, -0.5),
                        "Outside-click bounds do not match the drawn panel");
                count++;
            }
        }
        EditorLayout wide = EditorLayout.create(960, 540, 320, rows[3]);
        EditorLayout narrow = EditorLayout.create(320, 240, 320, rows[3]);
        require(narrow.width() < wide.width(), "Resize does not reduce the panel width");
        require(!narrow.contains(wide.width() - 1, 10), "Resize retains stale outside-click bounds");
        int[][] windows = {{427, 247}, {427, 240}, {480, 265}, {640, 360}, {320, 240}, {180, 180}};
        for (int[] window : windows) {
            ContainerViewport fitted = ContainerViewport.fit(window[0], window[1], 240, 256);
            int left = (fitted.width() - 240) / 2;
            int top = (fitted.height() - 256) / 2;
            require(fitted.screenX(left) >= 2 && fitted.screenY(top) >= 2,
                    "Container top/left are cropped");
            require(fitted.screenX(left + 240) <= window[0] - 2 && fitted.screenY(top + 256) <= window[1] - 2,
                    "Container bottom/right are cropped");
            require(fitted.scale() <= 1.0F, "Fit enlarges a container that already fits");
            if (window[1] >= 240) {
                require(fitted.scale() >= 0.9F, "Normal small-window fitting shrinks the container too much");
            }
            // Inverse-transform the painted centres of all storage slots, including the clipped ninth row.
            for (int row = 0; row < 9; row++) {
                for (int column = 0; column < 9; column++) {
                    double mouseX = fitted.layoutX(fitted.screenX(left + 16 + column * 18));
                    double mouseY = fitted.layoutY(fitted.screenY(top + 16 + row * 18));
                    require((int) ((mouseX - left - 8) / 18) == column
                                    && (int) ((mouseY - top - 8) / 18) == row,
                            "Rendered slot and mouse target disagree");
                }
            }
            require(fitted.layoutY(fitted.screenY(top + 255)) < top + 256,
                    "Bottom edge is treated as an outside click");
        }
        require(ContainerViewport.fit(427, 247, 240, 256).scale() > 0.93F,
                "Reported window should need less than 7 percent shrinkage");
        require(ContainerViewport.fit(480, 265, 240, 256).scale() == 1.0F,
                "Large screenshot window should retain its original size");
        System.out.println("GUI_LAYOUT_OK scenarios=" + count + " resize=PASS localContainerTransforms=" + windows.length);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
