package com.liymod.item;

import java.util.LinkedHashMap;
import java.util.Map;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliServerConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.server.level.ServerLevel;

/** Bounded effect settings; registry validation and application are server-side. */
public final class LoliFinalEffects {
    public static final int MAX_ENTRIES = 64;
    private static final String ROOT_KEY = "LoliFinal";
    private static final String EFFECTS_KEY = "Effects";
    private static final String ID_KEY = "Id";
    private static final String LEVEL_KEY = "Level";

    private LoliFinalEffects() {
    }

    public static void ensureDefaults(ItemStack stack) {
        CompoundTag custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag root = custom.getCompoundOrEmpty(ROOT_KEY);
        if (root.contains(EFFECTS_KEY)) {
            return;
        }
        Map<Identifier, Integer> defaults = new LinkedHashMap<>();
        defaults.put(Identifier.withDefaultNamespace("night_vision"), 1);
        defaults.put(Identifier.withDefaultNamespace("water_breathing"), 1);
        set(stack, defaults);
    }

    public static Map<Identifier, Integer> get(ItemStack stack) {
        Map<Identifier, Integer> values = new LinkedHashMap<>();
        CompoundTag custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (custom.sizeInBytes() > 64 * 1024) {
            return Map.of();
        }
        ListTag list = custom.getCompoundOrEmpty(ROOT_KEY).getListOrEmpty(EFFECTS_KEY);
        int maximum = 32;
        for (int index = 0; index < list.size() && values.size() < MAX_ENTRIES; index++) {
            CompoundTag entry = list.getCompoundOrEmpty(index);
            Identifier id = Identifier.tryParse(entry.getStringOr(ID_KEY, ""));
            int level = entry.getIntOr(LEVEL_KEY, 0);
            if (id != null && level > 0) {
                values.putIfAbsent(id, Math.clamp(level, 1, maximum));
            }
        }
        return Map.copyOf(values);
    }

    public static void set(ItemStack stack, Map<Identifier, Integer> effects) {
        ListTag encoded = new ListTag();
        effects.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .limit(MAX_ENTRIES)
                .forEach(entry -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString(ID_KEY, entry.getKey().toString());
                    tag.putInt(LEVEL_KEY, entry.getValue());
                    encoded.add(tag);
                });
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag loli = root.getCompoundOrEmpty(ROOT_KEY);
            loli.put(EFFECTS_KEY, encoded);
            root.put(ROOT_KEY, loli);
        });
    }

    public static boolean update(
            ServerLevel level,
            ItemStack stack,
            String encodedId,
            int requestedLevel
    ) {
        if (!(stack.getItem() instanceof LoliPickaxeItem)
                || encodedId == null
                || encodedId.length() > 128) {
            return false;
        }
        Identifier id = Identifier.tryParse(encodedId);
        if (id == null) {
            return false;
        }
        Registry<MobEffect> registry = level.registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        if (registry.get(id).isEmpty()) {
            return false;
        }
        int maximum = LoliServerConfig.getInt(LoliConfigOption.EFFECT_LEVEL_LIMIT);
        int levelValue = Math.clamp(requestedLevel, 0, maximum);
        Map<Identifier, Integer> mutable = new LinkedHashMap<>(get(stack));
        if (levelValue <= 0) {
            mutable.remove(id);
        } else if (mutable.containsKey(id) || mutable.size() < MAX_ENTRIES) {
            mutable.put(id, levelValue);
        } else {
            return false;
        }
        set(stack, mutable);
        return true;
    }
}
