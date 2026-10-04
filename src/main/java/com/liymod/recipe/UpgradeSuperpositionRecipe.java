package com.liymod.recipe;

import com.liymod.item.UpgradeItem;
import com.mojang.serialization.MapCodec;
import com.liymod.compat.RegistryFriendlyByteBuf;
import com.liymod.compat.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Nine equal upgrade tiers combine upward; one non-base tier splits into nine of the preceding tier. */
public final class UpgradeSuperpositionRecipe extends CustomRecipe {
    public UpgradeSuperpositionRecipe() { super(net.minecraft.world.item.crafting.CraftingBookCategory.MISC); }
    public static final MapCodec<UpgradeSuperpositionRecipe> MAP_CODEC =
            MapCodec.unit(UpgradeSuperpositionRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeSuperpositionRecipe> STREAM_CODEC =
            StreamCodec.of((buffer, recipe) -> { }, buffer -> new UpgradeSuperpositionRecipe());

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        return !assemble(input, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer input, net.minecraft.core.RegistryAccess registries) {
        UpgradeItem upgrade = null;
        int tier = -1;
        int count = 0;

        for (ItemStack stack : input.getItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof UpgradeItem candidate)) {
                return ItemStack.EMPTY;
            }
            int candidateTier = candidate.getTier(stack);
            if (upgrade == null) {
                upgrade = candidate;
                tier = candidateTier;
            } else if (candidate != upgrade || candidateTier != tier) {
                return ItemStack.EMPTY;
            }
            count++;
        }

        if (upgrade == null) {
            return ItemStack.EMPTY;
        }
        if (count == 9 && tier < upgrade.maxTier()) {
            return upgrade.createStack(tier + 1);
        }
        if (count == 1 && tier > 0) {
            ItemStack result = upgrade.createStack(tier - 1);
            result.setCount(9);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<UpgradeSuperpositionRecipe> getSerializer() {
        return ModRecipes.UPGRADE_SUPERPOSITION_SERIALIZER;
    }
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

}
