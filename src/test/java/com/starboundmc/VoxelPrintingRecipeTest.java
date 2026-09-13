package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxelPrintingRecipeTest {
    @Test
    void recipeContractTreatsVoxelAsAnOrdinaryMaterialEntry() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/starboundmc/recipe/VoxelPrintingRecipe.java"));

        assertTrue(source.contains("public static final int MAX_INGREDIENTS = 6;"));
        assertTrue(source.contains("public static final int MAX_MATERIAL_COUNT = Integer.MAX_VALUE;"));
        assertTrue(source.contains("Codec.intRange(1, MAX_MATERIAL_COUNT)"));
        assertTrue(source.contains("public boolean isVoxel()"));
        assertTrue(source.contains("MaterialEntry.CODEC.listOf().fieldOf(\"materials\")"));
        assertTrue(source.contains("ItemStack[] matchingItems = ingredient.getItems()"));
    }

    @Test
    void existingRecipeStoresVoxelInsideTheMaterialList() throws IOException {
        String recipe = Files.readString(Path.of(
                "src/main/resources/data/starboundmc/recipe/print_matter_manipulator_module.json"));

        assertTrue(recipe.contains("\"item\": \"starboundmc:voxel\""));
        assertTrue(recipe.contains("\"count\": 100"));
        assertFalse(recipe.contains("\"voxel_cost\""));
    }

    @Test
    void printerSpendsWalletOnlyForTheDerivedVoxelMaterialTotal() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/starboundmc/block/entity/VoxelPrintingStationBlockEntity.java"));

        assertTrue(source.contains("long voxelMaterialCount = printing.voxelMaterialCount();"));
        assertTrue(source.contains("long totalCost = voxelMaterialCount * quantity"));
        assertTrue(source.contains("totalCost > 0 && !VoxelWalletService.trySpend(operator, (int) totalCost)"));
    }
}
