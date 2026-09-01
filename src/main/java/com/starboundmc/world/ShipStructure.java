package com.starboundmc.world;

import com.starboundmc.block.CaptainChairBlock;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.ShipAiTerminalBlock;
import com.starboundmc.block.ShipEngineBlock;
import com.starboundmc.block.StarmapTerminalBlock;
import com.starboundmc.block.entity.AlloyFurnaceBlockEntity;
import com.starboundmc.block.entity.FuelControllerBlockEntity;
import com.starboundmc.block.entity.ShipCrateBlockEntity;
import com.starboundmc.block.entity.ShipDoorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Builds the default compact starter scout.
 *
 * <p>The central hull is generated from longitudinal slices rather than a
 * rectangular room. A narrow shared cabin keeps the starter ship modest while
 * the tapered bow, short keel and outboard engine pods spend more of the block
 * budget on a readable exterior silhouette.</p>
 *
 * <p>This procedural ship remains the fallback. A custom structure template at
 * {@code data/starboundmc/structures/ship.nbt} still overrides it through
 * {@link ShipTemplatePlacer}.</p>
 */
public class ShipStructure
{
    public static final int FLOOR_Y = StarterShipHullProfile.FLOOR_Y;
    public static final int CEIL_Y = StarterShipHullProfile.MAX_Y;
    public static final int MIN_X = StarterShipHullProfile.MIN_X;
    public static final int MAX_X = StarterShipHullProfile.MAX_X;
    public static final int MIN_Z = StarterShipHullProfile.MIN_Z;
    public static final int MAX_Z = StarterShipHullProfile.MAX_Z;

    /** Unified teleporter at the center of the shared cabin. */
    public static final BlockPos SHIP_TELEPORTER_POS = new BlockPos(0, FLOOR_Y + 1, 0);
    /** Shipboard AI terminal at the cabin's forward edge, facing the cockpit. */
    public static final BlockPos SHIP_AI_TERMINAL_POS = new BlockPos(0, FLOOR_Y + 1, 3);
    public static final Direction SHIP_AI_TERMINAL_FACING = Direction.SOUTH;
    /** Wall-mounted voxel printing station on the port-side cabin wall. */
    public static final BlockPos SHIP_VOXEL_PRINTING_STATION_POS = new BlockPos(-4, FLOOR_Y + 1, 1);
    public static final Direction SHIP_VOXEL_PRINTING_STATION_FACING = Direction.EAST;

    private static final int ENGINE_BULKHEAD_Z = -3;

    /**
     * Block states are resolved only when the procedural structure is placed.
     * This keeps geometry constants usable from data-only tests and avoids
     * touching Minecraft registries during class loading.
     */
    private static final class Palette
    {
        private static final BlockState FLOOR = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        private static final BlockState CEIL = Blocks.WHITE_CONCRETE.defaultBlockState();
        private static final BlockState HULL = Blocks.WHITE_CONCRETE.defaultBlockState();
        private static final BlockState TRIM = Blocks.GRAY_CONCRETE.defaultBlockState();
        private static final BlockState ACCENT = Blocks.CYAN_CONCRETE.defaultBlockState();
        private static final BlockState GLASS = Blocks.TINTED_GLASS.defaultBlockState();
        private static final BlockState LAMP = Blocks.SEA_LANTERN.defaultBlockState();
        private static final BlockState IRON = Blocks.IRON_BLOCK.defaultBlockState();
        /** Engine front faces into the ship (+Z), leaving the nozzle texture visible from the stern. */
        private static final BlockState ENGINE = ModBlocks.SHIP_ENGINE.get().defaultBlockState()
                .setValue(ShipEngineBlock.FACING, Direction.SOUTH);

        private Palette()
        {
        }
    }

    public static int placeInChunk(ChunkAccess chunk)
    {
        int count = 0;
        count += placeHull(chunk);
        count += placeEngineBulkhead(chunk);
        count += placeLighting(chunk);
        count += placeDeckDetails(chunk);
        count += placeFurniture(chunk);
        return count;
    }

    private static int placeHull(ChunkAccess chunk)
    {
        int count = 0;
        for (int x = MIN_X; x <= MAX_X; x++)
            for (int z = MIN_Z; z <= MAX_Z; z++)
                for (int y = StarterShipHullProfile.MIN_Y; y <= StarterShipHullProfile.MAX_Y; y++)
                {
                    if (StarterShipHullProfile.isMainShell(x, y, z))
                        count += place(chunk, x, y, z, mainHullBlock(x, y, z));
                    else if (StarterShipHullProfile.containsEnginePod(x, y, z))
                        count += place(chunk, x, y, z, enginePodBlock(x, y, z));
                    else if (StarterShipHullProfile.isKeel(x, y, z))
                        count += place(chunk, x, y, z, Palette.ACCENT);
                }
        return count;
    }

    private static BlockState mainHullBlock(int x, int y, int z)
    {
        StarterShipHullProfile.Slice slice = StarterShipHullProfile.sliceAt(z);
        if (slice == null)
            return Palette.HULL;

        int absX = Math.abs(x);
        boolean side = absX == slice.halfWidth();
        boolean forwardFace = !StarterShipHullProfile.containsMainVolume(x, y, z + 1);
        boolean rearFace = !StarterShipHullProfile.containsMainVolume(x, y, z - 1);

        if (z == MIN_Z && y >= 102 && y <= 104)
            return Palette.ENGINE;

        if (isCockpitGlass(x, y, z, slice, side, forwardFace))
            return Palette.GLASS;

        if (side && z >= -2 && z <= 3 && y >= 102 && y <= 103)
            return Palette.GLASS;

        if (y == slice.floorY())
            return Palette.FLOOR;

        if (y == slice.roofY())
        {
            if (side || forwardFace || rearFace)
                return Palette.ACCENT;
            return Palette.CEIL;
        }

        if (y == slice.floorY() + 1 && (side || forwardFace || rearFace))
            return Palette.TRIM;

        if (side && y == 104 && z >= -4 && z <= 3)
            return Palette.ACCENT;

        if (forwardFace || rearFace)
            return Palette.TRIM;

        return Palette.HULL;
    }

    private static boolean isCockpitGlass(int x, int y, int z,
                                           StarterShipHullProfile.Slice slice,
                                           boolean side, boolean forwardFace)
    {
        if (z < 4 || z > 8)
            return false;
        if (y == slice.roofY() && Math.abs(x) <= Math.min(2, slice.halfWidth()))
            return true;
        return y >= 102 && y <= 104 && (side || forwardFace);
    }

    private static BlockState enginePodBlock(int x, int y, int z)
    {
        if (z == -8 && y >= 102 && y <= 104)
            return Palette.ENGINE;
        if (z == -4)
            return Palette.ACCENT;
        if (y == 100 || y == 106)
            return Palette.TRIM;
        if (Math.abs(x) == 6 && y == 103)
            return Palette.ACCENT;
        return Palette.HULL;
    }

    /** One narrow automatic bulkhead isolates the engineering section. */
    private static int placeEngineBulkhead(ChunkAccess chunk)
    {
        int count = 0;
        for (int x = -3; x <= 3; x++)
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 4; y++)
            {
                BlockState state;
                if (x == 0 && y <= FLOOR_Y + 2)
                    state = ModBlocks.SHIP_DOOR.get().defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
                else if (y == FLOOR_Y + 3 && Math.abs(x) <= 1)
                    state = Palette.GLASS;
                else if (y == FLOOR_Y + 4)
                    state = Palette.ACCENT;
                else
                    state = Math.abs(x) == 1 ? Palette.TRIM : Palette.HULL;
                count += place(chunk, x, y, ENGINE_BULKHEAD_Z, state);
            }
        return count;
    }

    private static int placeLighting(ChunkAccess chunk)
    {
        int count = 0;
        int[] zs = { -5, 0, 5 };
        for (int z : zs)
        {
            StarterShipHullProfile.Slice slice = StarterShipHullProfile.sliceAt(z);
            if (slice != null)
                count += place(chunk, 0, slice.roofY(), z, Palette.LAMP);
        }
        return count;
    }

    private static int placeDeckDetails(ChunkAccess chunk)
    {
        int count = 0;

        // Engineering aisle.
        for (int z = -7; z <= -4; z++)
                    count += place(chunk, 0, FLOOR_Y, z, Palette.ACCENT);

        // Compact 3x3 teleporter marking instead of the old 5x5 pad.
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                if (x != 0 || z != 0)
                    count += place(chunk, x, FLOOR_Y, z, Palette.ACCENT);

        // Five-wide raised cockpit platform.
        for (int x = -2; x <= 2; x++)
            for (int z = 4; z <= 7; z++)
            {
                boolean border = Math.abs(x) == 2 || z == 4 || z == 7;
                count += place(chunk, x, FLOOR_Y + 1, z,
                        border ? Palette.ACCENT : Palette.FLOOR);
            }
        return count;
    }

    private static int placeFurniture(ChunkAccess chunk)
    {
        int count = 0;

        // Engineering: small exposed reactor with two fuel crates.
        count += place(chunk, 0, FLOOR_Y + 2, -6, Palette.LAMP);
        count += place(chunk, 0, FLOOR_Y + 1, -6, Palette.IRON);
        count += place(chunk, 0, FLOOR_Y + 3, -6, Palette.IRON);
        count += place(chunk, -1, FLOOR_Y + 2, -6, Palette.IRON);
        count += place(chunk, 1, FLOOR_Y + 2, -6, Palette.IRON);
        count += place(chunk, -2, FLOOR_Y + 1, -5, crateFacing(-2));
        count += place(chunk, 2, FLOOR_Y + 1, -5, crateFacing(2));

        // Shared cabin: every function remains, but wall equipment replaces open floor area.
        count += place(chunk, SHIP_TELEPORTER_POS.getX(), SHIP_TELEPORTER_POS.getY(), SHIP_TELEPORTER_POS.getZ(),
                ModBlocks.TELEPORTER.get().defaultBlockState());
        count += place(chunk, SHIP_AI_TERMINAL_POS.getX(), SHIP_AI_TERMINAL_POS.getY(),
                SHIP_AI_TERMINAL_POS.getZ(),
                ModBlocks.SHIP_AI_TERMINAL.get().defaultBlockState()
                        .setValue(ShipAiTerminalBlock.FACING, SHIP_AI_TERMINAL_FACING));
        count += place(chunk, -3, FLOOR_Y + 1, -1, crateFacing(-3));
        count += place(chunk, 3, FLOOR_Y + 1, -1, crateFacing(3));
        count += place(chunk, -3, FLOOR_Y + 1, 2,
                ModBlocks.MATTER_MANIPULATOR_WORKBENCH.get().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        count += place(chunk, 3, FLOOR_Y + 1, 2,
                ModBlocks.TITANIUM_ALLOY_FURNACE.get().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        count += place(chunk, -3, FLOOR_Y + 1, 3, Blocks.POTTED_CACTUS.defaultBlockState());
        count += place(chunk, SHIP_VOXEL_PRINTING_STATION_POS.getX(), SHIP_VOXEL_PRINTING_STATION_POS.getY(),
                SHIP_VOXEL_PRINTING_STATION_POS.getZ(),
                ModBlocks.VOXEL_PRINTING_STATION.get().defaultBlockState()
                        .setValue(com.starboundmc.block.VoxelPrintingStationBlock.FACING,
                                SHIP_VOXEL_PRINTING_STATION_FACING));

        // Cockpit: chair and all controls fit on one compact raised deck.
        count += place(chunk, 0, FLOOR_Y + 2, 5,
                ModBlocks.CAPTAIN_CHAIR.get().defaultBlockState()
                        .setValue(CaptainChairBlock.FACING, Direction.SOUTH));
        count += place(chunk, 0, FLOOR_Y + 2, 7,
                ModBlocks.STARMAP_TERMINAL.get().defaultBlockState()
                        .setValue(StarmapTerminalBlock.FACING, Direction.NORTH));
        count += place(chunk, 2, FLOOR_Y + 2, 7,
                ModBlocks.STARMAP_TERMINAL.get().defaultBlockState()
                        .setValue(StarmapTerminalBlock.FACING, Direction.NORTH));
        count += place(chunk, -2, FLOOR_Y + 2, 7,
                ModBlocks.FUEL_CONTROLLER.get().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        return count;
    }

    /** Crate latch faces the center aisle. */
    private static BlockState crateFacing(int x)
    {
        return ModBlocks.SHIP_CRATE.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, x < 0 ? Direction.EAST : Direction.WEST);
    }

    private static int place(ChunkAccess chunk, int x, int y, int z, BlockState state)
    {
        ChunkPos pos = chunk.getPos();
        if (x < pos.getMinBlockX() || x > pos.getMaxBlockX()
                || z < pos.getMinBlockZ() || z > pos.getMaxBlockZ())
            return 0;
        BlockPos blockPos = new BlockPos(x, y, z);
        chunk.setBlockState(blockPos, state, false);
        // ProtoChunk placement does not create block entities automatically.
        if (state.is(ModBlocks.SHIP_DOOR.get()))
            chunk.setBlockEntity(new ShipDoorBlockEntity(blockPos, state));
        else if (state.getBlock() == ModBlocks.SHIP_CRATE.get())
            chunk.setBlockEntity(new ShipCrateBlockEntity(blockPos, state));
        else if (state.getBlock() == ModBlocks.FUEL_CONTROLLER.get())
            chunk.setBlockEntity(new FuelControllerBlockEntity(blockPos, state));
        else if (state.getBlock() == ModBlocks.TITANIUM_ALLOY_FURNACE.get())
            chunk.setBlockEntity(new AlloyFurnaceBlockEntity(blockPos, state));
        else if (state.getBlock() == ModBlocks.VOXEL_PRINTING_STATION.get())
            chunk.setBlockEntity(new com.starboundmc.block.entity.VoxelPrintingStationBlockEntity(blockPos, state));
        return 1;
    }
}
