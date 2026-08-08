package com.liymod.recipe;

import com.liymod.LiyMod;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, LiyMod.MOD_ID);
    public static final RegistryObject<RecipeSerializer<?>> UPGRADE_SUPERPOSITION = SERIALIZERS.register("upgrade_superposition", () -> new SimpleCraftingRecipeSerializer<>(UpgradeSuperpositionRecipe::new));
    public static final RegistryObject<RecipeSerializer<?>> SMALL_LOLI_UPGRADE = SERIALIZERS.register("small_loli_upgrade", () -> new SimpleCraftingRecipeSerializer<>(SmallLoliUpgradeRecipe::new));
    public static final RegistryObject<RecipeSerializer<?>> LOLI_PICKAXE_UPGRADE = SERIALIZERS.register("loli_pickaxe_upgrade", () -> new SimpleCraftingRecipeSerializer<>(LoliPickaxeUpgradeRecipe::new));
    private ModRecipes() { }
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
}
