package com.starboundmc.block;

import com.mojang.serialization.MapCodec;
import com.starboundmc.block.entity.VoxelPrintingStationBlockEntity;
import com.starboundmc.menu.VoxelPrintingStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Wall-mounted open voxel fabrication bay. Prints tech components from raw
 * materials plus the shared voxel balance. The rear housing, printing bed,
 * and twin probes form a real chamber that drops when its support disappears.
 */
public final class VoxelPrintingStationBlock extends BaseEntityBlock {
    public static final MapCodec<VoxelPrintingStationBlock> CODEC = simpleCodec(VoxelPrintingStationBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Base model faces north, with its rear housing against the south wall. */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(1.0, 2.0, 13.25, 15.0, 15.5, 16.0),
            Block.box(2.5, 2.0, 4.0, 13.5, 5.5, 13.5),
            Block.box(1.0, 5.5, 7.0, 3.85, 15.5, 13.5),
            Block.box(12.15, 5.5, 7.0, 15.0, 15.5, 13.5),
            Block.box(3.85, 13.5, 7.0, 12.15, 15.5, 13.5));
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES.put(facing, rotateShape(Direction.NORTH, facing, NORTH_SHAPE));
        }
    }

    public VoxelPrintingStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = context.getClickedFace().getAxis().isHorizontal()
                ? context.getClickedFace()
                : context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, preferred);
        BlockPos supportPos = context.getClickedPos().relative(preferred.getOpposite());
        return context.getLevel().getBlockState(supportPos).isFaceSturdy(
                context.getLevel(), supportPos, preferred) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof VoxelPrintingStationBlockEntity station) {
                Containers.dropContents(level, pos, station);
                if (level instanceof ServerLevel serverLevel) {
                    station.dropReservedResources(serverLevel, pos);
                }
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoxelPrintingStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.VOXEL_PRINTING_STATION.get(),
                VoxelPrintingStationBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof VoxelPrintingStationBlockEntity station) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, ignored) -> new VoxelPrintingStationMenu(
                                containerId, inventory, station,
                                ContainerLevelAccess.create(level, pos)),
                        Component.translatable("container.starboundmc.voxel_printing_station")), pos);
                station.syncTo(serverPlayer);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = {shape, Shapes.empty()};
        int turns = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < turns; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    buffer[1] = Shapes.or(buffer[1], Shapes.box(
                            1 - maxZ, minY, minX,
                            1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }
}
