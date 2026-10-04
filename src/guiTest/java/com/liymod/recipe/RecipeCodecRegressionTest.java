package com.liymod.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.Bootstrap;

/** Reproduces recipe synchronization using fresh datapack-decoded instances. */
public final class RecipeCodecRegressionTest {
    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        check(UpgradeSuperpositionRecipe.MAP_CODEC, UpgradeSuperpositionRecipe.STREAM_CODEC);
        check(SmallLoliUpgradeRecipe.MAP_CODEC, SmallLoliUpgradeRecipe.STREAM_CODEC);
        check(LoliPickaxeUpgradeRecipe.MAP_CODEC, LoliPickaxeUpgradeRecipe.STREAM_CODEC);
        System.out.println("RECIPE_CODECS_OK freshDatapackInstances=3 repeatedRoundTrips=6");
    }

    private static <T> void check(MapCodec<T> mapCodec, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        for (int iteration = 0; iteration < 2; iteration++) {
            T recipe = mapCodec.codec().parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                    RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            try {
                codec.encode(buffer, recipe);
                T decoded = codec.decode(buffer);
                if (!recipe.getClass().equals(decoded.getClass()) || buffer.isReadable()) {
                    throw new AssertionError("Recipe did not round trip: " + recipe.getClass());
                }
            } finally {
                buffer.release();
            }
        }
    }
}
