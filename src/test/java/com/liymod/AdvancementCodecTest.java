package com.liymod;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Decodes shipped advancements with Minecraft's real registry-aware codec. */
public final class AdvancementCodecTest {
    @Test
    void bundledAdvancementsDecodeWithCurrentMinecraftSchema() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Path dataRoot = Path.of("src/main/resources/data/liymod");
        Path recipeRoot = dataRoot.resolve("recipe");
        var recipes = new MappedRegistry<Recipe<?>>(Registries.RECIPE, Lifecycle.stable());
        var recipeReferences = recipes.createRegistrationLookup();
        // World loading resolves advancements against the datapack's recipe IDs.
        // Declare the same forward references without creating a world or server.
        try (var paths = Files.walk(recipeRoot)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                String name = recipeRoot.relativize(path).toString().replace('\\', '/');
                name = name.substring(0, name.length() - ".json".length());
                recipeReferences.getOrThrow(ResourceKey.create(
                        Registries.RECIPE, Identifier.fromNamespaceAndPath("liymod", name)));
            }
        }
        var lookups = HolderLookup.Provider.create(Stream.concat(
                BuiltInRegistries.REGISTRY.stream().map(
                        registry -> (HolderLookup.RegistryLookup<?>) registry),
                Stream.of(recipes)));
        var ops = RegistryOps.create(JsonOps.INSTANCE, lookups);

        try (var paths = Files.walk(dataRoot.resolve("advancement"))) {
            var advancements = paths.filter(file -> file.toString().endsWith(".json")).toList();
            assertFalse(advancements.isEmpty(), "Expected bundled advancements to validate");
            for (Path path : advancements) {
                try (var reader = Files.newBufferedReader(path)) {
                    var json = JsonParser.parseReader(reader);
                    assertDoesNotThrow(() -> Advancement.CODEC.parse(ops, json).getOrThrow(),
                            path.toString());
                }
            }
        }
    }
}
