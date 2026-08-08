package com.liymod.recipe;

import com.liymod.item.UpgradeItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class UpgradeSuperpositionRecipe extends CustomRecipe {
    public UpgradeSuperpositionRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }

    @Override public boolean matches(CraftingContainer input, Level level) { return !assemble(input, level.registryAccess()).isEmpty(); }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess access) {
        UpgradeItem upgrade = null;
        int tier = -1, count = 0;
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof UpgradeItem candidate)) return ItemStack.EMPTY;
            int candidateTier = candidate.getTier(stack);
            if (upgrade == null) { upgrade = candidate; tier = candidateTier; }
            else if (upgrade != candidate || tier != candidateTier) return ItemStack.EMPTY;
            count++;
        }
        if (upgrade == null) return ItemStack.EMPTY;
        if (count == 9 && tier < upgrade.maxTier()) return upgrade.createStack(tier + 1);
        if (count == 1 && tier > 0) { ItemStack result = upgrade.createStack(tier - 1); result.setCount(9); return result; }
        return ItemStack.EMPTY;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.UPGRADE_SUPERPOSITION.get(); }
}
