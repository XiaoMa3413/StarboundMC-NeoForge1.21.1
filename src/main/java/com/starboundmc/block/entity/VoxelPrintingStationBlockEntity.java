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
 * Printing station storage: three material input slots and one output slot
 * persisted with the block. Print-task state and wallet deduction land in
 * M2/M3; the slot layout ships first so menu and UI wiring can be verified
 * against a stable container shape.
 */
public final class VoxelPrintingStationBlockEntity extends BlockEntity implements Container {
    public static final int MATERIAL_SLOTS = 3;
    public static final int TOTAL_SLOTS = MATERIAL_SLOTS + 1;

    private final ItemStack[] items = new ItemStack[TOTAL_SLOTS];

    public VoxelPrintingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOXEL_PRINTING_STATION.get(), pos, state);
        java.util.Arrays.fill(items, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < items.length ? items[index] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index >= 0 && index < items.length && !items[index].isEmpty()) {
            ItemStack removed = items[index].split(count);
            if (!removed.isEmpty()) {
                setChanged();
            }
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index >= 0 && index < items.length) {
            ItemStack stack = items[index];
            items[index] = ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                setChanged();
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= items.length) {
            return;
        }
        items[index] = stack;
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
        java.util.Arrays.fill(items, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                list.add(stack.save(registries));
            }
        }
        tag.put("items", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        java.util.Arrays.fill(items, ItemStack.EMPTY);
        if (tag.contains("items")) {
            net.minecraft.nbt.ListTag list = tag.getList("items", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && i < items.length; i++) {
                items[i] = ItemStack.parse(registries, list.getCompound(i)).orElse(ItemStack.EMPTY);
            }
        }
    }
}
