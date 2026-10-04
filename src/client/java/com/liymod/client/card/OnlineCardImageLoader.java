package com.liymod.client.card;

import com.liymod.item.LoliCardData;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLConnection;
import java.time.Duration;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.net.ssl.HttpsURLConnection;
import net.minecraft.util.Util;

/** Bounded HTTPS image download and decode, always executed away from the render thread. */
final class OnlineCardImageLoader {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 16_777_216L;
    private static final int IO_TIMEOUT_MILLIS = 4_000;
    private static final long TOTAL_TIMEOUT_NANOS = Duration.ofSeconds(8).toNanos();

    record LoadedImage(NativeImage image, int width, int height) {
    }

    private OnlineCardImageLoader() {
    }

    static CompletableFuture<LoadedImage> load(String encodedUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return download(encodedUrl);
            } catch (IOException | RuntimeException exception) {
                throw new CompletionException(exception);
            }
        }, Util.nonCriticalIoPool());
    }

    private static LoadedImage download(String encodedUrl) throws IOException {
        if (!LoliCardData.isSafeHttpsUrl(encodedUrl)) {
            throw new IOException("Online cards require a safe HTTPS URL");
        }
        long deadline = System.nanoTime() + TOTAL_TIMEOUT_NANOS;
        URI uri = URI.create(encodedUrl);
        URLConnection rawConnection = uri.toURL().openConnection();
        if (!(rawConnection instanceof HttpsURLConnection connection)) {
            throw new IOException("Online card URL is not HTTPS");
        }

        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setConnectTimeout(remainingTimeout(deadline));
        connection.setReadTimeout(remainingTimeout(deadline));
        connection.setRequestProperty("Accept", "image/png,image/jpeg,image/*;q=0.8");
        connection.setRequestProperty("User-Agent", "LoliPickaxe-Fabric/online-card");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Online card response was rejected: " + status);
            }
            String contentType = connection.getContentType();
            if (contentType == null
                    || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new IOException("Online card response is not an image");
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_BYTES) {
                throw new IOException("Online card exceeds the 8 MiB limit");
            }

            byte[] bytes;
            try (InputStream input = connection.getInputStream();
                    ByteArrayOutputStream output = new ByteArrayOutputStream(initialCapacity(contentLength))) {
                byte[] buffer = new byte[8192];
                int total = 0;
                boolean completed = false;
                while (System.nanoTime() < deadline) {
                    connection.setReadTimeout(remainingTimeout(deadline));
                    int read = input.read(buffer);
                    if (read < 0) {
                        completed = true;
                        break;
                    }
                    if (read == 0) {
                        continue;
                    }
                    total += read;
                    if (total > MAX_BYTES) {
                        throw new IOException("Online card exceeds the 8 MiB limit");
                    }
                    output.write(buffer, 0, read);
                }
                if (!completed) {
                    throw new SocketTimeoutException("Online card request exceeded its total timeout");
                }
                bytes = output.toByteArray();
            }

            ImageSize metadata = inspectDimensions(bytes);
            NativeImage image = NativeImage.read(bytes);
            if (!dimensionsAllowed(image.getWidth(), image.getHeight())) {
                image.close();
                throw new IOException("Decoded online card dimensions are too large");
            }
            if (image.getWidth() != metadata.width() || image.getHeight() != metadata.height()) {
                image.close();
                throw new IOException("Online card image dimensions changed during decoding");
            }
            return new LoadedImage(image, image.getWidth(), image.getHeight());
        } finally {
            connection.disconnect();
        }
    }

    private static ImageSize inspectDimensions(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IOException("Unable to inspect online card image");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported online card image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (!dimensionsAllowed(width, height)) {
                    throw new IOException("Online card image dimensions are too large");
                }
                return new ImageSize(width, height);
            } finally {
                reader.dispose();
            }
        }
    }

    private static boolean dimensionsAllowed(int width, int height) {
        return width > 0
                && height > 0
                && width <= MAX_DIMENSION
                && height <= MAX_DIMENSION
                && (long) width * height <= MAX_PIXELS;
    }

    private static int remainingTimeout(long deadline) throws SocketTimeoutException {
        long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0L) {
            throw new SocketTimeoutException("Online card request exceeded its total timeout");
        }
        long remainingMillis = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
        return (int) Math.min(IO_TIMEOUT_MILLIS, remainingMillis);
    }

    private static int initialCapacity(long contentLength) {
        if (contentLength <= 0L) {
            return 8192;
        }
        return (int) Math.min(contentLength, MAX_BYTES);
    }

    private record ImageSize(int width, int height) {
    }
}
