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
                new Enchantment(Enchantment.definition(
                        TagKey.create(Registries.ITEM, new ResourceLocation(LiyMod.MOD_ID, "enchantable/loli_auto_furnace")),
                        1, 1, Enchantment.constantCost(15), Enchantment.constantCost(61), 8,
                        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)) {
                    @Override
                    protected boolean checkCompatibility(Enchantment other) {
                        return super.checkCompatibility(other)
                                && other != net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH;
                    }
                });
    }
}
