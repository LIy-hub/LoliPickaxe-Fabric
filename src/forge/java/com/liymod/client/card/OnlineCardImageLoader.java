package com.liymod.client.card;

import com.liymod.item.LoliCardData;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.HttpsURLConnection;
import net.minecraft.Util;

final class OnlineCardImageLoader {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    record Loaded(NativeImage image, int width, int height) { }
    private OnlineCardImageLoader() { }
    static CompletableFuture<Loaded> load(String value) {
        return CompletableFuture.supplyAsync(() -> {
            try { return download(value); } catch (IOException exception) { throw new java.util.concurrent.CompletionException(exception); }
        }, Util.backgroundExecutor());
    }
    private static Loaded download(String value) throws IOException {
        if (!LoliCardData.isSafeHttpsUrl(value)) throw new IOException("Unsafe URL");
        URI uri = URI.create(value);
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()) throw new IOException("Private address rejected");
        }
        HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(4000); connection.setReadTimeout(4000);
        connection.setRequestProperty("Accept", "image/png,image/jpeg,image/*;q=0.8");
        connection.setRequestProperty("User-Agent", "LoliPickaxe-Forge/online-card");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("HTTP status rejected");
            String type = connection.getContentType(); if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("image/")) throw new IOException("Not an image");
            if (connection.getContentLengthLong() > MAX_BYTES) throw new IOException("Image too large");
            byte[] bytes;
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int total = 0, read;
                while ((read = input.read(buffer)) >= 0) { total += read; if (total > MAX_BYTES) throw new IOException("Image too large"); output.write(buffer, 0, read); }
                bytes = output.toByteArray();
            }
            NativeImage image = NativeImage.read(bytes); int width = image.getWidth(), height = image.getHeight();
            if (width <= 0 || height <= 0 || width > 4096 || height > 4096 || (long) width * height > 16_777_216L) { image.close(); throw new IOException("Image dimensions rejected"); }
            return new Loaded(image, width, height);
        } finally { connection.disconnect(); }
    }
}
