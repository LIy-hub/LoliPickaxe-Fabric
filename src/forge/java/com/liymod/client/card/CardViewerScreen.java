package com.liymod.client.card;

import com.liymod.item.LoliCardCatalog;
import com.liymod.item.LoliCardData;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class CardViewerScreen extends Screen {
    private record Image(ResourceLocation texture, int width, int height, String link) { }
    private final List<Image> bundled;
    private final String onlineUrl;
    private ResourceLocation onlineTexture;
    private DynamicTexture dynamicTexture;
    private int onlineWidth, onlineHeight, index;
    private double zoom = 1.0D, panX, panY, lastX, lastY;
    private double pressX, pressY;
    private boolean dragging, dragMoved, failed;

    private CardViewerScreen(List<Image> bundled, String onlineUrl, boolean album) {
        super(Component.translatable(album ? "gui.liymod.card.album_title" : onlineUrl == null ? "gui.liymod.card.title" : "gui.liymod.card.online_title"));
        this.bundled = bundled; this.onlineUrl = onlineUrl;
    }
    static CardViewerScreen bundled(List<LoliCardCatalog.Art> art, boolean album) {
        List<Image> images = new ArrayList<>();
        for (var entry : art) images.add(new Image(new ResourceLocation("liymod", "textures/lolicards/" + entry.texture() + ".png"), entry.width(), entry.height(), entry.link()));
        return new CardViewerScreen(images, null, album);
    }
    static CardViewerScreen online(String url) { return new CardViewerScreen(List.of(), url, false); }

    @Override protected void init() {
        if (bundled.size() > 1) {
            addRenderableWidget(Button.builder(Component.translatable("gui.liymod.card.previous"), b -> { index = Math.floorMod(index - 1, bundled.size()); reset(); }).bounds(10, height - 28, 80, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.liymod.card.next"), b -> { index = (index + 1) % bundled.size(); reset(); }).bounds(width - 90, height - 28, 80, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.card.close"), b -> onClose()).bounds(width / 2 - 40, height - 28, 80, 20).build());
        if (onlineUrl != null && onlineTexture == null && !failed) startLoad();
    }

    private void startLoad() {
        if (!LoliCardData.isSafeHttpsUrl(onlineUrl)) { failed = true; return; }
        OnlineCardImageLoader.load(onlineUrl).whenComplete((loaded, error) -> Minecraft.getInstance().execute(() -> {
            if (minecraft == null || minecraft.screen != this) { if (loaded != null) loaded.image().close(); return; }
            if (error != null || loaded == null) { failed = true; return; }
            NativeImage image = loaded.image(); dynamicTexture = new DynamicTexture(image);
            onlineTexture = new ResourceLocation("liymod", "online_card/" + Integer.toUnsignedString(onlineUrl.hashCode(), 16) + "_" + Long.toUnsignedString(System.nanoTime(), 16));
            minecraft.getTextureManager().register(onlineTexture, dynamicTexture); onlineWidth = loaded.width(); onlineHeight = loaded.height();
        }));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0101010);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        Image image = current();
        if (image == null) graphics.drawCenteredString(font, Component.translatable(failed ? "gui.liymod.card.load_failed" : "gui.liymod.card.loading"), width / 2, height / 2, 0xFFFFFFFF);
        else {
            int[] b = bounds(image);
            graphics.pose().pushPose();
            graphics.pose().translate(b[0], b[1], 0.0F);
            graphics.pose().scale((float) b[2] / image.width, (float) b[3] / image.height, 1.0F);
            graphics.blit(image.texture, 0, 0, 0, 0, image.width, image.height, image.width, image.height);
            graphics.pose().popPose();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    private Image current() {
        if (onlineUrl != null) return onlineTexture == null ? null : new Image(onlineTexture, onlineWidth, onlineHeight, onlineUrl);
        return bundled.isEmpty() ? null : bundled.get(index);
    }
    private int[] bounds(Image image) {
        double fit = Math.min((double) (width - 40) / image.width, (double) (height - 76) / image.height);
        int w = Math.max(1, (int) (image.width * fit * zoom)), h = Math.max(1, (int) (image.height * fit * zoom));
        return new int[]{(int) ((width - w) / 2.0 + panX), (int) (30 + (height - 76 - h) / 2.0 + panY), w, h};
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) { zoom = Math.max(0.25D, Math.min(4.0D, zoom * Math.pow(1.12D, delta))); return true; }
    @Override public boolean mouseClicked(double x, double y, int button) {
        Image image = current();
        if (button == 0 && image != null && contains(bounds(image), x, y)) {
            dragging = true; dragMoved = false; pressX = lastX = x; pressY = lastY = y; return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging && button == 0) {
            double totalX = x - pressX, totalY = y - pressY;
            if (totalX * totalX + totalY * totalY >= 9.0D) dragMoved = true;
            if (dragMoved) { panX += x - lastX; panY += y - lastY; }
            lastX = x; lastY = y; return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && dragging) {
            double totalX = x - pressX, totalY = y - pressY;
            boolean moved = dragMoved || totalX * totalX + totalY * totalY >= 9.0D;
            dragging = false;
            Image image = current();
            if (!moved && image != null && image.link != null && contains(bounds(image), x, y)) {
                ConfirmLinkScreen.confirmLinkNow(image.link, this, false);
            }
            return true;
        }
        return super.mouseReleased(x, y, button);
    }
    private static boolean contains(int[] bounds, double x, double y) {
        return x >= bounds[0] && x < bounds[0] + bounds[2] && y >= bounds[1] && y < bounds[1] + bounds[3];
    }
    private void reset() { zoom = 1.0D; panX = panY = 0.0D; }
    @Override public void removed() { if (dynamicTexture != null) { dynamicTexture.close(); dynamicTexture = null; onlineTexture = null; } super.removed(); }
}
