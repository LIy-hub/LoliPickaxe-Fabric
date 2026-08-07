package com.liymod.recipe;

import com.liymod.item.ModItems;
import com.liymod.item.SmallLoliPickaxeItem;
import com.liymod.item.UpgradeItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Converts a fully upgraded Small Loli Pickaxe plus the final Entity Soul into the existing final pickaxe. */
public final class LoliPickaxeUpgradeRecipe extends CustomRecipe {
    public static final MapCodec<LoliPickaxeUpgradeRecipe> MAP_CODEC = MapCodec.unit(LoliPickaxeUpgradeRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliPickaxeUpgradeRecipe> STREAM_CODEC =
            StreamCodec.unit(new LoliPickaxeUpgradeRecipe());

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack smallPickaxe = ItemStack.EMPTY;
        ItemStack entitySoul = ItemStack.EMPTY;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SmallLoliPickaxeItem) {
                if (!smallPickaxe.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                smallPickaxe = stack;
            } else if (stack.getItem() instanceof UpgradeItem upgrade
                    && upgrade.type() == UpgradeItem.Type.ENTITY_SOUL) {
                if (!entitySoul.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                entitySoul = stack;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (!SmallLoliPickaxeItem.isFullyUpgraded(smallPickaxe)
                || entitySoul.isEmpty()
                || ((UpgradeItem) entitySoul.getItem()).getTier(entitySoul)
                        != UpgradeItem.Type.ENTITY_SOUL.maxTier()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = ModItems.LOLI_PICKAXE.getDefaultInstance().copy();
        CustomData inherited = smallPickaxe.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!inherited.isEmpty()) {
            result.set(DataComponents.CUSTOM_DATA, inherited);
        }
        return result;
    }

    @Override
    public RecipeSerializer<LoliPickaxeUpgradeRecipe> getSerializer() {
        return ModRecipes.LOLI_PICKAXE_UPGRADE_SERIALIZER;
    }
}
