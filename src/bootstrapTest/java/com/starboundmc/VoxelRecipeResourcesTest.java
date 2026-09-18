package com.starboundmc.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M2/M3: data-driven voxel machine recipes land with agreed values. */
final class VoxelRecipeResourcesTest {
    private static final Path RECIPES = Path.of("src/main/resources/data/starboundmc/recipe");

    @Test
    void shippedPrintingRecipesHaveEffectiveVoxelCostsAndCategories() throws IOException {
        var categorized = new java.util.HashSet<String>();
        for (String category : java.util.List.of("equipment", "components", "machines", "building")) {
            var tag = JsonParser.parseString(Files.readString(RECIPES.resolve(
                    "../tags/item/printing/" + category + ".json"))).getAsJsonObject();
            for (var item : tag.getAsJsonArray("values")) {
                assertTrue(categorized.add(item.getAsString()), "Duplicate printing category: " + item);
            }
        }
        try (var paths = Files.list(RECIPES)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().startsWith("print_")).toList()) {
                var recipe = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                assertFalse(recipe.has("voxel_cost"), path.toString());
                var materials = recipe.getAsJsonArray("materials");
                assertTrue(materials.size() <= 6, path.toString());
                long voxels = materials.asList().stream().map(e -> e.getAsJsonObject())
                        .filter(m -> m.getAsJsonObject("ingredient").has("item")
                                && m.getAsJsonObject("ingredient").get("item").getAsString().equals("starboundmc:voxel"))
                        .mapToLong(m -> m.get("count").getAsLong()).sum();
                assertTrue(voxels > 0, "Missing effective voxel cost: " + path);
                assertTrue(categorized.remove(recipe.getAsJsonObject("result").get("id").getAsString()),
                        "Missing category: " + path);
            }
        }
        assertTrue(categorized.isEmpty(), "Category lists unprintable outputs: " + categorized);
    }

    @Test
    void modulePrintingRecipeMatchesAgreedValues() throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(
                RECIPES.resolve("print_matter_manipulator_module.json"))).getAsJsonObject();
        assertEquals("starboundmc:voxel_printing", root.get("type").getAsString());
        assertFalse(root.has("voxel_cost"));
        assertEquals(5, root.get("print_seconds").getAsInt());

        JsonObject result = root.getAsJsonObject("result");
        assertEquals("starboundmc:matter_manipulator_module", result.get("id").getAsString());
        assertEquals(2, result.get("count").getAsInt());

        var materials = root.getAsJsonArray("materials").asList();
        assertEquals(3, materials.size());
        JsonObject voxel = materials.get(0).getAsJsonObject();
        assertEquals("starboundmc:voxel", voxel.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals(100, voxel.get("count").getAsInt());
        JsonObject lapis = materials.get(1).getAsJsonObject();
        assertEquals("minecraft:lapis_lazuli", lapis.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals(1, lapis.get("count").getAsInt());
        JsonObject iron = materials.get(2).getAsJsonObject();
        assertEquals("minecraft:iron_ingot", iron.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals(4, iron.get("count").getAsInt());
    }

    @Test
    void decompositionValuesFollowTheDensityTable() throws IOException {
        assertDecomposition("decomposition_coal", "minecraft:coal", 5);
        assertDecomposition("decomposition_charcoal", "minecraft:charcoal", 5);
        assertDecomposition("decomposition_diamond", "minecraft:diamond", 5);
        assertDecomposition("decomposition_redstone", "minecraft:redstone", 3);
        assertDecomposition("decomposition_lapis", "minecraft:lapis_lazuli", 4);
        assertDecomposition("decomposition_emerald", "minecraft:emerald", 5);
        assertDecomposition("decomposition_raw_iron", "minecraft:raw_iron", 15);
        assertDecomposition("decomposition_iron_ingot", "minecraft:iron_ingot", 15);
        assertDecomposition("decomposition_raw_copper", "minecraft:raw_copper", 17);
        assertDecomposition("decomposition_copper_ingot", "minecraft:copper_ingot", 17);
        assertDecomposition("decomposition_raw_gold", "minecraft:raw_gold", 40);
        assertDecomposition("decomposition_gold_ingot", "minecraft:gold_ingot", 40);
        assertDecomposition("decomposition_ancient_debris", "minecraft:ancient_debris", 30);
        assertDecomposition("decomposition_netherite_ingot", "minecraft:netherite_ingot", 50);
    }

    @Test
    void moduleWorkbenchRecipeIsRemoved() {
        assertTrue(java.nio.file.Files.notExists(RECIPES.resolve("matter_manipulator_module.json")),
                "the temporary workbench recipe for modules must stay removed");
    }

    private static void assertDecomposition(String file, String item, int voxels) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(RECIPES.resolve(file + ".json")))
                .getAsJsonObject();
        assertEquals("starboundmc:voxel_decomposition", root.get("type").getAsString());
        assertEquals(item, root.getAsJsonObject("input").get("item").getAsString());
        assertEquals(voxels, root.get("voxels").getAsInt());
    }
}
