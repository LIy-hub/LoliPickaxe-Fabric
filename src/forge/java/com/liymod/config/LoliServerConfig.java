package com.liymod.config;

import com.liymod.LiyMod;
import com.liymod.safe.SafeEffect;
import com.liymod.safe.SafeEffectService;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import net.minecraftforge.fml.loading.FMLPaths;

/** Small validated server configuration; destructive legacy effects remain disabled by default. */
public final class LoliServerConfig {
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    private static final Properties VALUES = new Properties();
    static {
        DEFAULTS.put("inventory_protection", "false"); DEFAULTS.put("max_teleport_distance", "512");
        DEFAULTS.put("loli_card_drop_chance", "0.1"); DEFAULTS.put("loli_card_album_drop_chance", "0.01");
        DEFAULTS.put("loli_record_drop_chance", "0.001"); DEFAULTS.put("entity_soul_drop_chance", "0.01");
        DEFAULTS.put("safe_attack_command", "false"); DEFAULTS.put("safe_blue_screen", "false");
        DEFAULTS.put("safe_exit", "false"); DEFAULTS.put("safe_fail_respond", "false");
        DEFAULTS.put("dimension_blacklist", "");
        DEFAULTS.put("loli_attack", "true"); DEFAULTS.put("loli_teleport", "true");
        DEFAULTS.put("loli_speed", "1.0");
        DEFAULTS.put("reincarnation_list", ""); DEFAULTS.put("soul_redemption_list", "");
        DEFAULTS.put("soul_redemption_whitelist", "");
    }
    private LoliServerConfig() { }
    public static synchronized void load() {
        VALUES.clear(); VALUES.putAll(DEFAULTS); Path path = path();
        if (Files.isRegularFile(path)) try (Reader reader = Files.newBufferedReader(path)) { Properties read = new Properties(); read.load(reader); for (String key : DEFAULTS.keySet()) if (read.containsKey(key)) setInternal(key, read.getProperty(key)); }
        catch (IOException exception) { LiyMod.LOGGER.warn("Could not read {}", path, exception); }
        syncSafeEffects(); save();
    }
    public static synchronized boolean set(String key, String value) {
        if (!DEFAULTS.containsKey(key)) return false;
        try { setInternal(key, value); syncSafeEffects(); save(); return true; } catch (IllegalArgumentException exception) { return false; }
    }
    public static String get(String key) { return VALUES.getProperty(key, DEFAULTS.getOrDefault(key, "")); }
    public static boolean bool(String key) { return Boolean.parseBoolean(get(key)); }
    public static double number(String key) { try { return Double.parseDouble(get(key)); } catch (NumberFormatException ignored) { return Double.parseDouble(DEFAULTS.get(key)); } }
    public static Map<String, String> values() { Map<String, String> result = new LinkedHashMap<>(); DEFAULTS.keySet().forEach(key -> result.put(key, get(key))); return result; }
    private static void setInternal(String key, String value) {
        String normalized;
        if (key.equals("dimension_blacklist")) {
            normalized = normalizeDimensionList(value);
        } else if (key.endsWith("_list") || key.endsWith("_whitelist")) {
            normalized = normalizePlayerList(value);
        } else if (key.startsWith("safe_") || key.equals("inventory_protection") || key.equals("loli_attack") || key.equals("loli_teleport")) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) throw new IllegalArgumentException(); normalized = value.toLowerCase(java.util.Locale.ROOT);
        } else {
            double parsed = Double.parseDouble(value); if (!Double.isFinite(parsed)) throw new IllegalArgumentException();
            if (key.equals("max_teleport_distance")) parsed = Math.max(1.0D, Math.min(4096.0D, parsed));
            else if (key.equals("loli_speed")) parsed = Math.max(0.1D, Math.min(15.0D, parsed));
            else parsed = Math.max(0.0D, Math.min(1.0D, parsed));
            normalized = Double.toString(parsed);
        }
        VALUES.setProperty(key, normalized);
    }

    private static String normalizePlayerList(String value) {
        if (value == null || value.isBlank()) return "";
        Set<String> entries = new LinkedHashSet<>();
        for (String raw : value.split(",", -1)) {
            String entry = raw.trim();
            if (!entry.matches("[A-Za-z0-9_]{1,16}")) {
                try { entry = UUID.fromString(entry).toString(); }
                catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Invalid player list entry", exception); }
            }
            String duplicateKey = entry.toLowerCase(java.util.Locale.ROOT);
            if (entries.stream().noneMatch(existing -> existing.toLowerCase(java.util.Locale.ROOT).equals(duplicateKey))) entries.add(entry);
            if (entries.size() > 24) throw new IllegalArgumentException("Player list exceeds 24 entries");
        }
        String normalized = String.join(",", entries);
        if (normalized.length() > 1024) throw new IllegalArgumentException("Player list is too long");
        return normalized;
    }
    private static String normalizeDimensionList(String value) {
        if (value == null || value.isBlank()) return "";
        Set<String> entries = new LinkedHashSet<>();
        for (String raw : value.split(",", -1)) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(raw.trim());
            if (id == null || !entries.add(id.toString())) {
                if (id == null) throw new IllegalArgumentException("Invalid dimension id");
                continue;
            }
            if (entries.size() > 64) throw new IllegalArgumentException("Dimension blacklist exceeds 64 entries");
        }
        String normalized = String.join(",", entries);
        if (normalized.length() > 2048) throw new IllegalArgumentException("Dimension blacklist is too long");
        return normalized;
    }
    private static void syncSafeEffects() {
        SafeEffectService.setEnabled(SafeEffect.BLUE_SCREEN, bool("safe_blue_screen"));
        SafeEffectService.setEnabled(SafeEffect.EXIT, bool("safe_exit"));
        SafeEffectService.setEnabled(SafeEffect.FAIL_RESPOND, bool("safe_fail_respond"));
    }
    private static void save() {
        Path path = path(); try { Files.createDirectories(path.getParent()); try (Writer writer = Files.newBufferedWriter(path)) { VALUES.store(writer, "LoliPickaxe Forge safe server config"); } }
        catch (IOException exception) { LiyMod.LOGGER.warn("Could not save {}", path, exception); }
    }
    private static Path path() { return FMLPaths.CONFIGDIR.get().resolve("liymod-forge.properties"); }
}
