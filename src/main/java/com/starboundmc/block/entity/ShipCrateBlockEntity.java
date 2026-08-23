package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.menu.ShipCrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShipCrateBlockEntity extends BlockEntity
{
    private final SimpleContainer container = new SimpleContainer(ShipCrateMenu.SLOTS);

    public ShipCrateBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.SHIP_CRATE.get(), pos, state);
    }

    public SimpleContainer getContainer()
    {
        return container;
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("Items", container.createTag());
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        container.fromTag(tag.getList("Items", Tag.TAG_COMPOUND));
    }
}
