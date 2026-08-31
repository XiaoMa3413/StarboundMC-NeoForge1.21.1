package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Refinery storage: a single material input slot persisted with the block.
 * Decomposition logic lands in M2; the slot layout ships first so menu and
 * UI wiring can be verified against a stable container shape.
 */
public final class VoxelRefineryBlockEntity extends BlockEntity implements Container {
    public static final int INPUT_SLOTS = 1;

    private ItemStack input = ItemStack.EMPTY;

    public VoxelRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOXEL_REFINERY.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return INPUT_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return input.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? input : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index != 0 || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = input.split(count);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index != 0 || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = input;
        input = ItemStack.EMPTY;
        setChanged();
        return removed;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index != 0) {
            return;
        }
        input = stack;
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        input = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!input.isEmpty()) {
            tag.put("input", input.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = tag.contains("input") ? ItemStack.parse(registries, tag.getCompound("input"))
                .orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }
}
