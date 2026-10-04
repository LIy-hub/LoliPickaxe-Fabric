package com.liymod.recipe;

import com.liymod.LiyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipes {
    public static final RecipeSerializer<UpgradeSuperpositionRecipe> UPGRADE_SUPERPOSITION_SERIALIZER = register(
            "upgrade_superposition",
            serializer(UpgradeSuperpositionRecipe.MAP_CODEC, UpgradeSuperpositionRecipe.STREAM_CODEC)
    );
    public static final RecipeSerializer<SmallLoliUpgradeRecipe> SMALL_LOLI_UPGRADE_SERIALIZER = register(
            "small_loli_upgrade",
            serializer(SmallLoliUpgradeRecipe.MAP_CODEC, SmallLoliUpgradeRecipe.STREAM_CODEC)
    );
    public static final RecipeSerializer<LoliPickaxeUpgradeRecipe> LOLI_PICKAXE_UPGRADE_SERIALIZER = register(
            "loli_pickaxe_upgrade",
            serializer(LoliPickaxeUpgradeRecipe.MAP_CODEC, LoliPickaxeUpgradeRecipe.STREAM_CODEC)
    );

    private ModRecipes() {
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> serializer(
            com.mojang.serialization.MapCodec<T> codec,
            com.liymod.compat.StreamCodec<com.liymod.compat.RegistryFriendlyByteBuf, T> stream) {
        return new RecipeSerializer<T>() {
            @Override public com.mojang.serialization.Codec<T> codec() { return codec.codec(); }
            @Override public T fromNetwork(net.minecraft.network.FriendlyByteBuf buffer) {
                return stream.decode(new com.liymod.compat.RegistryFriendlyByteBuf(buffer,
                        net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
            }
            @Override public void toNetwork(net.minecraft.network.FriendlyByteBuf buffer,T recipe) {
                stream.encode(new com.liymod.compat.RegistryFriendlyByteBuf(buffer,
                        net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)),recipe);
            }
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
