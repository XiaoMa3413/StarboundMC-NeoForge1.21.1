package com.starboundmc.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.starboundmc.entity.ModEntities;
import com.starboundmc.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The captain's chair; right-click to sit. FACING is the direction a seated player looks. */
public class CaptainChairBlock extends Block
{
    public static final MapCodec<CaptainChairBlock> CODEC = simpleCodec(CaptainChairBlock::new);
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Unrotated model faces south (+Z): seat in the front, backrest on the north side. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.0, 0.0, 3.0, 14.0, 6.0, 13.0),   // seat
            Block.box(2.0, 6.0, 0.0, 14.0, 16.0, 3.0));  // backrest (north, -Z)

    /** Shape rotated per facing, so the collision box follows the visual model. */
    private static final Map<Direction, VoxelShape> SHAPES_BY_FACING = new EnumMap<>(Direction.class);

    static
    {
        for (Direction facing : Direction.Plane.HORIZONTAL)
        {
            // The blockstate rotates the model so the backrest ends up BEHIND the
            // look direction (e.g. facing=west -> y:90 -> the model's north face,
            // the backrest, lands on the east side). The collision must rotate the
            // same way, so it targets facing.getOpposite().
            SHAPES_BY_FACING.put(facing, rotateShape(Direction.NORTH, facing.getOpposite(), SHAPE));
        }
    }

    /** Rotates a NORTH-facing shape around the Y axis to match the given facing. */
    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape)
    {
        VoxelShape[] buffer = new VoxelShape[] { shape, Shapes.empty() };
        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++)
        {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1],
                    Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }

    public CaptainChairBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends Block> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        // The chair faces the player who places it.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        // The collision/outline shape rotates with the facing, matching the
        // model (the model's y-rotation comes from the blockstate).
        return SHAPES_BY_FACING.get(state.getValue(FACING));
    }

    /** The seat/backrest leave the top half of the block empty; use the full cube for
     *  raycasting so right-clicking/breaking works no matter where the chair is aimed. */
    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos)
    {
        return Shapes.block();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        LOGGER.debug("CaptainChairBlock.use side={} player={} pos={} hit={}", level.isClientSide ? "client" : "server", player.getScoreboardName(), pos, hit.getLocation());
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.CONSUME;

        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        SeatEntity seat = seats.isEmpty() ? null : seats.get(0);
        boolean createdNew = false;
        if (seat == null)
        {
            seat = new SeatEntity(ModEntities.SEAT.get(), level);
            // Slight raise so the player sits ON the seat box; yaw matches the chair facing.
            seat.moveTo(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                    state.getValue(FACING).toYRot(), 0.0F);
            level.addFreshEntity(seat);
            createdNew = true;
            LOGGER.debug("CaptainChairBlock spawned seat at {} for {}", seat.position(), player.getScoreboardName());
        }
        else if (seat.isVehicle() && seat.getPassengers().contains(player))
        {
            return InteractionResult.CONSUME;
        }

        boolean riding = serverPlayer.startRiding(seat, true);
        LOGGER.debug("CaptainChairBlock startRiding={} vehicle={} playerPassenger={}", riding, seat.getId(), serverPlayer.isPassenger());
        if (!riding && createdNew)
        {
            seat.discard();
        }
        return InteractionResult.CONSUME;
    }
}
