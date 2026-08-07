package com.liymod.item;

import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Bounded server-side enchantment editor that preserves the final pickaxe baseline. */
public final class LoliFinalEnchantments {
    public static final int MAX_ENTRIES = 64;

    private LoliFinalEnchantments() {
    }

    public static boolean update(ServerLevel level, ItemStack stack, String encodedId, int requestedLevel) {
        if (!(stack.getItem() instanceof LoliPickaxeItem)
                || encodedId == null
                || encodedId.length() > 128) {
            return false;
        }
        Identifier id = Identifier.tryParse(encodedId);
        if (id == null) {
            return false;
        }
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder.Reference<Enchantment> enchantment = registry.get(id).orElse(null);
        if (enchantment == null) {
            return false;
        }

        int maximum = LoliServerConfig.getInt(LoliConfigOption.ENCHANTMENT_LEVEL_LIMIT);
        int targetLevel = Math.clamp(requestedLevel, 0, maximum);
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (targetLevel > 0 && current.getLevel(enchantment) == 0 && current.size() >= MAX_ENTRIES) {
            return false;
        }
        Holder<Enchantment> fortune = registry.getOrThrow(Enchantments.FORTUNE);
        EnchantmentHelper.updateEnchantments(stack, mutable -> {
            mutable.set(enchantment, targetLevel);
            mutable.set(fortune, Math.max(LoliPickaxeItem.FORTUNE_LEVEL, mutable.getLevel(fortune)));
        });
        return true;
    }
}
