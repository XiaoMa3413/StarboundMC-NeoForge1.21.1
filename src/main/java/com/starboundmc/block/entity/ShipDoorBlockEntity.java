package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.block.Stage2Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ShipDoorBlockEntity extends BlockEntity
{
    private static final int CHECK_INTERVAL = 8;

    public ShipDoorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.SHIP_DOOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShipDoorBlockEntity be)
    {
        if (level.isClientSide)
            return;
        // The BOTTOM half of a paired door drives the whole door, so the two
        // halves never toggle twice or double the sound.
        if (level.getBlockState(pos.below()).getBlock() instanceof Stage2Blocks.ShipDoor)
            return;
        if (level.getGameTime() % CHECK_INTERVAL != 0)
            return;

        boolean shouldOpen = !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(2.5)).isEmpty();
        boolean open = state.getValue(Stage2Blocks.ShipDoor.OPEN);
        if (shouldOpen != open)
        {
            Stage2Blocks.ShipDoor.setOpen(level, pos, shouldOpen);
            level.playSound(null, pos, shouldOpen ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND,
                    SoundSource.BLOCKS, 0.6F, 1.0F);
        }
    }
}
