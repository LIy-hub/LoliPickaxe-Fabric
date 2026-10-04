package com.liymod.item;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Bounded card metadata stored without overwriting unrelated custom data. */
public final class LoliCardData {
    public static final int MAX_URL_LENGTH = 512;
    private static final String ROOT_KEY = "LoliCard";
    private static final String ART_KEY = "Picture";
    private static final String GROUP_KEY = "PictureGroup";
    private static final String URL_KEY = "ImageUrl";

    private LoliCardData() {
    }

    public static Optional<String> art(ItemStack stack) {
        String id = root(stack).getStringOr(ART_KEY, "");
        return LoliCardCatalog.byId(id).map(LoliCardCatalog.Art::id);
    }

    public static void setArt(ItemStack stack, String id) {
        if (LoliCardCatalog.byId(id).isEmpty()) {
            throw new IllegalArgumentException("Unknown bundled card art: " + id);
        }
        update(stack, tag -> tag.putString(ART_KEY, id));
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
    }

    public static Optional<String> group(ItemStack stack) {
        String group = root(stack).getStringOr(GROUP_KEY, "");
        return LoliCardCatalog.GROUPS.contains(group) ? Optional.of(group) : Optional.empty();
    }

    public static void setGroup(ItemStack stack, String group) {
        if (!LoliCardCatalog.GROUPS.contains(group)) {
            throw new IllegalArgumentException("Unknown bundled card group: " + group);
        }
        update(stack, tag -> tag.putString(GROUP_KEY, group));
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
    }

    public static String url(ItemStack stack) {
        String url = root(stack).getStringOr(URL_KEY, "");
        return isSafeHttpsUrl(url) ? url : "";
    }

    public static boolean setUrl(ItemStack stack, String url) {
        String normalized = url == null ? "" : url.strip();
        if (!normalized.isEmpty() && !isSafeHttpsUrl(normalized)) {
            return false;
        }
        update(stack, tag -> tag.putString(URL_KEY, normalized));
        return true;
    }

    public static boolean isSafeHttpsUrl(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_URL_LENGTH) {
            return false;
        }
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && isPublicHostname(host)
                    && uri.getUserInfo() == null
                    && uri.getFragment() == null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static boolean isPublicHostname(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = host.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.contains(".")
                || normalized.equals("localhost")
                || normalized.endsWith(".localhost")
                || normalized.endsWith(".local")
                || normalized.endsWith(".internal")
                || normalized.endsWith(".lan")
                || normalized.contains(":")) {
            return false;
        }
        // Numeric hosts bypass normal DNS-name expectations and can address loopback/private ranges.
        String[] labels = normalized.split("\\.", -1);
        if (labels.length == 4) {
            boolean numeric = true;
            for (String label : labels) {
                try {
                    int octet = Integer.parseInt(label);
                    numeric &= octet >= 0 && octet <= 255;
                } catch (NumberFormatException exception) {
                    numeric = false;
                }
            }
            if (numeric) {
                return false;
            }
        }
        return true;
    }

    private static CompoundTag root(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getCompoundOrEmpty(ROOT_KEY);
    }

    private static void update(ItemStack stack, java.util.function.Consumer<CompoundTag> consumer) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, custom -> {
            CompoundTag root = custom.getCompoundOrEmpty(ROOT_KEY);
            consumer.accept(root);
            custom.put(ROOT_KEY, root);
        });
    }
}
