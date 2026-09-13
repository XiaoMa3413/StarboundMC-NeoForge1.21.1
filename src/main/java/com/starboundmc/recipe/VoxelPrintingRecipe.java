package com.starboundmc.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Printing recipe: up to six material ingredients (each with a required
 * count, aggregated across the operator inventory). A voxel ingredient is
 * backed by the operator's voxel wallet, but remains an ordinary entry in the
 * same material list. Materials and voxels are reserved when work enters the
 * print queue.
 */
public final class VoxelPrintingRecipe implements Recipe<MachineRecipeInput> {
    public static final int MAX_INGREDIENTS = 6;
    /** Material counts may span multiple inventory stacks, like the old voxel cost did. */
    public static final int MAX_MATERIAL_COUNT = Integer.MAX_VALUE;

    /** One required material: what to match and how many items it costs. */
    public record MaterialEntry(Ingredient ingredient, int count) {
        public static final Codec<MaterialEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(MaterialEntry::ingredient),
                Codec.intRange(1, MAX_MATERIAL_COUNT).optionalFieldOf("count", 1).forGetter(MaterialEntry::count)
        ).apply(instance, MaterialEntry::new));

        public MaterialEntry {
            if (count < 1) {
                throw new IllegalArgumentException("Material count must be positive: " + count);
            }
        }

        /** Whether this entry uses the wallet-backed voxel material. */
        public boolean isVoxel() {
            ItemStack[] matchingItems = ingredient.getItems();
            return matchingItems.length == 1 && matchingItems[0].is(ModItems.VOXEL.get());
        }
    }

    public static final MapCodec<VoxelPrintingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MaterialEntry.CODEC.listOf().fieldOf("materials").forGetter(recipe -> recipe.materials),
            Codec.intRange(1, 3600).fieldOf("print_seconds").forGetter(recipe -> recipe.printSeconds),
            ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
    ).apply(instance, VoxelPrintingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoxelPrintingRecipe> STREAM_CODEC =
            StreamCodec.of(VoxelPrintingRecipe::write, VoxelPrintingRecipe::read);

    public static final RecipeType<VoxelPrintingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "starboundmc:voxel_printing";
        }
    };

    public static final RecipeSerializer<VoxelPrintingRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<VoxelPrintingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VoxelPrintingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private final List<MaterialEntry> materials;
    private final int printSeconds;
    private final ItemStack result;

    public VoxelPrintingRecipe(List<MaterialEntry> materials, int printSeconds, ItemStack result) {
        if (materials.size() > MAX_INGREDIENTS) {
            throw new IllegalArgumentException("Too many material ingredients: " + materials.size());
        }
        this.materials = List.copyOf(materials);
        this.printSeconds = printSeconds;
        this.result = result;
    }

    /** Returns the total wallet-backed voxel material count required for one craft. */
    public long voxelMaterialCount() {
        long total = 0;
        for (MaterialEntry entry : materials) {
            if (entry.isVoxel()) {
                total += entry.count();
            }
        }
        return total;
    }

    public int printSeconds() {
        return printSeconds;
    }

    public List<MaterialEntry> materials() {
        return materials;
    }

    /**
     * Consumes one craft's inventory-backed materials from a list of working-copy
     * stacks. Unrelated inventory contents are ignored; matching stacks are
     * consumed in source order. Wallet-backed voxel entries are accounted for by
     * {@link #voxelMaterialCount()} and are intentionally not removed from the inventory.
     *
     * <p>The working copies may be partially changed when the result is empty.
     * Callers must discard a failed simulation rather than applying it.</p>
     */
    public Optional<List<ItemStack>> reserveMaterials(List<ItemStack> simulatedSources) {
        List<MaterialEntry> inventoryMaterials = materials.stream()
                .filter(entry -> !entry.isVoxel())
                .toList();
        return MaterialReservationPlanner.reserve(
                simulatedSources,
                inventoryMaterials,
                MaterialEntry::count,
                (entry, stack) -> !stack.isEmpty() && entry.ingredient().test(stack),
                ItemStack::getCount,
                ItemStack::shrink,
                ItemStack::copyWithCount);
    }

    /**
     * True when every material entry's count is covered by the slots and no
     * slot holds an item no entry asks for.
     */
    @Override
    public boolean matches(MachineRecipeInput input, Level level) {
        List<MaterialEntry> remaining = materials.stream()
                .filter(entry -> !entry.isVoxel())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            boolean matched = false;
            for (int i = 0; i < remaining.size(); i++) {
                MaterialEntry entry = remaining.get(i);
                if (entry.ingredient().test(stack)) {
                    matched = true;
                    if (stack.getCount() >= entry.count()) {
                        remaining.remove(i);
                    } else {
                        remaining.set(i, new MaterialEntry(entry.ingredient(), entry.count() - stack.getCount()));
                    }
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return remaining.stream().allMatch(entry -> entry.count() <= 0);
    }

    @Override
    public ItemStack assemble(MachineRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    private static VoxelPrintingRecipe read(RegistryFriendlyByteBuf buffer) {
        int entryCount = buffer.readVarInt();
        if (entryCount < 0 || entryCount > MAX_INGREDIENTS) {
            throw new IllegalArgumentException("Too many material ingredients: " + entryCount);
        }
        List<MaterialEntry> materials = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            int count = buffer.readVarInt();
            materials.add(new MaterialEntry(ingredient, count));
        }
        int printSeconds = buffer.readVarInt();
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        return new VoxelPrintingRecipe(materials, printSeconds, result);
    }

    private static void write(RegistryFriendlyByteBuf buffer, VoxelPrintingRecipe recipe) {
        buffer.writeVarInt(recipe.materials.size());
        for (MaterialEntry entry : recipe.materials) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, entry.ingredient());
            buffer.writeVarInt(entry.count());
        }
        buffer.writeVarInt(recipe.printSeconds);
        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
    }
}
