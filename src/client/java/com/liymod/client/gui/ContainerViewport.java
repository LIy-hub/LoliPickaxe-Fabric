package com.liymod.client.gui;

/** A local container transform, independent of the user's window/HUD GUI scale. */
public record ContainerViewport(int width, int height, float scale, float offsetX, float offsetY) {
    public static ContainerViewport fit(int viewportWidth, int viewportHeight, int panelWidth, int panelHeight) {
        float scale = Math.min(1.0F, Math.min(Math.max(1, viewportWidth - 8) / (float) panelWidth,
                Math.max(1, viewportHeight - 8) / (float) panelHeight));
        int width = (int) Math.ceil(viewportWidth / (double) scale);
        int height = (int) Math.ceil(viewportHeight / (double) scale);
        return new ContainerViewport(width, height, scale,
                (viewportWidth - width * scale) / 2.0F, (viewportHeight - height * scale) / 2.0F);
    }

    public double layoutX(double screenX) {
        return (screenX - offsetX) / scale;
    }

    public double layoutY(double screenY) {
        return (screenY - offsetY) / scale;
    }

    public double screenX(double layoutX) {
        return layoutX * scale + offsetX;
    }

    public double screenY(double layoutY) {
        return layoutY * scale + offsetY;
    }
}
