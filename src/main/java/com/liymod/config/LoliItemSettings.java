package com.liymod.config;

import com.liymod.item.ModItems;
import com.liymod.nbt.LoliCustomData;
import java.util.Optional;
import java.util.UUID;
import com.liymod.compat.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.liymod.compat.CustomData;

/** Validated per-item settings and ownership stored without replacing unrelated CUSTOM_DATA. */
public final class LoliItemSettings {
    private static final String ROOT_KEY = "LoliFinal";
    private static final String SETTINGS_KEY = "Settings";
    private static final String OWNER_UUID_KEY = "OwnerUuid";
    private static final String OWNER_NAME_KEY = "OwnerName";
    private static final String SETTINGS_REVISION_KEY = "SettingsRevision";
    private static final String SERVER_MINING_LIMIT_KEY = "ServerMiningLimit";
    private static final int CURRENT_SETTINGS_REVISION = 2;

    private LoliItemSettings() {
    }

    public static boolean isFinalPickaxe(ItemStack stack) {
        return stack.is(ModItems.LOLI_PICKAXE);
    }

    public static boolean getBoolean(ItemStack stack, LoliConfigOption option) {
        requireType(option, LoliConfigOption.ValueType.BOOLEAN);
        return (Boolean) value(stack, option);
    }

    public static int getInt(ItemStack stack, LoliConfigOption option) {
        requireType(option, LoliConfigOption.ValueType.INTEGER);
        return (Integer) value(stack, option);
    }

    public static double getDouble(ItemStack stack, LoliConfigOption option) {
        requireType(option, LoliConfigOption.ValueType.DOUBLE);
        return (Double) value(stack, option);
    }

    public static String getString(ItemStack stack, LoliConfigOption option) {
        requireType(option, LoliConfigOption.ValueType.STRING);
        return (String) value(stack, option);
    }

    public static boolean set(ItemStack stack, LoliConfigOption option, String encoded) {
        if (!isFinalPickaxe(stack) || !option.itemOverride()) {
            return false;
        }
        Object parsed;
        try {
            parsed = option.parse(encoded);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (option == LoliConfigOption.MINING_RADIUS) {
            parsed = Math.min(
                    (Integer) parsed,
                    LoliServerConfig.getInt(LoliConfigOption.MAX_MINING_RANGE)
            );
        }
        Object validated = parsed;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag loli = root.getCompound(ROOT_KEY);
            CompoundTag settings = loli.getCompound(SETTINGS_KEY);
            put(settings, option, validated);
            loli.put(SETTINGS_KEY, settings);
            loli.putInt(SERVER_MINING_LIMIT_KEY, LoliServerConfig.getInt(LoliConfigOption.MAX_MINING_RANGE));
            root.put(ROOT_KEY, loli);
        });
        return true;
    }

    /** Materializes server defaults so remote clients never fall back to their own config file. */
    public static void ensureDefaults(ItemStack stack) {
        if (!isFinalPickaxe(stack)) {
            return;
        }
        CompoundTag currentRoot = root(stack);
        CompoundTag existing = currentRoot.getCompound(SETTINGS_KEY);
        int revision = com.liymod.compat.LegacyNbt.getIntOr(currentRoot, SETTINGS_REVISION_KEY, 0);
        int miningLimit = LoliServerConfig.getInt(LoliConfigOption.MAX_MINING_RANGE);
        boolean limitChanged = com.liymod.compat.LegacyNbt.getIntOr(currentRoot, SERVER_MINING_LIMIT_KEY, -1) != miningLimit;
        boolean missing = false;
        for (LoliConfigOption option : LoliConfigOption.values()) {
            if (option.itemOverride() && !existing.contains(option.id())) {
                missing = true;
                break;
            }
        }
        boolean migrateReach = revision < 2
                && existing.contains(LoliConfigOption.BLOCK_REACH_DISTANCE.id())
                && com.liymod.compat.LegacyNbt.getDoubleOr(existing, LoliConfigOption.BLOCK_REACH_DISTANCE.id(), 0.0D) == 0.0D;
        if (!missing && !migrateReach && !limitChanged && revision >= CURRENT_SETTINGS_REVISION) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag loli = root.getCompound(ROOT_KEY);
            CompoundTag settings = loli.getCompound(SETTINGS_KEY);
            for (LoliConfigOption option : LoliConfigOption.values()) {
                if (option.itemOverride() && !settings.contains(option.id())) {
                    put(settings, option, LoliServerConfig.get(option));
                }
            }
            if (migrateReach) {
                put(settings, LoliConfigOption.BLOCK_REACH_DISTANCE, 1024.0D);
            }
            loli.put(SETTINGS_KEY, settings);
            loli.putInt(SETTINGS_REVISION_KEY, CURRENT_SETTINGS_REVISION);
            loli.putInt(SERVER_MINING_LIMIT_KEY, miningLimit);
            root.put(ROOT_KEY, loli);
        });
    }

    public static int getMiningRadius(ItemStack stack) {
        if (!isFinalPickaxe(stack)) {
            return 0;
        }
        int maximum = com.liymod.compat.LegacyMath.clamp(com.liymod.compat.LegacyNbt.getIntOr(root(stack), SERVER_MINING_LIMIT_KEY,
                LoliServerConfig.getInt(LoliConfigOption.MAX_MINING_RANGE)), 0, 5);
        return com.liymod.compat.LegacyMath.clamp(getInt(stack, LoliConfigOption.MINING_RADIUS), 0, maximum);
    }

    /** Mining always checks the current server policy, even before the next item tick. */
    public static int getServerMiningRadius(ItemStack stack) {
        return isFinalPickaxe(stack) ? com.liymod.compat.LegacyMath.clamp(getInt(stack, LoliConfigOption.MINING_RADIUS),
                0, LoliServerConfig.getInt(LoliConfigOption.MAX_MINING_RANGE)) : 0;
    }

    public static int cycleMiningRadius(ItemStack stack) {
        if (!isFinalPickaxe(stack)) {
            return 0;
        }
        int maximum = LoliServerConfig.getInt(LoliConfigOption.MAX_MINING_RANGE);
        int current = getServerMiningRadius(stack);
        int next = current >= maximum ? 0 : current + 1;
        set(stack, LoliConfigOption.MINING_RADIUS, Integer.toString(next));
        return next;
    }

    public static void bindOwnerIfAbsent(ItemStack stack, Player player) {
        if (!isFinalPickaxe(stack) || ownerUuid(stack).isPresent()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag loli = root.getCompound(ROOT_KEY);
            loli.putString(OWNER_UUID_KEY, player.getUUID().toString());
            loli.putString(OWNER_NAME_KEY, player.getName().getString());
            root.put(ROOT_KEY, loli);
        });
    }

    public static Optional<UUID> ownerUuid(ItemStack stack) {
        String encoded = com.liymod.compat.LegacyNbt.getStringOr(root(stack), OWNER_UUID_KEY, "");
        if (encoded.isEmpty() || encoded.length() > 36) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(encoded));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static boolean isOwner(ItemStack stack, Player player) {
        return ownerUuid(stack).map(player.getUUID()::equals).orElse(true);
    }

    private static Object value(ItemStack stack, LoliConfigOption option) {
        Object fallback = LoliServerConfig.get(option);
        if (!option.itemOverride() || !isFinalPickaxe(stack)) {
            return fallback;
        }
        CompoundTag settings = root(stack).getCompound(SETTINGS_KEY);
        if (!settings.contains(option.id())) {
            return fallback;
        }
        return switch (option.type()) {
            case BOOLEAN -> com.liymod.compat.LegacyNbt.getBooleanOr(settings, option.id(), (Boolean) fallback);
            case INTEGER -> option.sanitize(com.liymod.compat.LegacyNbt.getIntOr(settings, option.id(), (Integer) fallback));
            case DOUBLE -> option.sanitize(com.liymod.compat.LegacyNbt.getDoubleOr(settings, option.id(), (Double) fallback));
            case STRING -> option.sanitize(com.liymod.compat.LegacyNbt.getStringOr(settings, option.id(), (String) fallback));
        };
    }

    private static CompoundTag root(ItemStack stack) {
        CompoundTag custom = LoliCustomData.view(stack);
        return custom.getCompound(ROOT_KEY);
    }

    private static void put(CompoundTag tag, LoliConfigOption option, Object value) {
        switch (option.type()) {
            case BOOLEAN -> tag.putBoolean(option.id(), (Boolean) value);
            case INTEGER -> tag.putInt(option.id(), (Integer) value);
            case DOUBLE -> tag.putDouble(option.id(), (Double) value);
            case STRING -> tag.putString(option.id(), (String) value);
        }
    }

    private static void requireType(LoliConfigOption option, LoliConfigOption.ValueType expected) {
        if (option.type() != expected) {
            throw new IllegalArgumentException("Wrong option type for " + option.id());
        }
    }
}
