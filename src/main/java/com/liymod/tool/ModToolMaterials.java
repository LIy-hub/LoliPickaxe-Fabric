package com.liymod.tool;

import com.liymod.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public enum ModToolMaterials implements ToolMaterial {
    LOLI(
            Integer.MAX_VALUE,
            Float.MAX_VALUE,
            Float.POSITIVE_INFINITY,
            30,
            () -> Ingredient.ofItems(ModItems.LOLI)
    );

    private final int itemDurability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final Supplier<Ingredient> repairIngredient;

    ModToolMaterials(
            int itemDurability,
            float miningSpeed,
            float attackDamage,
            int enchantability,
            Supplier<Ingredient> repairIngredient
    ) {
        this.itemDurability = itemDurability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        this.repairIngredient = repairIngredient;
    }

    public int getDurability() {
        return this.itemDurability;
    }

    public float getMiningSpeedMultiplier() {
        return this.miningSpeed;
    }

    public float getAttackDamage() {
        return this.attackDamage;
    }

    public TagKey<Block> getInverseTag() {
        return TagKey.of(
                RegistryKeys.BLOCK,
                Identifier.of("liymod", "incorrect_for_loli_tool")
        );
    }

    public int getEnchantability() {
        return this.enchantability;
    }

    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }
}
