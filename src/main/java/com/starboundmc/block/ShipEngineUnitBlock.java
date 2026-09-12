package com.starboundmc.block;

import com.mojang.serialization.MapCodec;
import com.starboundmc.block.entity.ShipEngineBlockEntity;
import com.starboundmc.menu.ShipEngineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/** Dedicated shipboard power/ignition unit; the existing ship_engine ID remains a thruster. */
public class ShipEngineUnitBlock extends BaseEntityBlock
{
    public static final MapCodec<ShipEngineUnitBlock> CODEC = simpleCodec(ShipEngineUnitBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ShipEngineUnitBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShipEngineBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Recover a missing socket without changing the block or shared story state. */
    public static ShipEngineBlockEntity storage(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.SHIP_ENGINE_UNIT.get())) return null;
        if (level.getBlockEntity(pos) instanceof ShipEngineBlockEntity engine) return engine;
        if (level.isClientSide || level.getBlockEntity(pos) != null) return null;
        var engine = new ShipEngineBlockEntity(pos, level.getBlockState(pos));
        level.setBlockEntity(engine);
        engine.setChanged();
        return engine;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            var engine = storage(level, pos);
            if (engine != null) serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, owner) -> new ShipEngineMenu(id, inventory, engine),
                    Component.translatable("container.starboundmc.ship_engine")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.or(
                Block.box(1, 0, 1, 15, 3, 15),
                Block.box(1.5, 3, 1.5, 14.5, 15, 14.5),
                Block.box(5, 15, 5, 11, 16, 11));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ShipEngineBlockEntity engine)
                Containers.dropContents(level, pos, engine);
            super.onRemove(state, level, pos, next, moving);
        }
    }
}
