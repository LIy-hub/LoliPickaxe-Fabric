package com.liymod.config;

import com.liymod.registry.ModContent;
import com.liymod.storage.LoliStorageData;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Validated per-stack settings for the Forge final pickaxe. */
public final class FinalToolSettings {
    public static final String AUTO_FURNACE = "LoliAutoFurnace";
    public static final String STOP_ON_LIQUID = "LoliStopOnLiquid";
    public static final String THORNS = "LoliThorns";
    public static final String AUTO_KILL = "LoliAutoKill";
    public static final String AUTO_KILL_RANGE = "LoliAutoKillRange";
    public static final String TARGET_FRIENDLY = "LoliTargetFriendlyEntities";
    public static final String TARGET_ALL = "LoliTargetAllEntities";
    public static final String FORCE_REMOVE = "LoliForceRemove";
    public static final String CLEAR_INVENTORY = "LoliClearInventory";
    public static final String DROP_EQUIPMENT = "LoliDropEquipment";
    public static final String KICK_PLAYER = "LoliKickPlayer";
    public static final String KICK_MESSAGE = "LoliKickMessage";
    public static final String REINCARNATION = "LoliReincarnation";
    public static final String SOUL_REDEMPTION = "LoliSoulRedemption";
    public static final String EFFECTS = "LoliEffects";
    public static final String ENCHANTMENTS = "LoliEnchantmentLevels";

    private FinalToolSettings() { }

    public static boolean isFinal(ItemStack stack) { return stack.is(ModContent.LOLI_PICKAXE.get()); }
    public static int radius(ItemStack stack) { return clamp(stack.getOrCreateTag().getInt("LoliMiningRadius"), 0, 5); }
    public static void radius(ItemStack stack, int value) { if (isFinal(stack)) stack.getOrCreateTag().putInt("LoliMiningRadius", clamp(value, 0, 5)); }
    public static boolean autoAccept(ItemStack stack) { return LoliStorageData.autoAccept(stack); }
    public static void autoAccept(ItemStack stack, boolean value) { LoliStorageData.setAutoAccept(stack, value); }
    public static boolean autoFurnace(ItemStack stack) { return bool(stack, AUTO_FURNACE, true); }
    public static boolean stopOnLiquid(ItemStack stack) { return bool(stack, STOP_ON_LIQUID, false); }
    public static boolean thorns(ItemStack stack) { return bool(stack, THORNS, true); }
    public static boolean autoKill(ItemStack stack) { return bool(stack, AUTO_KILL, false); }
    public static boolean targetFriendly(ItemStack stack) { return bool(stack, TARGET_FRIENDLY, false); }
    public static boolean targetAll(ItemStack stack) { return bool(stack, TARGET_ALL, false); }
    public static boolean forceRemove(ItemStack stack) { return bool(stack, FORCE_REMOVE, false); }
    public static boolean clearInventory(ItemStack stack) { return bool(stack, CLEAR_INVENTORY, false); }
    public static boolean dropEquipment(ItemStack stack) { return bool(stack, DROP_EQUIPMENT, false); }
    public static boolean kickPlayer(ItemStack stack) { return bool(stack, KICK_PLAYER, false); }
    public static boolean reincarnation(ItemStack stack) { return bool(stack, REINCARNATION, false); }
    public static boolean soulRedemption(ItemStack stack) { return bool(stack, SOUL_REDEMPTION, false); }
    public static String kickMessage(ItemStack stack) {
        String value = stack.getOrCreateTag().getString(KICK_MESSAGE).replace('\n', ' ').replace('\r', ' ').replace("§", "");
        return value.isBlank() ? "你被氪金萝莉踢出了服务器" : value;
    }
    public static int autoKillRange(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.contains(AUTO_KILL_RANGE) ? clamp(tag.getInt(AUTO_KILL_RANGE), 1, 10) : 5;
    }

    public static boolean set(ItemStack stack, String key, String encoded) {
        if (!isFinal(stack) || key == null || encoded == null || key.length() > 64 || encoded.length() > 192) return false;
        try {
            return switch (key) {
                case "mining_radius" -> { radius(stack, Integer.parseInt(encoded)); yield true; }
                case "auto_accept" -> { autoAccept(stack, parseBoolean(encoded)); yield true; }
                case "auto_furnace" -> { stack.getOrCreateTag().putBoolean(AUTO_FURNACE, parseBoolean(encoded)); yield true; }
                case "stop_on_liquid" -> { stack.getOrCreateTag().putBoolean(STOP_ON_LIQUID, parseBoolean(encoded)); yield true; }
                case "thorns" -> { stack.getOrCreateTag().putBoolean(THORNS, parseBoolean(encoded)); yield true; }
                case "auto_kill_range_entity" -> { stack.getOrCreateTag().putBoolean(AUTO_KILL, parseBoolean(encoded)); yield true; }
                case "auto_kill_range" -> { stack.getOrCreateTag().putInt(AUTO_KILL_RANGE, clamp(Integer.parseInt(encoded), 1, 10)); yield true; }
                case "target_friendly_entities" -> { stack.getOrCreateTag().putBoolean(TARGET_FRIENDLY, parseBoolean(encoded)); yield true; }
                case "target_all_entities" -> { stack.getOrCreateTag().putBoolean(TARGET_ALL, parseBoolean(encoded)); yield true; }
                case "force_remove" -> { stack.getOrCreateTag().putBoolean(FORCE_REMOVE, parseBoolean(encoded)); yield true; }
                case "clear_inventory" -> { stack.getOrCreateTag().putBoolean(CLEAR_INVENTORY, parseBoolean(encoded)); yield true; }
                case "drop_equipment" -> { stack.getOrCreateTag().putBoolean(DROP_EQUIPMENT, parseBoolean(encoded)); yield true; }
                case "kick_player" -> { stack.getOrCreateTag().putBoolean(KICK_PLAYER, parseBoolean(encoded)); yield true; }
                case "kick_message" -> {
                    if (encoded.length() > 160) yield false;
                    stack.getOrCreateTag().putString(KICK_MESSAGE, encoded.replace('\n', ' ').replace('\r', ' ').replace("§", ""));
                    yield true;
                }
                case "reincarnation" -> { stack.getOrCreateTag().putBoolean(REINCARNATION, parseBoolean(encoded)); yield true; }
                case "soul_redemption" -> { stack.getOrCreateTag().putBoolean(SOUL_REDEMPTION, parseBoolean(encoded)); yield true; }
                default -> false;
            };
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static Map<ResourceLocation, Integer> effects(ItemStack stack) { return intMap(stack, EFFECTS, 32); }
    public static Map<ResourceLocation, Integer> enchantments(ItemStack stack) { return intMap(stack, ENCHANTMENTS, 32768); }
    public static void setMapValue(ItemStack stack, String root, ResourceLocation id, int level, int maximum) {
        if (!isFinal(stack) || id == null) return;
        CompoundTag values = stack.getOrCreateTag().getCompound(root);
        if (level <= 0) values.remove(id.toString());
        else if (values.size() < 64 || values.contains(id.toString())) values.putInt(id.toString(), clamp(level, 1, maximum));
        stack.getOrCreateTag().put(root, values);
    }

    private static Map<ResourceLocation, Integer> intMap(ItemStack stack, String root, int maximum) {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        CompoundTag values = stack.getOrCreateTag().getCompound(root);
        for (String key : values.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null && result.size() < 64) result.put(id, clamp(values.getInt(key), 1, maximum));
        }
        return result;
    }

    private static boolean bool(ItemStack stack, String key, boolean fallback) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.contains(key) ? tag.getBoolean(key) : fallback;
    }
    private static boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException("Expected boolean");
    }
    private static int clamp(int value, int minimum, int maximum) { return Math.max(minimum, Math.min(maximum, value)); }
}
