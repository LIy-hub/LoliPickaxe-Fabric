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
        System.out.println("GUI_LAYOUT_OK scenarios=" + count + " resize=PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
