package com.liymod.item;

import com.liymod.LiyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

/** Registry equivalent of the datapack enchantment on pre-1.21 versions. */
public final class ModLegacyEnchantments {
    private ModLegacyEnchantments() { }

    public static void register() {
        Registry.register(BuiltInRegistries.ENCHANTMENT,
                new ResourceLocation(LiyMod.MOD_ID, "loli_auto_furnace"),
                new Enchantment(Enchantment.Rarity.VERY_RARE,
                        net.minecraft.world.item.enchantment.EnchantmentCategory.DIGGER,
                        new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                    @Override public int getMinCost(int level) { return 15; }
                    @Override public int getMaxCost(int level) { return 61; }
                    @Override public boolean canEnchant(net.minecraft.world.item.ItemStack stack) {
                        return stack.getItem() instanceof LoliPickaxeItem || stack.getItem() instanceof SmallLoliPickaxeItem;
                    }
                    @Override
                    protected boolean checkCompatibility(Enchantment other) {
                        return super.checkCompatibility(other)
                                && other != net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH;
                    }
                });
    }
}
