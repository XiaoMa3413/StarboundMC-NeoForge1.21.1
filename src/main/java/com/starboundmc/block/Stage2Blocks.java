package com.starboundmc.block;

import com.mojang.serialization.MapCodec;
import com.starboundmc.block.entity.AlloyFurnaceBlockEntity;
import com.starboundmc.block.entity.FuelControllerBlockEntity;
import com.starboundmc.block.entity.ShipCrateBlockEntity;
import com.starboundmc.block.entity.ShipDoorBlockEntity;
import com.starboundmc.menu.AlloyFurnaceMenu;
import com.starboundmc.menu.FuelControllerMenu;
import com.starboundmc.menu.ShipConsoleMenu;
import com.starboundmc.menu.ShipCrateMenu;
import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.menu.UpgradeMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.TeleporterListPacketHelper;
import com.starboundmc.world.TeleporterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Stage 2 block implementations for systems whose full behavior belongs to a
 * later migration stage. The legacy Forge classes remain untouched beside
 * these adapters and will replace them when their dependencies are migrated.
 */
public final class Stage2Blocks {
    private Stage2Blocks() {
    }

    private abstract static class FacingBlock extends Block {
        static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

        FacingBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
    }

    private abstract static class FacingEntityBlock extends BaseEntityBlock {
        static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

        FacingEntityBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }
    }

    public static final class Workbench extends FacingBlock {
        public static final MapCodec<Workbench> CODEC = simpleCodec(Workbench::new);

        public Workbench(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, ignored) -> new UpgradeMenu(containerId, inventory,
                                ContainerLevelAccess.create(level, pos)),
                        Component.translatable("container.starboundmc.matter_manipulator_workbench")));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static final class ShipConsole extends FacingBlock {
        public static final MapCodec<ShipConsole> CODEC = simpleCodec(ShipConsole::new);

        public ShipConsole(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, ignored) -> new ShipConsoleMenu(containerId, inventory,
                                ContainerLevelAccess.create(level, pos)),
                        Component.translatable("container.starboundmc.ship_console")));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static final class Teleporter extends Block {
        public static final MapCodec<Teleporter> CODEC = simpleCodec(Teleporter::new);

        public Teleporter(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, ignored) -> new TeleporterMenu(containerId, inventory,
                                ContainerLevelAccess.create(level, pos), pos),
                        Component.translatable("container.starboundmc.teleporter")));
                ModNetwork.sendToPlayer(serverPlayer,
                        TeleporterListPacketHelper.build(serverPlayer.getServer(), level.dimension(), pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock()) && !level.isClientSide && level.getServer() != null) {
                TeleporterManager.remove(level.getServer(), level.dimension(), pos);
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    public static final class ShipCrate extends FacingEntityBlock {
        public static final MapCodec<ShipCrate> CODEC = simpleCodec(ShipCrate::new);

        public ShipCrate(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ShipCrateBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof ShipCrateBlockEntity crate) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, ignored) -> new ShipCrateMenu(containerId, inventory,
                                crate.getContainer(), ContainerLevelAccess.create(level, pos), crate::setChanged),
                        Component.translatable("container.starboundmc.ship_crate")));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock())
                    && level.getBlockEntity(pos) instanceof ShipCrateBlockEntity crate) {
                Containers.dropContents(level, pos, crate.getContainer());
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    public static final class ShipDoor extends BaseEntityBlock {
        public static final MapCodec<ShipDoor> CODEC = simpleCodec(ShipDoor::new);
        public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

        public ShipDoor(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(OPEN, false).setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(OPEN, FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ShipDoorBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> type) {
            return createTickerHelper(type, ModBlockEntities.SHIP_DOOR.get(), ShipDoorBlockEntity::tick);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        public static void setOpen(Level level, BlockPos pos, boolean open) {
            setOpenAt(level, pos, open);
            setOpenAt(level, pos.above(), open);
            setOpenAt(level, pos.below(), open);
        }

        private static void setOpenAt(Level level, BlockPos pos, boolean open) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ShipDoor && state.getValue(OPEN) != open)
                level.setBlock(pos, state.setValue(OPEN, open), 3);
        }
    }

    public static final class FuelController extends FacingEntityBlock {
        public static final MapCodec<FuelController> CODEC = simpleCodec(FuelController::new);

        public FuelController(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new FuelControllerBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof FuelControllerBlockEntity fuel) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, ignored) -> new FuelControllerMenu(containerId, inventory, fuel,
                                ContainerLevelAccess.create(level, pos)),
                        Component.translatable("container.starboundmc.fuel_controller")));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock())
                    && level.getBlockEntity(pos) instanceof FuelControllerBlockEntity fuel) {
                Containers.dropContents(level, pos, fuel);
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    public static final class AlloyFurnace extends FacingEntityBlock {
        public static final MapCodec<AlloyFurnace> CODEC = simpleCodec(AlloyFurnace::new);

        public AlloyFurnace(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new AlloyFurnaceBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> type) {
            return createTickerHelper(type, ModBlockEntities.ALLOY_FURNACE.get(), AlloyFurnaceBlockEntity::tick);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            if (player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof AlloyFurnaceBlockEntity furnace) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, ignored) -> new AlloyFurnaceMenu(containerId, inventory,
                                furnace.getInventory(), furnace.getContainerData(),
                                ContainerLevelAccess.create(level, pos)),
                        Component.translatable("container.starboundmc.titanium_alloy_furnace")));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock())
                    && level.getBlockEntity(pos) instanceof AlloyFurnaceBlockEntity furnace) {
                Containers.dropContents(level, pos, furnace.getInventory());
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }
}
