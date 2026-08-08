package com.liymod.config;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Writes validated integer enchantment levels without the vanilla signed-short truncation. */
public final class LogicalEnchantments {
    public static final int MAXIMUM_LEVEL = 32768;

    private LogicalEnchantments() { }

    public static void setLevel(ItemStack stack, Enchantment enchantment, int requestedLevel) {
        int level = Mth.clamp(requestedLevel, 0, MAXIMUM_LEVEL);
        Map<Enchantment, Integer> desired = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (level == 0) desired.remove(enchantment); else desired.put(enchantment, level);

        Map<Enchantment, Integer> encoded = new LinkedHashMap<>();
        desired.forEach((entry, value) -> encoded.put(entry, Math.min(Short.MAX_VALUE, value)));
        EnchantmentHelper.setEnchantments(encoded, stack);

        ListTag tags = stack.getEnchantmentTags();
        for (int index = 0; index < tags.size(); index++) {
            CompoundTag tag = tags.getCompound(index);
            ResourceLocation id = EnchantmentHelper.getEnchantmentId(tag);
            Enchantment current = id == null ? null : net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(id);
            Integer exact = current == null ? null : desired.get(current);
            if (exact != null) tag.putInt("lvl", Mth.clamp(exact, 0, MAXIMUM_LEVEL));
        }
    }
}
