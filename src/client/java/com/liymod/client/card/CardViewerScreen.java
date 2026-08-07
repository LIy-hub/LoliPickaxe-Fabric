package com.liymod.client.card;

import com.liymod.item.LoliCardCatalog;
import com.liymod.item.LoliCardData;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Bundled album/card viewer and bounded asynchronous online-card viewer. */
final class CardViewerScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0xE0101010;
    private static final int PLACEHOLDER_COLOR = 0xFF292929;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final double MIN_ZOOM = 0.25D;
    private static final double MAX_ZOOM = 4.0D;
    private static final double DRAG_THRESHOLD_SQUARED = 9.0D;
    private static final String GK_ARTWORK_URL = "https://www.pixiv.net/artworks/61282195";
    private static final String XIAOMO_ARTIST_URL = "https://www.pixiv.net/users/5776001";

    private enum OnlineState {
        NONE,
        LOADING,
        READY,
        FAILED
    }

    private record ImageEntry(Identifier texture, int width, int height, String link) {
    }

    private record ImageBounds(int left, int top, int width, int height) {
        boolean contains(double x, double y) {
            return x >= left && x < left + width && y >= top && y < top + height;
        }
    }

    private final List<ImageEntry> bundledImages;
    private final String onlineUrl;
    private final boolean album;
    private OnlineState onlineState;
    private Identifier onlineTexture;
    private int onlineWidth;
    private int onlineHeight;
    private CompletableFuture<OnlineCardImageLoader.LoadedImage> onlineLoad;
    private int imageIndex;
    private double zoom = 1.0D;
    private double panX;
    private double panY;
    private boolean dragging;
    private boolean dragMoved;
    private double pressMouseX;
    private double pressMouseY;
    private double lastMouseX;
    private double lastMouseY;
    private boolean removed;

    private CardViewerScreen(
            Component title,
            List<ImageEntry> bundledImages,
            String onlineUrl,
            boolean album
    ) {
        super(title);
        this.bundledImages = List.copyOf(bundledImages);
        this.onlineUrl = onlineUrl;
        this.album = album;
        if (onlineUrl == null) {
            onlineState = OnlineState.NONE;
        } else if (LoliCardData.isSafeHttpsUrl(onlineUrl)) {
            onlineState = OnlineState.LOADING;
        } else {
            onlineState = OnlineState.FAILED;
        }
    }

    static CardViewerScreen bundled(boolean album, List<LoliCardCatalog.Art> art) {
        List<ImageEntry> images = new ArrayList<>();
        for (LoliCardCatalog.Art entry : art) {
            ImageDimensions size = bundledDimensions(entry.resourceName());
            String link = entry.group().equals(LoliCardCatalog.DAUGHTER_GROUP)
                    ? XIAOMO_ARTIST_URL
                    : entry.id().equals("gk_head_portrait") ? GK_ARTWORK_URL : null;
            images.add(new ImageEntry(
                    Identifier.fromNamespaceAndPath(
                            "liymod",
                            "textures/lolicards/" + entry.resourceName() + ".png"),
                    size.width(),
                    size.height(),
                    link));
        }
        return new CardViewerScreen(
                Component.translatable(album ? "gui.liymod.card.album_title" : "gui.liymod.card.title"),
                images,
                null,
                album);
    }

    static CardViewerScreen online(String url) {
        return new CardViewerScreen(
                Component.translatable("gui.liymod.card.online_title"),
                List.of(),
                url == null ? "" : url.strip(),
                false);
    }

    @Override
    protected void init() {
        int buttonY = height - 26;
        if (album && bundledImages.size() > 1) {
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.liymod.card.previous"),
                            button -> changePage(-1))
                    .bounds(width / 2 - 125, buttonY, 75, 20)
                    .build());
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.liymod.card.next"),
                            button -> changePage(1))
                    .bounds(width / 2 + 50, buttonY, 75, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.card.close"),
                        button -> onClose())
                .bounds(width / 2 - 40, buttonY, 80, 20)
                .build());
        if (onlineState == OnlineState.LOADING) {
            startOnlineLoad();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        graphics.fill(0, 0, width, height, BACKGROUND_COLOR);
        graphics.centeredText(font, title, width / 2, 8, TEXT_COLOR);

        ImageEntry image = currentImage();
        if (image != null) {
            drawImage(graphics, image);
        } else {
            drawPlaceholder(graphics);
        }

        if (album && !bundledImages.isEmpty()) {
            graphics.centeredText(
                    font,
                    Component.literal((imageIndex + 1) + " / " + bundledImages.size()),
                    width / 2,
                    21,
                    TEXT_COLOR);
        }
        if (onlineUrl != null) {
            graphics.centeredText(font, elidedUrl(), width / 2, height - 39, TEXT_COLOR);
        }
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        ImageEntry image = currentImage();
        if (event.button() == 0 && image != null && imageBounds(image).contains(event.x(), event.y())) {
            dragging = true;
            dragMoved = false;
            pressMouseX = event.x();
            pressMouseY = event.y();
            lastMouseX = event.x();
            lastMouseY = event.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging && event.button() == 0) {
            double totalX = event.x() - pressMouseX;
            double totalY = event.y() - pressMouseY;
            if (totalX * totalX + totalY * totalY >= DRAG_THRESHOLD_SQUARED) {
                dragMoved = true;
            }
            if (dragMoved) {
                panX += event.x() - lastMouseX;
                panY += event.y() - lastMouseY;
            }
            lastMouseX = event.x();
            lastMouseY = event.y();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && dragging) {
            double totalX = event.x() - pressMouseX;
            double totalY = event.y() - pressMouseY;
            boolean wasDragged = dragMoved
                    || totalX * totalX + totalY * totalY >= DRAG_THRESHOLD_SQUARED;
            dragging = false;
            ImageEntry image = currentImage();
            if (!wasDragged
                    && image != null
                    && image.link() != null
                    && imageBounds(image).contains(event.x(), event.y())) {
                ConfirmLinkScreen.confirmLinkNow(this, image.link());
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentImage() == null || verticalAmount == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        double oldZoom = zoom;
        zoom = Math.clamp(zoom * Math.pow(1.12D, verticalAmount), MIN_ZOOM, MAX_ZOOM);
        double ratio = zoom / oldZoom;
        double oldCenterX = width / 2.0D + panX;
        double oldCenterY = height / 2.0D + panY;
        panX = mouseX - (mouseX - oldCenterX) * ratio - width / 2.0D;
        panY = mouseY - (mouseY - oldCenterY) * ratio - height / 2.0D;
        return true;
    }

    @Override
    public void removed() {
        removed = true;
        if (onlineTexture != null && minecraft != null) {
            minecraft.getTextureManager().release(onlineTexture);
            onlineTexture = null;
        }
        super.removed();
    }

    private void startOnlineLoad() {
        if (onlineLoad != null) {
            return;
        }
        onlineLoad = OnlineCardImageLoader.load(onlineUrl);
        onlineLoad.whenComplete((loaded, error) -> Minecraft.getInstance().execute(() -> {
            if (removed || minecraft == null || minecraft.gui.screen() != this) {
                if (loaded != null) {
                    loaded.image().close();
                }
                return;
            }
            if (error != null || loaded == null) {
                onlineState = OnlineState.FAILED;
                return;
            }
            installOnlineTexture(loaded);
        }));
    }

    private void installOnlineTexture(OnlineCardImageLoader.LoadedImage loaded) {
        NativeImage image = loaded.image();
        Identifier texture = Identifier.fromNamespaceAndPath(
                "liymod",
                "online_card/" + Integer.toUnsignedString(onlineUrl.hashCode(), 16)
                        + "_" + Long.toUnsignedString(System.nanoTime(), 16));
        DynamicTexture dynamicTexture = null;
        try {
            dynamicTexture = new DynamicTexture(() -> "LoliPickaxe online card", image);
            minecraft.getTextureManager().register(texture, dynamicTexture);
            onlineTexture = texture;
            onlineWidth = loaded.width();
            onlineHeight = loaded.height();
            onlineState = OnlineState.READY;
            resetView();
        } catch (RuntimeException exception) {
            if (dynamicTexture == null) {
                image.close();
            } else {
                dynamicTexture.close();
            }
            onlineState = OnlineState.FAILED;
        }
    }

    private ImageEntry currentImage() {
        if (onlineUrl != null) {
            return onlineState == OnlineState.READY && onlineTexture != null
                    ? new ImageEntry(
                            onlineTexture,
                            onlineWidth,
                            onlineHeight,
                            LoliCardData.isSafeHttpsUrl(onlineUrl) ? onlineUrl : null)
                    : null;
        }
        return bundledImages.isEmpty() ? null : bundledImages.get(imageIndex);
    }

    private void drawImage(GuiGraphicsExtractor graphics, ImageEntry image) {
        ImageBounds bounds = imageBounds(image);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                image.texture(),
                bounds.left(),
                bounds.top(),
                0.0F,
                0.0F,
                bounds.width(),
                bounds.height(),
                image.width(),
                image.height(),
                image.width(),
                image.height());
    }

    private ImageBounds imageBounds(ImageEntry image) {
        int availableWidth = Math.max(1, width - 40);
        int availableHeight = Math.max(1, height - (onlineUrl == null ? 68 : 80));
        double fit = Math.min(
                (double) availableWidth / image.width(),
                (double) availableHeight / image.height());
        int drawWidth = Math.max(1, (int) Math.round(image.width() * fit * zoom));
        int drawHeight = Math.max(1, (int) Math.round(image.height() * fit * zoom));
        int left = (int) Math.round((width - drawWidth) / 2.0D + panX);
        int top = (int) Math.round(30.0D + (availableHeight - drawHeight) / 2.0D + panY);
        return new ImageBounds(left, top, drawWidth, drawHeight);
    }

    private void drawPlaceholder(GuiGraphicsExtractor graphics) {
        int placeholderWidth = Math.min(320, Math.max(120, width - 80));
        int placeholderHeight = Math.min(180, Math.max(80, height - 120));
        int left = (width - placeholderWidth) / 2;
        int top = (height - placeholderHeight) / 2;
        graphics.fill(left, top, left + placeholderWidth, top + placeholderHeight, PLACEHOLDER_COLOR);
        Component message = onlineState == OnlineState.LOADING
                ? Component.translatable("gui.liymod.card.loading")
                : Component.translatable("gui.liymod.card.load_failed");
        graphics.centeredText(font, message, width / 2, top + placeholderHeight / 2 - 4, TEXT_COLOR);
    }

    private void changePage(int delta) {
        if (bundledImages.isEmpty()) {
            return;
        }
        imageIndex = Math.floorMod(imageIndex + Integer.signum(delta), bundledImages.size());
        resetView();
    }

    private void resetView() {
        zoom = 1.0D;
        panX = 0.0D;
        panY = 0.0D;
        dragging = false;
        dragMoved = false;
    }

    private String elidedUrl() {
        int maximumWidth = Math.max(40, width - 40);
        if (font.width(onlineUrl) <= maximumWidth) {
            return onlineUrl;
        }
        String suffix = "...";
        return font.plainSubstrByWidth(onlineUrl, maximumWidth - font.width(suffix)) + suffix;
    }

    private static ImageDimensions bundledDimensions(String resourceName) {
        return switch (resourceName) {
            case "altar_guide" -> new ImageDimensions(63, 63);
            case "card_xiaomo_1" -> new ImageDimensions(656, 1000);
            case "card_xiaomo_2" -> new ImageDimensions(644, 1000);
            case "card_xiaomo_3" -> new ImageDimensions(1157, 1637);
            case "card_xiaomo_4" -> new ImageDimensions(2890, 4092);
            case "card_xiaomo_5" -> new ImageDimensions(1060, 1500);
            case "card_xiaomo_6" -> new ImageDimensions(1920, 1237);
            case "card_xiaomo_7" -> new ImageDimensions(1301, 2015);
            case "card_xiaomo_8" -> new ImageDimensions(1018, 1500);
            case "gk_head_portrait" -> new ImageDimensions(1000, 1000);
            default -> new ImageDimensions(1, 1);
        };
    }

    private record ImageDimensions(int width, int height) {
    }
}
