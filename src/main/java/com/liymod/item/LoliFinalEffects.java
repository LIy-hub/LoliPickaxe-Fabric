package com.liymod.item;

import java.util.LinkedHashMap;
import java.util.Map;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliServerConfig;
import com.liymod.nbt.LoliCustomData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
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
        CompoundTag custom = LoliCustomData.view(stack);
        CompoundTag root = custom.getCompound(ROOT_KEY);
        if (root.contains(EFFECTS_KEY)) {
            return;
        }
        Map<ResourceLocation, Integer> defaults = new LinkedHashMap<>();
        defaults.put(ResourceLocation.withDefaultNamespace("night_vision"), 1);
        defaults.put(ResourceLocation.withDefaultNamespace("water_breathing"), 1);
        set(stack, defaults);
    }

    public static Map<ResourceLocation, Integer> get(ItemStack stack) {
        Map<ResourceLocation, Integer> values = new LinkedHashMap<>();
        ListTag list = LoliCustomData.view(stack).getCompound(ROOT_KEY).getList(EFFECTS_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND);
        if (list.sizeInBytes() > 64 * 1024) {
            return Map.of();
        }
        int maximum = 32;
        for (int index = 0; index < list.size() && values.size() < MAX_ENTRIES; index++) {
            CompoundTag entry = list.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(com.liymod.compat.LegacyNbt.getStringOr(entry, ID_KEY, ""));
            int level = com.liymod.compat.LegacyNbt.getIntOr(entry, LEVEL_KEY, 0);
            if (id != null && level > 0) {
                values.putIfAbsent(id, Math.clamp(level, 1, maximum));
            }
        }
        return Map.copyOf(values);
    }

    public static void set(ItemStack stack, Map<ResourceLocation, Integer> effects) {
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
            CompoundTag loli = root.getCompound(ROOT_KEY);
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
        ResourceLocation id = ResourceLocation.tryParse(encodedId);
        if (id == null) {
            return false;
        }
        Registry<MobEffect> registry = level.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
        if (registry.getHolder(id).isEmpty()) {
            return false;
        }
        int maximum = LoliServerConfig.getInt(LoliConfigOption.EFFECT_LEVEL_LIMIT);
        int levelValue = Math.clamp(requestedLevel, 0, maximum);
        Map<ResourceLocation, Integer> mutable = new LinkedHashMap<>(get(stack));
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
