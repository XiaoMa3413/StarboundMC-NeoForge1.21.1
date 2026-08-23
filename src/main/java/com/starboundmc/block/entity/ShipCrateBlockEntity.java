package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.menu.ShipCrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShipCrateBlockEntity extends BlockEntity implements Container
{
    private final SimpleContainer container = new SimpleContainer(ShipCrateMenu.SLOTS);

    public ShipCrateBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.SHIP_CRATE.get(), pos, state);
        container.addListener(ignored -> setChanged());
    }

    public SimpleContainer getContainer()
    {
        return container;
    }

    @Override
    public int getContainerSize()
    {
        return container.getContainerSize();
    }

    @Override
    public boolean isEmpty()
    {
        return container.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return container.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount)
    {
        return container.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        ItemStack removed = container.removeItemNoUpdate(slot);
        if (!removed.isEmpty())
            setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack)
    {
        container.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player)
    {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent()
    {
        container.clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("Items", container.createTag(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        container.fromTag(tag.getList("Items", Tag.TAG_COMPOUND), registries);
    }
}
