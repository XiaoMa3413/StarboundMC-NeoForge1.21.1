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
        helper.assertTrue(recipes.size() == 13, "All 13 printing recipes must decode, including costs above 64");
        for (var holder : recipes) {
            var recipe = holder.value();
            var output = recipe.getResultItem(level.registryAccess());
            helper.assertTrue(recipe.voxelMaterialCount() > 0, "Missing wallet cost: " + holder.id());
            helper.assertTrue(PrintingCategory.ALL.matches(output), "All category must include " + holder.id());
            helper.assertTrue(!PrintingCategory.OTHER.matches(output), "Missing output category: " + holder.id());
            if (holder.id().getPath().equals("print_epp_mk3_upgrade_kit")) {
                helper.assertTrue(recipe.voxelMaterialCount() == 120, "Mk.III must reserve 120 voxels");
                helper.assertTrue(PrintingCategory.COMPONENTS.matches(output), "Upgrade kit must be a component");
                helper.assertTrue(!PrintingCategory.EQUIPMENT.matches(output), "Upgrade kit must not match equipment");
            }
        }
        helper.assertTrue(PrintingCategory.OTHER.matches(new ItemStack(Items.DIAMOND)),
                "Untagged data pack outputs must remain accessible in Other");
        helper.succeed();
    }
}
