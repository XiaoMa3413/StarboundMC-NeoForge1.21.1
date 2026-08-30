package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShipDoorBlockEntity extends BlockEntity
{
    public ShipDoorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.SHIP_DOOR.get(), pos, state);
    }
}
