package com.starboundmc.recipe;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom recipe types for the voxel machines. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, StarboundMC.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, StarboundMC.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<VoxelDecompositionRecipe>> VOXEL_DECOMPOSITION =
            RECIPE_TYPES.register("voxel_decomposition", () -> VoxelDecompositionRecipe.TYPE);
    public static final DeferredHolder<RecipeType<?>, RecipeType<VoxelPrintingRecipe>> VOXEL_PRINTING =
            RECIPE_TYPES.register("voxel_printing", () -> VoxelPrintingRecipe.TYPE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VoxelDecompositionRecipe>>
            VOXEL_DECOMPOSITION_SERIALIZER =
            RECIPE_SERIALIZERS.register("voxel_decomposition", () -> VoxelDecompositionRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VoxelPrintingRecipe>>
            VOXEL_PRINTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("voxel_printing", () -> VoxelPrintingRecipe.SERIALIZER);

    private ModRecipes() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
