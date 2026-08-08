package com.liymod.recipe;

import com.liymod.item.SmallLoliPickaxeItem;
import com.liymod.item.UpgradeItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class SmallLoliUpgradeRecipe extends CustomRecipe {
    public SmallLoliUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    @Override public boolean matches(CraftingContainer input, Level level) { return !assemble(input, level.registryAccess()).isEmpty(); }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess access) {
        ItemStack pickaxe = ItemStack.EMPTY;
        Map<UpgradeItem.Type, Integer> upgrades = new EnumMap<>(UpgradeItem.Type.class);
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof SmallLoliPickaxeItem) {
                if (!pickaxe.isEmpty()) return ItemStack.EMPTY;
                pickaxe = stack;
            } else if (stack.getItem() instanceof UpgradeItem upgrade
                    && upgrade.type().appliesToSmallPickaxe()
                    && upgrades.putIfAbsent(upgrade.type(), upgrade.getTier(stack)) == null) {
                // accepted
            } else return ItemStack.EMPTY;
        }
        if (pickaxe.isEmpty() || upgrades.isEmpty()) return ItemStack.EMPTY;
        for (Map.Entry<UpgradeItem.Type, Integer> entry : upgrades.entrySet()) {
            if (entry.getValue() != SmallLoliPickaxeItem.getUpgradeTier(pickaxe, entry.getKey()) + 1) return ItemStack.EMPTY;
        }
        ItemStack result = pickaxe.copy(); result.setCount(1);
        upgrades.forEach((type, tier) -> SmallLoliPickaxeItem.setUpgradeTier(result, type, tier));
        return result;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.SMALL_LOLI_UPGRADE.get(); }
}
