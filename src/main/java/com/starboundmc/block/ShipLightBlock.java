package com.starboundmc.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Constant ship lighting with vanilla wall, floor and ceiling attachment behavior. */
public final class ShipLightBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<ShipLightBlock> CODEC = simpleCodec(ShipLightBlock::new);
    private static final Map<Direction, VoxelShape> WALL = MachineryShapes.horizontal(Block.box(1,5,12.45,15,11,16));
    private static final Map<Direction, VoxelShape> FLOOR = MachineryShapes.horizontal(Block.box(1,0,5,15,3.55,11));
    private static final Map<Direction, VoxelShape> CEILING = MachineryShapes.horizontal(Block.box(1,12.45,5,15,16,11));

    public ShipLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACE, AttachFace.WALL).setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<ShipLightBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR.get(state.getValue(FACING));
            case CEILING -> CEILING.get(state.getValue(FACING));
            case WALL -> WALL.get(state.getValue(FACING));
        };
    }
}
