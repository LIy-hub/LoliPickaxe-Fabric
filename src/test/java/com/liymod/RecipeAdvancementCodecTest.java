package com.liymod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.liymod.block.ModBlocks;
import com.liymod.item.ModItems;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.*;

/** Runs shipped recipe advancements through the actual 26.3 recipe/advancement codecs. */
final class RecipeAdvancementCodecTest {
    private static final Map<String, JsonObject> ADVANCEMENTS = new LinkedHashMap<>();
    private static final Map<ResourceKey<Recipe<?>>, Recipe<?>> RECIPES = new LinkedHashMap<>();
    private static RegistryOps<JsonElement> ops;

    @BeforeAll static void loadProductionRecipes() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ModBlocks.registerModBlocks();
        ModItems.registerModItems();
        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var recipeOps = RegistryOps.create(JsonOps.INSTANCE, builtIns);
        var root = Path.of("src/main/resources/data/liymod/advancement");
        try (var paths = Files.walk(root)) {
            for (var path : paths.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                var relative = root.relativize(path).toString().replace('\\', '/');
                // Read processed classpath resources, so these are the bytes packaged in the runtime JAR.
                var advancement = readJson("/data/liymod/advancement/" + relative);
                ADVANCEMENTS.put(relative, advancement);
                var recipeId = Identifier.parse(advancement.getAsJsonObject("rewards").getAsJsonArray("recipes").get(0).getAsString());
                var recipe = Recipe.DIRECT_CODEC.parse(recipeOps,
                        readJson("/data/liymod/recipe/" + recipeId.getPath() + ".json")).getOrThrow();
                RECIPES.put(ResourceKey.create(Registries.RECIPE, recipeId), recipe);
            }
        }
        assertEquals(20, ADVANCEMENTS.size(), "Cover all twenty shipped recipe advancements");
        assertEquals(20, RECIPES.size());
        var registries = new RegistrySetBuilder().add(Registries.RECIPE,
                context -> RECIPES.forEach(context::register)).build(builtIns);
        ops = RegistryOps.create(JsonOps.INSTANCE, registries);
    }

    @TestFactory Stream<DynamicTest> allRecipeAdvancementsDecodeAndRetainUnlockSemantics() {
        return ADVANCEMENTS.entrySet().stream().map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
            var json = entry.getValue();
            var advancement = Advancement.CODEC.parse(ops, json).getOrThrow();
            assertEquals("minecraft:recipes/root", advancement.parent().orElseThrow().toString());
            assertEquals(Set.of("has_primary_ingredient", "has_the_recipe"), advancement.criteria().keySet());
            assertEquals(List.of(List.of("has_primary_ingredient", "has_the_recipe")), advancement.requirements().requirements());
            assertTrue(advancement.requirements().test("has_primary_ingredient"::equals));
            assertTrue(advancement.requirements().test("has_the_recipe"::equals));
            assertFalse(advancement.requirements().test(ignored -> false));

            var reward = advancement.rewards().recipes();
            assertEquals(1, reward.size());
            assertEquals(0, advancement.rewards().experience());
            assertTrue(advancement.display().isEmpty());
            var conditions = json.getAsJsonObject("criteria").getAsJsonObject("has_the_recipe").getAsJsonObject("conditions");
            assertEquals(Set.of("recipes"), conditions.keySet(), "26.3 requires recipes, retaining a single exact ID");
            assertEquals(reward.getFirst().identifier().toString(), conditions.get("recipes").getAsString());

            var trigger = assertInstanceOf(RecipeUnlockedTrigger.TriggerInstance.class,
                    advancement.criteria().get("has_the_recipe").triggerInstance());
            assertEquals(1, trigger.recipes().size());
            for (var candidate : RECIPES.entrySet()) {
                assertEquals(candidate.getKey().equals(reward.getFirst()),
                        trigger.matches(new RecipeHolder<>(candidate.getKey(), candidate.getValue())),
                        "The unlock trigger must match only its own rewarded recipe");
            }
        }));
    }

    @Test void officialCodecRejectsThePreviousSingularRecipeField() {
        for (var json : ADVANCEMENTS.values()) {
            var previous = json.deepCopy();
            var conditions = previous.getAsJsonObject("criteria").getAsJsonObject("has_the_recipe").getAsJsonObject("conditions");
            conditions.add("recipe", conditions.remove("recipes"));
            var error = Advancement.CODEC.parse(ops, previous).error().orElseThrow();
            assertTrue(error.message().contains("No key recipes"), error.message());
        }
    }

    private static JsonObject readJson(String path) throws Exception {
        try (var input = RecipeAdvancementCodecTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
