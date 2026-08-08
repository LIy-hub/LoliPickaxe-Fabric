package com.liymod.item;

import com.liymod.registry.ModContent;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public enum LoliToolTier implements Tier {
    INSTANCE;

    @Override public int getUses() { return Integer.MAX_VALUE; }
    @Override public float getSpeed() { return Float.MAX_VALUE; }
    @Override public float getAttackDamageBonus() { return Float.POSITIVE_INFINITY; }
    @Override public int getLevel() { return Integer.MAX_VALUE; }
    @Override public int getEnchantmentValue() { return 30; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModContent.LOLI.get()); }
}
