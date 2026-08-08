package com.liymod.config;

import com.liymod.LiyMod;
import com.liymod.safe.SafeTntEffect;
import com.liymod.safe.SafeTntEffectService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/** Small, dependency-free, server-authoritative configuration store. */
public final class LoliServerConfig {
    private static final String FILE_NAME = "liymod-loli.properties";
    private static final String REVISION_KEY = "config_revision";
    private static final int CURRENT_REVISION = 2;
    private static final Map<LoliConfigOption, Object> VALUES = new EnumMap<>(LoliConfigOption.class);

    private static Path path;

    private LoliServerConfig() {
    }

    public static synchronized void initialize() {
        path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        reload();
    }

    public static synchronized void reload() {
        resetDefaults();
        if (path == null) {
            path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        }
        Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException exception) {
                LiyMod.LOGGER.warn("Unable to read {}; using safe defaults", path, exception);
            }
        }
        for (LoliConfigOption option : LoliConfigOption.values()) {
            String encoded = properties.getProperty(option.id());
            if (encoded == null) {
                continue;
            }
            try {
                VALUES.put(option, option.parse(encoded));
            } catch (IllegalArgumentException exception) {
                LiyMod.LOGGER.warn("Ignoring invalid Loli config value {}={}", option.id(), encoded);
            }
        }
        migrate(properties);
        synchronizeSafeEffects();
        save();
    }

    public static synchronized boolean set(LoliConfigOption option, String encoded) {
        try {
            VALUES.put(option, option.parse(encoded));
        } catch (IllegalArgumentException exception) {
            return false;
        }
        synchronizeSafeEffects();
        save();
        return true;
    }

    public static synchronized Object get(LoliConfigOption option) {
        return VALUES.getOrDefault(option, option.defaultValue());
    }

    public static boolean getBoolean(LoliConfigOption option) {
        return (Boolean) getTyped(option, LoliConfigOption.ValueType.BOOLEAN);
    }

    public static int getInt(LoliConfigOption option) {
        return (Integer) getTyped(option, LoliConfigOption.ValueType.INTEGER);
    }

    public static double getDouble(LoliConfigOption option) {
        return (Double) getTyped(option, LoliConfigOption.ValueType.DOUBLE);
    }

    public static String getString(LoliConfigOption option) {
        return (String) getTyped(option, LoliConfigOption.ValueType.STRING);
    }

    public static synchronized void save() {
        if (path == null) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty(REVISION_KEY, Integer.toString(CURRENT_REVISION));
        for (LoliConfigOption option : LoliConfigOption.values()) {
            properties.setProperty(option.id(), option.encode(get(option)));
        }
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "LiyMod Loli server configuration");
            }
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LiyMod.LOGGER.error("Unable to save Loli config to {}", path, exception);
        }
    }

    private static synchronized void resetDefaults() {
        VALUES.clear();
        for (LoliConfigOption option : LoliConfigOption.values()) {
            VALUES.put(option, option.defaultValue());
        }
    }

    private static void migrate(Properties properties) {
        int revision = 0;
        try {
            revision = Integer.parseInt(properties.getProperty(REVISION_KEY, "0"));
        } catch (NumberFormatException ignored) {
            // Treat malformed revision metadata as an older configuration.
        }
        if (revision < 2
                && "32".equals(properties.getProperty(LoliConfigOption.ENCHANTMENT_LEVEL_LIMIT.id()))) {
            VALUES.put(LoliConfigOption.ENCHANTMENT_LEVEL_LIMIT, 32768);
        }
    }

    private static Object getTyped(LoliConfigOption option, LoliConfigOption.ValueType expected) {
        if (option.type() != expected) {
            throw new IllegalArgumentException("Wrong option type for " + option.id());
        }
        return get(option);
    }

    private static void synchronizeSafeEffects() {
        SafeTntEffectService.configure(new SafeTntEffectService.Settings(
                getBoolean(LoliConfigOption.SAFE_BLUE_SCREEN),
                getBoolean(LoliConfigOption.SAFE_EXIT),
                getBoolean(LoliConfigOption.SAFE_FAIL_RESPOND)
        ));
    }
}
