package com.starboundmc.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarmapTerminalBlockTest {
    private final StarmapTerminalBlock block = new StarmapTerminalBlock(BlockBehaviour.Properties.of());

    @Test
    void exposesEveryHorizontalFacing() {
        assertEquals(Set.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST),
                Set.copyOf(StarmapTerminalBlock.FACING.getPossibleValues()));
        assertEquals(Direction.NORTH, block.defaultBlockState().getValue(StarmapTerminalBlock.FACING));
    }

    @Test
    void rotatesFacingWithTheBlockState() {
        BlockState north = block.defaultBlockState().setValue(StarmapTerminalBlock.FACING, Direction.NORTH);

        assertEquals(Direction.EAST,
                block.rotate(north, Rotation.CLOCKWISE_90).getValue(StarmapTerminalBlock.FACING));
        assertEquals(Direction.SOUTH,
                block.rotate(north, Rotation.CLOCKWISE_180).getValue(StarmapTerminalBlock.FACING));
        assertEquals(Direction.WEST,
                block.rotate(north, Rotation.COUNTERCLOCKWISE_90).getValue(StarmapTerminalBlock.FACING));
    }
}
