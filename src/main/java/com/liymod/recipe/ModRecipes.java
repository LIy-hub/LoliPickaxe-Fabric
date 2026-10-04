package com.liymod.recipe;

import com.liymod.LiyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipes {
    public static final RecipeSerializer<UpgradeSuperpositionRecipe> UPGRADE_SUPERPOSITION_SERIALIZER = register(
            "upgrade_superposition",
            new RecipeSerializer<>(UpgradeSuperpositionRecipe.MAP_CODEC, UpgradeSuperpositionRecipe.STREAM_CODEC)
    );
    public static final RecipeSerializer<SmallLoliUpgradeRecipe> SMALL_LOLI_UPGRADE_SERIALIZER = register(
            "small_loli_upgrade",
            new RecipeSerializer<>(SmallLoliUpgradeRecipe.MAP_CODEC, SmallLoliUpgradeRecipe.STREAM_CODEC)
    );
    public static final RecipeSerializer<LoliPickaxeUpgradeRecipe> LOLI_PICKAXE_UPGRADE_SERIALIZER = register(
            "loli_pickaxe_upgrade",
            new RecipeSerializer<>(LoliPickaxeUpgradeRecipe.MAP_CODEC, LoliPickaxeUpgradeRecipe.STREAM_CODEC)
    );

    private ModRecipes() {
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> register(
            String name,
            RecipeSerializer<T> serializer
    ) {
        return Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, name),
                serializer
        );
    }

    public static void registerRecipes() {
        LiyMod.LOGGER.info("Registering original dynamic recipes for {}", LiyMod.MOD_ID);
    }
}
