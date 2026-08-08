package com.liymod.item;

import java.net.URI;
import net.minecraft.world.item.ItemStack;

public final class LoliCardData {
    public static final int MAX_URL_LENGTH = 512;
    private LoliCardData() { }
    public static String art(ItemStack stack) { return stack.getOrCreateTag().getString("LoliCardArt"); }
    public static void art(ItemStack stack, String id) { if (LoliCardCatalog.byId(id).isPresent()) stack.getOrCreateTag().putString("LoliCardArt", id); }
    public static String url(ItemStack stack) { String value = stack.getOrCreateTag().getString("LoliCardUrl"); return isSafeHttpsUrl(value) ? value : ""; }
    public static boolean url(ItemStack stack, String value) {
        value = value == null ? "" : value.strip();
        if (!value.isEmpty() && !isSafeHttpsUrl(value)) return false;
        stack.getOrCreateTag().putString("LoliCardUrl", value); return true;
    }
    public static boolean isSafeHttpsUrl(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_URL_LENGTH) return false;
        try {
            URI uri = URI.create(value); String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null || uri.getFragment() != null) return false;
            String normalized = host.toLowerCase(java.util.Locale.ROOT);
            if (!normalized.contains(".") || normalized.equals("localhost") || normalized.endsWith(".localhost") || normalized.endsWith(".local") || normalized.endsWith(".internal") || normalized.endsWith(".lan")) return false;
            if (normalized.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return false;
            return !normalized.equals("0:0:0:0:0:0:0:1") && !normalized.equals("::1");
        } catch (RuntimeException ignored) { return false; }
    }
}
