package com.starboundmc.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Decomposition recipe: one matched input item refines into a fixed voxel
 * amount held in the refinery's public output buffer. Each craft consumes one item.
 */
public final class VoxelDecompositionRecipe implements Recipe<MachineRecipeInput> {
    public static final MapCodec<VoxelDecompositionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(recipe -> recipe.input),
            com.mojang.serialization.Codec.intRange(1, 1000).fieldOf("voxels").forGetter(recipe -> recipe.voxels)
    ).apply(instance, VoxelDecompositionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoxelDecompositionRecipe> STREAM_CODEC =
            StreamCodec.of(VoxelDecompositionRecipe::write, VoxelDecompositionRecipe::read);

    public static final RecipeType<VoxelDecompositionRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "starboundmc:voxel_decomposition";
        }
    };

    public static final RecipeSerializer<VoxelDecompositionRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<VoxelDecompositionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VoxelDecompositionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private final Ingredient input;
    private final int voxels;

    public VoxelDecompositionRecipe(Ingredient input, int voxels) {
        this.input = input;
        this.voxels = voxels;
    }

    public int voxels() {
        return voxels;
    }

    @Override
    public boolean matches(MachineRecipeInput input, Level level) {
        return this.input.test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(MachineRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    private static VoxelDecompositionRecipe read(RegistryFriendlyByteBuf buffer) {
        Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        int voxels = buffer.readVarInt();
        return new VoxelDecompositionRecipe(input, voxels);
    }

    private static void write(RegistryFriendlyByteBuf buffer, VoxelDecompositionRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
        ByteBufCodecs.VAR_INT.encode(buffer, recipe.voxels);
    }
}
