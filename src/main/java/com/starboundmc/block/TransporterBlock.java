package com.starboundmc.block;

import com.mojang.serialization.MapCodec;
import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.TeleporterListPacketHelper;
import com.starboundmc.story.ShipEnvironmentService;
import com.starboundmc.world.TeleporterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Three occupied blocks, an open passenger bay, and one menu/registry anchor. */
public class TransporterBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    public static final double DECK_HEIGHT = 9.0 / 16;
    private static final VoxelShape[][] SHAPES = new VoxelShape[3][4];
    static {
        VoxelShape rear = Block.box(1.25, 0, 13.1, 14.75, 16, 15.85);
        VoxelShape[] north = {
                Shapes.or(Block.box(.25, 0, .2, 15.75, 9, 15.9), rear),
                Shapes.or(rear, Block.box(4.2, 7, 12.88, 11.8, 13.8, 13.45)),
                Shapes.or(Block.box(1.25, 0, 13.1, 14.75, 14.6, 15.85),
                        Block.box(.5, 10.38, .05, 15.5, 15.95, 15.9))};
        for (int p = 0; p < 3; p++) {
            var shape = north[p];
            for (int r = 0; r < 4; r++) {
                SHAPES[p][r] = shape.optimize();
                VoxelShape[] next = {Shapes.empty()};
                shape.forAllBoxes((x0,y0,z0,x1,y1,z1) -> next[0] = Shapes.or(next[0], Shapes.box(1-z1,y0,x0,1-z0,y1,x1)));
                shape = next[0];
            }
        }
    }

    public TransporterBlock(Properties properties) {
        super(properties);
        // Pre-facing saves load this default: the authored ship opens south into the cabin.
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, 0));
    }

    @Override protected MapCodec<? extends Block> codec() { return simpleCodec(TransporterBlock::new); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, PART); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int rotation = switch (state.getValue(FACING)) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        return SHAPES[state.getValue(PART)][rotation];
    }
    @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return rotate(s, m.getRotation(s.getValue(FACING))); }

    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (pos.getY() + 2 >= level.getMaxBuildHeight()) return null;
        for (int i = 1; i <= 2; i++)
            if (!level.getBlockState(pos.above(i)).canBeReplaced(context) || !level.getFluidState(pos.above(i)).isEmpty()
                    || !level.isUnobstructed(state.setValue(PART, i), pos.above(i), CollisionContext.empty())) return null;
        return state;
    }

    public static BlockPos basePos(BlockPos pos, BlockState state) { return pos.below(state.getValue(PART)); }

    /** Called on placement and when legacy chunks load. Never replaces occupied space. */
    public static boolean ensureAssembly(ServerLevel level, BlockPos base) {
        BlockState lower = level.getBlockState(base);
        if (!(lower.getBlock() instanceof TransporterBlock) || lower.getValue(PART) != 0
                || base.getY() + 2 >= level.getMaxBuildHeight()) return false;
        for (int i = 1; i <= 2; i++) {
            var current = level.getBlockState(base.above(i));
            if (current.is(lower.getBlock()) && current.getValue(PART) == i) continue;
            if (!current.canBeReplaced() || !current.getFluidState().isEmpty()) return false;
        }
        for (int i = 1; i <= 2; i++) {
            var desired = lower.setValue(PART, i);
            if (level.getBlockState(base.above(i)) != desired) level.setBlock(base.above(i), desired, 3);
        }
        return true;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel server) ensureAssembly(server, pos);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) == 0) ensureAssembly(level, pos);
        else {
            var lower = level.getBlockState(basePos(pos, state));
            if (!lower.is(this) || lower.getValue(PART) != 0) level.removeBlock(pos, false);
        }
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (!level.isClientSide() && state.getValue(PART) == 0 && direction == Direction.UP) level.scheduleTick(pos, this, 1);
        return super.updateShape(state, direction, other, level, pos, otherPos);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos base = basePos(pos, state);
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel server && ensureAssembly(server, base)) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, ignored) -> new TeleporterMenu(id, inventory, ContainerLevelAccess.create(level, base), base),
                    Component.translatable("container.starboundmc.teleporter")))
                    .ifPresent(id -> ShipEnvironmentService.sendSnapshot(serverPlayer, id));
            ModNetwork.sendToPlayer(serverPlayer, TeleporterListPacketHelper.build(serverPlayer.getServer(), level.dimension(), base));
        } else if (player instanceof ServerPlayer) {
            player.displayClientMessage(Component.translatable("message.starboundmc.teleporter.obstructed"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) {
            BlockPos base = basePos(pos, state);
            if (level.getServer() != null) TeleporterManager.remove(level.getServer(), level.dimension(), base);
            // Only the originally broken part owns loot. Sibling cleanup never drops;
            // vanilla mining, creative, explosions and manipulator tools keep their own policy.
            for (int i = 0; i <= 2; i++) {
                var childPos = base.above(i);
                var child = level.getBlockState(childPos);
                if (!childPos.equals(pos) && child.is(this) && child.getValue(PART) == i)
                    level.setBlock(childPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }

    public static Vec3 landingPosition(BlockPos base, BlockState state) {
        Vec3 offset = new Vec3(0, 0, 7.3 / 16 - .5);
        offset = switch (state.getValue(FACING)) {
            case EAST -> new Vec3(-offset.z, 0, offset.x);
            case SOUTH -> offset.scale(-1);
            case WEST -> new Vec3(offset.z, 0, -offset.x);
            default -> offset;
        };
        return new Vec3(base.getX() + .5 + offset.x, base.getY() + DECK_HEIGHT, base.getZ() + .5 + offset.z);
    }

    public static boolean teleportHere(ServerPlayer player, ServerLevel level, BlockPos base) {
        if (!ensureAssembly(level, base)) return refuse(player);
        Vec3 target = landingPosition(base, level.getBlockState(base));
        var bounds = player.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(target).deflate(.001);
        if (!level.noCollision(player, bounds)) return refuse(player);
        var source = com.starboundmc.world.TransporterEffects.origin(player);
        player.stopRiding();
        player.teleportTo(level, target.x, target.y, target.z, level.getBlockState(base).getValue(FACING).toYRot(), 0);
        if (player.serverLevel() != level || player.position().distanceToSqr(target) > .001) return false;
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        com.starboundmc.world.TransporterEffects.afterTravel(player, source);
        return true;
    }
    private static boolean refuse(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.starboundmc.teleporter.obstructed"), true);
        return false;
    }
}
