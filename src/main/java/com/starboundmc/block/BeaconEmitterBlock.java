package com.starboundmc.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BeaconEmitterBlock extends Block {
    public static final MapCodec<BeaconEmitterBlock> CODEC = simpleCodec(BeaconEmitterBlock::new);
    private static final VoxelShape SHAPE = MachineryShapes.boxes(
            new double[]{3,0,3,13,4.3,13}, new double[]{4.6,4.3,4.6,11.4,10.3,11.4},
            new double[]{2.8,10.3,2.8,13.2,12.8,13.2}, new double[]{6.8,12.8,6.8,9.2,15.8,9.2});
    public BeaconEmitterBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<BeaconEmitterBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
