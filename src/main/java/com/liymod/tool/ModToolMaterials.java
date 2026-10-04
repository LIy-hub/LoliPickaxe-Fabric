package com.liymod.tool;

import com.liymod.LiyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;

public final class ModToolMaterials {
    private static final TagKey<Block> INCORRECT_FOR_LOLI_TOOL = TagKey.create(
            Registries.BLOCK,
            new ResourceLocation(LiyMod.MOD_ID, "incorrect_for_loli_tool")
    );
    private static final TagKey<Item> LOLI_REPAIR_MATERIALS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(LiyMod.MOD_ID, "loli_repair_materials")
    );

    public static final net.minecraft.world.item.Tier LOLI = new net.minecraft.world.item.Tier() {
        @Override public int getUses() { return Integer.MAX_VALUE; }
        @Override public float getSpeed() { return Float.MAX_VALUE; }
        @Override public float getAttackDamageBonus() { return Float.POSITIVE_INFINITY; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return INCORRECT_FOR_LOLI_TOOL; }
        @Override public int getEnchantmentValue() { return 30; }
        @Override public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() {
            return net.minecraft.world.item.crafting.Ingredient.of(LOLI_REPAIR_MATERIALS);
        }
    };

    private ModToolMaterials() {
    }
}
