package com.liymod.recipe;

import com.liymod.item.ModItems;
import com.liymod.item.SmallLoliPickaxeItem;
import com.liymod.item.UpgradeItem;
import com.mojang.serialization.MapCodec;
import com.liymod.compat.DataComponents;
import com.liymod.compat.RegistryFriendlyByteBuf;
import com.liymod.compat.StreamCodec;
import net.minecraft.world.item.ItemStack;
import com.liymod.compat.CustomData;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Converts a fully upgraded Small Loli Pickaxe plus the final Entity Soul into the existing final pickaxe. */
public final class LoliPickaxeUpgradeRecipe extends CustomRecipe {
    public LoliPickaxeUpgradeRecipe() { this(new net.minecraft.resources.ResourceLocation("liymod", "loli_pickaxe_upgrade")); }
    public LoliPickaxeUpgradeRecipe(net.minecraft.resources.ResourceLocation id) { super(id, net.minecraft.world.item.crafting.CraftingBookCategory.MISC); }
    public static final MapCodec<LoliPickaxeUpgradeRecipe> MAP_CODEC = MapCodec.unit(LoliPickaxeUpgradeRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliPickaxeUpgradeRecipe> STREAM_CODEC =
            StreamCodec.of((buffer, recipe) -> { }, buffer -> new LoliPickaxeUpgradeRecipe());

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        return !assemble(input, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer input, net.minecraft.core.RegistryAccess registries) {
        ItemStack smallPickaxe = ItemStack.EMPTY;
        ItemStack entitySoul = ItemStack.EMPTY;

        for (ItemStack stack : input.getItems()) {
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
        CustomData inherited = com.liymod.compat.LegacyComponents.getOrDefault(smallPickaxe, DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!inherited.isEmpty()) {
            com.liymod.compat.LegacyComponents.set(result, DataComponents.CUSTOM_DATA, inherited);
        }
        return result;
    }

    @Override
    public RecipeSerializer<LoliPickaxeUpgradeRecipe> getSerializer() {
        return ModRecipes.LOLI_PICKAXE_UPGRADE_SERIALIZER;
    }
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

}
