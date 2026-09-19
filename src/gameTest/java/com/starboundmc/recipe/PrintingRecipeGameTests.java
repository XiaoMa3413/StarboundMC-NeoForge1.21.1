package com.starboundmc.recipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class PrintingRecipeGameTests {
    @GameTest(template = "shuttle_test_empty")
    public static void loadedRecipesChargeVoxelsAndResolveOutputCategories(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipes = level.getRecipeManager().getAllRecipesFor(VoxelPrintingRecipe.TYPE);
        helper.assertTrue(recipes.size() == 14, "All 14 printing recipes must decode, including costs above 64");
        for (var holder : recipes) {
            var recipe = holder.value();
            var output = recipe.getResultItem(level.registryAccess());
            boolean refinery = holder.id().getPath().equals("print_voxel_refinery");
            helper.assertTrue(refinery ? recipe.voxelMaterialCount() == 0 : recipe.voxelMaterialCount() > 0,
                    "Wrong wallet cost: " + holder.id());
            long categories = java.util.Arrays.stream(PrintingCategory.values()).filter(c -> c.matches(output)).count();
            helper.assertTrue(categories == 1, "Output must match exactly one category: " + holder.id());
            if (refinery) helper.assertTrue(PrintingCategory.MACHINES.matches(output), "Refinery is a machine");
            if (holder.id().getPath().equals("print_epp_mk3_upgrade_kit")) {
                helper.assertTrue(recipe.voxelMaterialCount() == 120, "Mk.III must reserve 120 voxels");
                helper.assertTrue(PrintingCategory.SURVIVAL.matches(output), "EPP upgrade kit must be survival equipment");
                helper.assertTrue(!PrintingCategory.MATERIALS.matches(output), "EPP upgrade kit must not match materials");
            }
        }
        helper.assertTrue(PrintingCategory.MATERIALS.matches(new ItemStack(Items.DIAMOND)),
                "Untagged data pack outputs must remain accessible in basic materials");
        helper.succeed();
    }
}
