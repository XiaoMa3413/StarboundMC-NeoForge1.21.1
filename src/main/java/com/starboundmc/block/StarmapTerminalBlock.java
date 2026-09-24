package com.starboundmc.block;

import com.starboundmc.menu.StarmapTerminalMenu;
import com.starboundmc.story.ShipEnvironmentService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;

/** Dedicated entry block for the LDLib2 starmap redraw. */
public final class StarmapTerminalBlock extends Block {
    public static final MapCodec<StarmapTerminalBlock> CODEC = simpleCodec(StarmapTerminalBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        VoxelShape shape = createNorthShape();
        for (int turn = 0; turn < 4; turn++) {
            SHAPES[turn] = shape;
            VoxelShape[] rotated = {Shapes.empty()};
            shape.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
            shape = rotated[0];
        }
    }

    private static VoxelShape createNorthShape() {
        // Model units: the operator stands north (-Z); the deck rises 0.43 units per Z.
        VoxelShape shape = Block.box(1.3, 0, 2.4, 14.7, 1.18, 14.4);
        // Follow the rearward tapered support without filling the front overhang.
        for (double y = 1.04; y < 13.2; y += 1) {
            double top = Math.min(y + 1, 13.2);
            double taper = Math.clamp((y - 1.04) / 11.8, 0, 1);
            double left = 3.65 + 0.8 * taper;
            double front = Math.max(6.25 + 1.5 * taper, 1.5 + (top - 8.51) / 0.43);
            shape = Shapes.or(shape, Block.box(left, y, front, 16 - left, top, 13.55));
        }
        // One-unit depth slices approximate the inclined shell to within 0.43 units.
        // A single enclosing box would block clicks and movement above the low front edge.
        for (double z = 1.3; z < 14.65; z += 1) {
            double back = Math.min(z + 1, 14.65);
            double bottom = 8.28 + 0.43 * (z - 1.5);
            double top = Math.min(15.61, 10.04 + 0.43 * (back - 1.5));
            shape = Shapes.or(shape, Block.box(0.72, bottom, z, 15.28, top, back));
        }
        return shape.optimize();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[switch (state.getValue(FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        }];
    }

    public StarmapTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.starboundmc.starmap_terminal");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                        int containerId, net.minecraft.world.entity.player.Inventory inventory, Player ignored) {
                    return new StarmapTerminalMenu(containerId, inventory,
                            ContainerLevelAccess.create(level, pos), pos);
                }
            }).ifPresent(containerId -> ShipEnvironmentService.sendSnapshot(serverPlayer, containerId));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
