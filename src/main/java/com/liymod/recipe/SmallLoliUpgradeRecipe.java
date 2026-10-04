package com.liymod.recipe;

import com.liymod.item.SmallLoliPickaxeItem;
import com.liymod.item.UpgradeItem;
import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Applies one or more distinct upgrade types when each input is exactly one tier above the pickaxe. */
public final class SmallLoliUpgradeRecipe extends CustomRecipe {
    public static final MapCodec<SmallLoliUpgradeRecipe> MAP_CODEC = MapCodec.unit(SmallLoliUpgradeRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SmallLoliUpgradeRecipe> STREAM_CODEC =
            StreamCodec.unit(new SmallLoliUpgradeRecipe());

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack pickaxe = ItemStack.EMPTY;
        Map<UpgradeItem.Type, Integer> upgrades = new EnumMap<>(UpgradeItem.Type.class);

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SmallLoliPickaxeItem) {
                if (!pickaxe.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                pickaxe = stack;
                continue;
            }
            if (!(stack.getItem() instanceof UpgradeItem upgrade)
                    || !upgrade.type().appliesToSmallPickaxe()
                    || upgrades.putIfAbsent(upgrade.type(), upgrade.getTier(stack)) != null) {
                return ItemStack.EMPTY;
            }
        }

        if (pickaxe.isEmpty() || upgrades.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (Map.Entry<UpgradeItem.Type, Integer> entry : upgrades.entrySet()) {
            if (entry.getValue() != SmallLoliPickaxeItem.getUpgradeTier(pickaxe, entry.getKey()) + 1) {
                return ItemStack.EMPTY;
            }
        }

        ItemStack result = pickaxe.copyWithCount(1);
        upgrades.forEach((type, tier) -> SmallLoliPickaxeItem.setUpgradeTier(result, type, tier));
        return result;
    }

    @Override
    public RecipeSerializer<SmallLoliUpgradeRecipe> getSerializer() {
        return ModRecipes.SMALL_LOLI_UPGRADE_SERIALIZER;
    }
}
