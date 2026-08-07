package com.liymod.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative options shared by commands and per-pickaxe settings. */
public enum LoliConfigOption {
    MAX_MINING_RANGE("max_mining_range", ValueType.INTEGER, 5, 0.0D, 5.0D, false),
    MINING_RADIUS("mining_radius", ValueType.INTEGER, 0, 0.0D, 5.0D, true),
    MANDATORY_DROP("mandatory_drop", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    STOP_ON_LIQUID("stop_on_liquid", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    AUTO_FURNACE("auto_furnace", ValueType.BOOLEAN, true, 0.0D, 1.0D, true),
    AUTO_ACCEPT("auto_accept", ValueType.BOOLEAN, true, 0.0D, 1.0D, true),
    THORNS("thorns", ValueType.BOOLEAN, true, 0.0D, 1.0D, true),
    BLOCK_REACH_DISTANCE("block_reach_distance", ValueType.DOUBLE, 0.0D, 0.0D, 20.0D, true),
    AUTO_KILL_RANGE_ENTITY("auto_kill_range_entity", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    AUTO_KILL_RANGE("auto_kill_range", ValueType.INTEGER, 5, 0.0D, 10.0D, true),
    TARGET_FRIENDLY_ENTITIES("target_friendly_entities", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    TARGET_ALL_ENTITIES("target_all_entities", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    FORCE_REMOVE("force_remove", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    CLEAR_INVENTORY("clear_inventory", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    DROP_EQUIPMENT("drop_equipment", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    KICK_PLAYER("kick_player", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    KICK_MESSAGE("kick_message", ValueType.STRING, "你被氪金萝莉踢出了服务器", 0.0D, 160.0D, true),
    REINCARNATION("reincarnation", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    REINCARNATION_LIST("reincarnation_list", ValueType.STRING, "", 0.0D, 1024.0D, false),
    SOUL_REDEMPTION("soul_redemption", ValueType.BOOLEAN, false, 0.0D, 1.0D, true),
    SOUL_REDEMPTION_LIST("soul_redemption_list", ValueType.STRING, "", 0.0D, 1024.0D, false),
    SOUL_REDEMPTION_WHITELIST("soul_redemption_whitelist", ValueType.STRING, "", 0.0D, 1024.0D, false),
    OWNER_PROTECTION("owner_protection", ValueType.BOOLEAN, true, 0.0D, 1.0D, true),
    FIND_OWNER_RANGE("find_owner_range", ValueType.INTEGER, 50, 0.0D, 128.0D, false),
    DROP_PROTECT_TICKS("drop_protect_ticks", ValueType.INTEGER, 4, 0.0D, 1200.0D, false),
    INVENTORY_PROTECTION("inventory_protection", ValueType.BOOLEAN, false, 0.0D, 1.0D, false),
    ENCHANTMENT_LEVEL_LIMIT("enchantment_level_limit", ValueType.INTEGER, 32, 0.0D, 32.0D, false),
    EFFECT_LEVEL_LIMIT("effect_level_limit", ValueType.INTEGER, 32, 0.0D, 32.0D, false),
    SPACE_FOLDING("space_folding", ValueType.BOOLEAN, true, 0.0D, 1.0D, false),
    DIMENSION_BLACKLIST("dimension_blacklist", ValueType.STRING, "", 0.0D, 128.0D, false),
    MAX_TELEPORT_DISTANCE("max_teleport_distance", ValueType.DOUBLE, 512.0D, 1.0D, 4096.0D, false),
    LOLI_CARD_DROP_CHANCE("loli_card_drop_chance", ValueType.DOUBLE, 0.1D, 0.0D, 1.0D, false),
    LOLI_CARD_ALBUM_DROP_CHANCE("loli_card_album_drop_chance", ValueType.DOUBLE, 0.01D, 0.0D, 1.0D, false),
    LOLI_RECORD_DROP_CHANCE("loli_record_drop_chance", ValueType.DOUBLE, 0.001D, 0.0D, 1.0D, false),
    ENTITY_SOUL_DROP_CHANCE("entity_soul_drop_chance", ValueType.DOUBLE, 0.01D, 0.0D, 1.0D, false),
    SAFE_ATTACK_COMMAND("safe_attack_command", ValueType.BOOLEAN, false, 0.0D, 1.0D, false),
    SAFE_BLUE_SCREEN("safe_blue_screen", ValueType.BOOLEAN, false, 0.0D, 1.0D, false),
    SAFE_EXIT("safe_exit", ValueType.BOOLEAN, false, 0.0D, 1.0D, false),
    SAFE_FAIL_RESPOND("safe_fail_respond", ValueType.BOOLEAN, false, 0.0D, 1.0D, false);

    public enum ValueType {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        STRING
    }

    private final String id;
    private final ValueType type;
    private final Object defaultValue;
    private final double minimum;
    private final double maximum;
    private final boolean itemOverride;

    LoliConfigOption(
            String id,
            ValueType type,
            Object defaultValue,
            double minimum,
            double maximum,
            boolean itemOverride
    ) {
        this.id = id;
        this.type = type;
        this.defaultValue = defaultValue;
        this.minimum = minimum;
        this.maximum = maximum;
        this.itemOverride = itemOverride;
    }

    public String id() {
        return id;
    }

    public ValueType type() {
        return type;
    }

    public Object defaultValue() {
        return defaultValue;
    }

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }

    public boolean itemOverride() {
        return itemOverride;
    }

    public String translationKey() {
        return "config.liymod.loli." + id;
    }

    public Object parse(String encoded) {
        int maximumLength = type == ValueType.STRING
                ? Math.clamp((int) maximum, 1, 2048)
                : 256;
        if (encoded == null || encoded.length() > maximumLength) {
            throw new IllegalArgumentException("Invalid value length");
        }
        return switch (type) {
            case BOOLEAN -> parseBoolean(encoded);
            case INTEGER -> Math.clamp(Integer.parseInt(encoded), (int) minimum, (int) maximum);
            case DOUBLE -> {
                double value = Double.parseDouble(encoded);
                if (!Double.isFinite(value)) {
                    throw new IllegalArgumentException("Value must be finite");
                }
                yield Math.clamp(value, minimum, maximum);
            }
            case STRING -> normalizeString(encoded.codePoints().limit(maximumLength).collect(
                    StringBuilder::new,
                    StringBuilder::appendCodePoint,
                    StringBuilder::append
            ).toString());
        };
    }

    public String encode(Object value) {
        return String.valueOf(sanitize(value));
    }

    public Object sanitize(Object value) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return parse(String.valueOf(value));
        } catch (IllegalArgumentException exception) {
            return defaultValue;
        }
    }

    public static Optional<LoliConfigOption> byId(String id) {
        if (id == null || id.length() > 64) {
            return Optional.empty();
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(option -> option.id.equals(normalized)).findFirst();
    }

    private static boolean parseBoolean(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Expected true or false");
        };
    }

    private String normalizeString(String value) {
        if (this != REINCARNATION_LIST
                && this != SOUL_REDEMPTION_LIST
                && this != SOUL_REDEMPTION_WHITELIST) {
            return value;
        }

        Map<String, String> entries = new LinkedHashMap<>();
        if (!value.isBlank()) {
            for (String rawEntry : value.split(",", -1)) {
                String entry = rawEntry.trim();
                if (entry.isEmpty() || !isPlayerIdentifier(entry)) {
                    throw new IllegalArgumentException(
                            "Player lists accept comma-separated UUIDs or 1-16 character player names"
                    );
                }
                entries.putIfAbsent(entry.toLowerCase(Locale.ROOT), entry);
                if (entries.size() > 24) {
                    throw new IllegalArgumentException("Player lists are limited to 24 entries");
                }
            }
        }
        return String.join(",", entries.values());
    }

    private static boolean isPlayerIdentifier(String value) {
        if (value.matches("[A-Za-z0-9_]{1,16}")) {
            return true;
        }
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
