package com.liymod.recipe;

import com.liymod.item.SmallLoliPickaxeItem;
import com.liymod.item.UpgradeItem;
import com.liymod.registry.ModContent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class LoliPickaxeUpgradeRecipe extends CustomRecipe {
    public LoliPickaxeUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    @Override public boolean matches(CraftingContainer input, Level level) { return !assemble(input, level.registryAccess()).isEmpty(); }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess access) {
        ItemStack small = ItemStack.EMPTY, soul = ItemStack.EMPTY;
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof SmallLoliPickaxeItem) { if (!small.isEmpty()) return ItemStack.EMPTY; small = stack; }
            else if (stack.getItem() instanceof UpgradeItem upgrade && upgrade.type() == UpgradeItem.Type.ENTITY_SOUL) { if (!soul.isEmpty()) return ItemStack.EMPTY; soul = stack; }
            else return ItemStack.EMPTY;
        }
        if (!SmallLoliPickaxeItem.isFullyUpgraded(small) || soul.isEmpty()
                || ((UpgradeItem) soul.getItem()).getTier(soul) != UpgradeItem.Type.ENTITY_SOUL.maxTier()) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(ModContent.LOLI_PICKAXE.get());
        if (small.hasTag()) result.setTag(small.getTag().copy());
        return result;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.LOLI_PICKAXE_UPGRADE.get(); }
}
