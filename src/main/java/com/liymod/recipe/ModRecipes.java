package com.liymod.recipe;

import com.liymod.LiyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipes {
    public static final RecipeSerializer<UpgradeSuperpositionRecipe> UPGRADE_SUPERPOSITION_SERIALIZER = register(
            "upgrade_superposition",
            serializer(UpgradeSuperpositionRecipe::new)
    );
    public static final RecipeSerializer<SmallLoliUpgradeRecipe> SMALL_LOLI_UPGRADE_SERIALIZER = register(
            "small_loli_upgrade",
            serializer(SmallLoliUpgradeRecipe::new)
    );
    public static final RecipeSerializer<LoliPickaxeUpgradeRecipe> LOLI_PICKAXE_UPGRADE_SERIALIZER = register(
            "loli_pickaxe_upgrade",
            serializer(LoliPickaxeUpgradeRecipe::new)
    );

    private ModRecipes() {
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> serializer(
            java.util.function.Function<ResourceLocation,T> factory) {
        return new RecipeSerializer<T>() {
            @Override public T fromJson(ResourceLocation id,com.google.gson.JsonObject json) { return factory.apply(id); }
            @Override public T fromNetwork(ResourceLocation id,net.minecraft.network.FriendlyByteBuf buffer) { return factory.apply(id); }
            @Override public void toNetwork(net.minecraft.network.FriendlyByteBuf buffer,T recipe) { }
        };
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> register(
            String name,
            RecipeSerializer<T> serializer
    ) {
        return Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                new ResourceLocation(LiyMod.MOD_ID, name),
                serializer
        );
    }

    public static void registerRecipes() {
        LiyMod.LOGGER.info("Registering original dynamic recipes for {}", LiyMod.MOD_ID);
    }
}
