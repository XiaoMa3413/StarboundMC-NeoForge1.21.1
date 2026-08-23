package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fuel controller storage: five fuel slots persisted with the block, so fuel
 * placed in the console survives closing the UI and reloading the world.
 */
public class FuelControllerBlockEntity extends BlockEntity implements Container
{
    public static final int FUEL_SLOTS = 5;
    private final ItemStack[] items = new ItemStack[FUEL_SLOTS];

    public FuelControllerBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.FUEL_CONTROLLER.get(), pos, state);
        for (int i = 0; i < this.items.length; i++)
        {
            this.items[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public int getContainerSize()
    {
        return this.items.length;
    }

    @Override
    public boolean isEmpty()
    {
        for (ItemStack stack : this.items)
        {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index)
    {
        return index >= 0 && index < this.items.length ? this.items[index] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count)
    {
        if (index >= 0 && index < this.items.length && !this.items[index].isEmpty())
        {
            ItemStack removed = this.items[index].split(count);
            if (!removed.isEmpty())
            {
                this.setChanged();
            }
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index)
    {
        if (index >= 0 && index < this.items.length)
        {
            ItemStack stack = this.items[index];
            this.items[index] = ItemStack.EMPTY;
            if (!stack.isEmpty())
                this.setChanged();
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack)
    {
        if (index >= 0 && index < this.items.length)
        {
            this.items[index] = stack;
            if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize())
            {
                stack.setCount(this.getMaxStackSize());
            }
            this.setChanged();
        }
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player)
    {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent()
    {
        for (int i = 0; i < this.items.length; i++)
        {
            this.items[i] = ItemStack.EMPTY;
        }
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (ItemStack stack : this.items)
        {
            list.add(stack.saveOptional(registries));
        }
        tag.put("FuelItems", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.items.length; i++)
            this.items[i] = ItemStack.EMPTY;
        if (tag.contains("FuelItems", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("FuelItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < this.items.length && i < list.size(); i++)
            {
                this.items[i] = ItemStack.parseOptional(registries, list.getCompound(i));
            }
        }
    }
}
