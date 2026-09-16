package com.starboundmc.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Authorable layouts for the rocky moon's abandoned mining outposts.
 *
 * <p>The moon is the gas giant's dead mining claim, and the surface has to say
 * so. These layouts are built from the mod's own industrial hull set
 * ({@code hull_plating}, {@code reinforced_hull}, {@code hull_window},
 * {@code industrial_light}, {@code hull_hazard}, {@code hull_grate}) plus the
 * shipboard devices, so an outpost reads as the same civilisation as the ship
 * rather than a vanilla stone-brick village.</p>
 *
 * <p><b>Airless-moon rules.</b> The moon has no atmosphere, so:</p>
 * <ul>
 *   <li><b>No combustion.</b> No torch, lantern or campfire appears in any
 *       layout. Light is electric ({@code industrial_light}) or comes from the
 *       mined fuel crystal, never from a flame that would have no oxidizer.</li>
 *   <li><b>Habitable volumes are sealed.</b> A pressurised module is a closed
 *       hull box - floor, four walls and a ceiling - entered through the ship's
 *       own {@code ship_door} airlock. A breach is authored explicitly as a
 *       missing wall block, so "ruined" reads as why the crew left.</li>
 *   <li><b>A structure is a connected assembly.</b> Every block is reachable
 *       from the pad through face-adjacent neighbours of the same layout, so an
 *       outpost never leaves an island hanging in the air. Roofs and mast braces
 *       legitimately span gaps, so the rule is connectivity, not "rests on the
 *       block below". Layouts never use chains or bare bars over open air, which
 *       vanilla would let hang forever.</li>
 * </ul>
 *
 * <p>Layouts are pure data in structure-local coordinates so they can be unit
 * tested without a Minecraft bootstrap. Every layout fits inside a single
 * 16x16 chunk, which keeps the worldgen write inside the generating chunk's own
 * region. The landing site carries a fixed beacon plus one habitat module
 * ({@link #clusterAt(int, int)}); everything else is a rare find scattered by a
 * coarse lattice ({@link #forChunk(int, int)}, {@link #WILDERNESS_CELL}).</p>
 */
public final class RockyMoonOutposts
{
    /**
     * Block kinds; the chunk generator resolves these to actual block states.
     * There is intentionally no flame kind (see the airless-moon rules above).
     */
    public enum Kind
    {
        HULL, REINFORCED, WINDOW, LIGHT, HAZARD, GRATE,
        DOOR, CRATE, CRATE_LOOT, FURNACE, REFINERY, BEACON, CRYSTAL
    }

    /** One block at a structure-local offset; y is relative to the pad surface. */
    public record Block(int x, int y, int z, Kind kind)
    {
    }

    /** One stack pre-loaded into the layout's loot crate. */
    public record Loot(int slot, String item, int count)
    {
    }

    /**
     * A complete layout. {@code halfX}/{@code halfZ} bound the footprint that
     * has to be levelled, and {@code clearHeight} is how far above the pad the
     * air carve must run before the blocks are stamped.
     */
    public record Structure(String id, int halfX, int halfZ, int clearHeight,
                            List<Block> blocks, List<Loot> loot)
    {
        public Structure
        {
            blocks = List.copyOf(blocks);
            loot = List.copyOf(loot);
        }

        /** Whether this layout belongs to the authored group near the landing site. */
        public boolean isCluster()
        {
            return CLUSTER_IDS.contains(id);
        }
    }

    /** Largest footprint half-extent any layout may request. */
    public static final int MAX_HALF_EXTENT = 4;
    /** Chunk-local jitter applied to a structure centre (keeps it inside the chunk). */
    public static final int MAX_CENTER_JITTER = 2;

    /**
     * The authored outpost group, as (chunkOffsetX, chunkOffsetZ, structureId).
     * One module only: the claim is dead, so the landing site is a beacon pad
     * with the crew's habitat beside it and nothing else. The offset sits inside
     * the landing plain's flat core (well within {@code FLAT_RADIUS}), so the
     * module stands on ground the plain already levelled rather than cutting a
     * step into the skirt. Everything else the claim once had is now a rare
     * wilderness find - see {@link #WILDERNESS_TYPES}.
     */
    private static final Object[][] CLUSTER_PLOTS = {
            {2, 0, "habitat_module"},
    };
    private static final List<String> CLUSTER_IDS = List.of("habitat_module");

    /**
     * Wilderness spawn lattice, in blocks. The moon is meant to read as empty,
     * so a structure is placed per lattice <em>cell</em>, not per chunk: at most
     * one per {@value} blocks square, which is 16x16 chunks.
     *
     * <p>Cell-based placement is what makes the emptiness hold. The previous
     * per-chunk roll put one structure in a fifth of all chunks, and because
     * adjacent chunks were decided independently the distance between two
     * neighbouring finds worked out to about a chunk - no matter how low the
     * probability was set. Locking a cell's single candidate to the middle of
     * its cell instead guarantees that candidates in neighbouring cells are at
     * least {@code WILDERNESS_CELL / 16 - 2*WILDERNESS_CENTER_JITTER} chunks
     * apart, so finds can never clump.</p>
     */
    public static final int WILDERNESS_CELL = 256;

    /**
     * How far a cell's candidate may slide off the cell centre, in chunks. Small
     * enough that neighbouring cells' candidates can never meet.
     */
    private static final int WILDERNESS_CENTER_JITTER = 2;

    /** Chance that a lattice cell carries any wilderness structure at all. */
    private static final double CELL_CHANCE = 0.12;

    /**
     * Wilderness structure types and their relative weights. Small claim markers
     * are the common sighting; the intact installations are the rare payoff, and
     * the tall comms tower is deliberately rarest because a mast on the horizon
     * is a landmark, not scenery.
     */
    private static final Object[][] WILDERNESS_TYPES = {
            {"crystal_outcrop", 46.0},
            {"scrapper_camp", 30.0},
            {"supply_cache", 10.0},
            {"processing_plant", 8.0},
            {"comms_tower", 6.0},
    };

    private RockyMoonOutposts()
    {
    }

    /** The beacon pad stamped at the teleporter landing point. */
    public static Structure spawnBeacon()
    {
        List<Block> blocks = new ArrayList<>();
        // Landing field: plating with a hazard-banded rim and a grated cross.
        for (int x = -4; x <= 4; x++)
        {
            for (int z = -4; z <= 4; z++)
            {
                if (Math.abs(x) == 4 && Math.abs(z) == 4)
                    continue;
                boolean rim = Math.abs(x) == 4 || Math.abs(z) == 4;
                boolean cross = (x == 0 && Math.abs(z) <= 2) || (z == 0 && Math.abs(x) <= 2);
                blocks.add(new Block(x, 0, z,
                        rim ? Kind.HAZARD : cross ? Kind.GRATE : Kind.HULL));
            }
        }
        // Corner approach lights on plinths.
        for (int sx : new int[] { -3, 3 })
        {
            for (int sz : new int[] { -3, 3 })
            {
                blocks.add(new Block(sx, 1, sz, Kind.REINFORCED));
                blocks.add(new Block(sx, 2, sz, Kind.LIGHT));
            }
        }
        // Beacon mast with a purpose-built signal head. Deliberately off the
        // centre cell: the teleporter lands on the pad's grated cross at (0, 0)
        // and findSurfaceSpawn scans that exact column first, so a mast there
        // would drop the player on top of the emitter.
        blocks.add(new Block(2, 1, 2, Kind.REINFORCED));
        blocks.add(new Block(2, 2, 2, Kind.REINFORCED));
        blocks.add(new Block(2, 3, 2, Kind.BEACON));
        return new Structure("beacon_pad", 4, 4, 5, blocks, List.of());
    }

    /**
     * The authored outpost group. Returns null unless the chunk is one of the
     * reserved cluster plots, so the group always lands in the same place
     * relative to the spawn chunk.
     */
    public static Structure clusterAt(int chunkX, int chunkZ)
    {
        for (Object[] plot : CLUSTER_PLOTS)
        {
            if ((int) plot[0] == chunkX && (int) plot[1] == chunkZ)
                return byId((String) plot[2]);
        }
        return null;
    }

    public static Structure byId(String id)
    {
        return switch (id)
        {
            case "beacon_pad" -> spawnBeacon();
            case "habitat_module" -> habitatModule();
            case "processing_plant" -> processingPlant();
            case "comms_tower" -> commsTower();
            case "supply_cache" -> supplyCache();
            case "crystal_outcrop" -> crystalOutcrop();
            case "scrapper_camp" -> scrapperCamp();
            default -> throw new IllegalArgumentException("Unknown outpost: " + id);
        };
    }

    /**
     * Deterministic wilderness structure for a chunk, or null when it has none.
     *
     * <p>A cell nominates exactly one hosting chunk, so this returns a structure
     * for that chunk alone. Placements therefore stay confined to their own
     * chunk - the layout is still stamped entirely inside it - while the spacing
     * between finds is governed by {@link #WILDERNESS_CELL}.</p>
     */
    public static Structure forChunk(int chunkX, int chunkZ)
    {
        if (chunkX == 0 && chunkZ == 0)
            return null;
        if (clusterAt(chunkX, chunkZ) != null)
            return null;
        if (!isWildernessHost(chunkX, chunkZ))
            return null;
        int cellX = Math.floorDiv(chunkX, cellChunks());
        int cellZ = Math.floorDiv(chunkZ, cellChunks());
        if (unit(hash(cellX, cellZ, 0x51ED270B3D1A5E69L)) >= CELL_CHANCE)
            return null;
        return byId(pickWildernessId(cellX, cellZ));
    }

    /** Chunks per lattice cell; {@link #WILDERNESS_CELL} is a multiple of 16. */
    private static int cellChunks()
    {
        return WILDERNESS_CELL / 16;
    }

    /** Whether this chunk is the single candidate its lattice cell nominates. */
    private static boolean isWildernessHost(int chunkX, int chunkZ)
    {
        int cellChunks = cellChunks();
        int cellX = Math.floorDiv(chunkX, cellChunks);
        int cellZ = Math.floorDiv(chunkZ, cellChunks);
        int centerChunkX = cellX * cellChunks + cellChunks / 2;
        int centerChunkZ = cellZ * cellChunks + cellChunks / 2;
        int hostX = centerChunkX + cellJitter(cellX, cellZ, 0x2545F4914F6CDD1DL);
        int hostZ = centerChunkZ + cellJitter(cellX, cellZ, 0x9E3779B97F4A7C15L);
        return chunkX == hostX && chunkZ == hostZ;
    }

    /** Cell-local offset of the nominated chunk, bounded by {@link #WILDERNESS_CENTER_JITTER}. */
    private static int cellJitter(int cellX, int cellZ, long salt)
    {
        double u = unit(hash(cellX, cellZ, salt));
        return (int) Math.round((u - 0.5) * 2.0 * WILDERNESS_CENTER_JITTER);
    }

    /** Weighted pick of the structure type for a cell that carries one. */
    private static String pickWildernessId(int cellX, int cellZ)
    {
        double total = 0.0;
        for (Object[] entry : WILDERNESS_TYPES)
            total += (double) entry[1];
        double roll = unit(hash(cellX, cellZ, 0x14057B7EF767814FL)) * total;
        double cumulative = 0.0;
        for (Object[] entry : WILDERNESS_TYPES)
        {
            cumulative += (double) entry[1];
            if (roll < cumulative)
                return (String) entry[0];
        }
        return (String) WILDERNESS_TYPES[WILDERNESS_TYPES.length - 1][0];
    }

    /**
     * Pressurised habitat module: a sealed hull box with an airlock door, hull
     * windows, ceiling lights and the crew's abandoned kit. A breach in the -X
     * wall is the only hole, which is what killed the claim.
     */
    private static Structure habitatModule()
    {
        List<Block> blocks = new ArrayList<>();
        // Floor and ceiling of a 9x9 module with a 3-high interior. The two
        // ceiling fixtures are chosen here so they replace plating instead of
        // stacking on top of it.
        for (int x = -4; x <= 4; x++)
        {
            for (int z = -4; z <= 4; z++)
            {
                blocks.add(new Block(x, 0, z, Kind.HULL));
                boolean fixture = x % 4 == 2 && z == 0;
                blocks.add(new Block(x, 4, z, fixture ? Kind.LIGHT : Kind.REINFORCED));
            }
        }
        for (int y = 1; y <= 3; y++)
        {
            for (int x = -4; x <= 4; x++)
            {
                for (int z = -4; z <= 4; z++)
                {
                    if (Math.abs(x) != 4 && Math.abs(z) != 4)
                        continue;
                    // Airlock doorway on +Z (the generator expands the door to
                    // the full three blocks).
                    if (z == 4 && x == 0)
                        continue;
                    // The breach that depressurised the module: a hole in the
                    // -X wall, kept off the top layer so the ceiling still rests
                    // on the wall above it.
                    if (x == -4 && y == 2 && (z == -1 || z == -2))
                        continue;
                    boolean window = y == 2
                            && ((Math.abs(x) == 4 && (z == -3 || z == 3))
                                || (Math.abs(z) == 4 && (x == -3 || x == 3)));
                    blocks.add(new Block(x, y, z, window ? Kind.WINDOW : Kind.HULL));
                }
            }
        }
        // Airlock door; the generator expands it to the full 3-block bulkhead.
        blocks.add(new Block(0, 1, 4, Kind.DOOR));
        // The crew's kit, left where it stood.
        blocks.add(new Block(-3, 1, -3, Kind.CRATE_LOOT));
        blocks.add(new Block(3, 1, -3, Kind.FURNACE));
        blocks.add(new Block(-3, 1, 3, Kind.CRATE));
        return new Structure("habitat_module", 4, 4, 7, blocks,
                List.of(new Loot(0, "starboundmc:fuel_crystal", 2),
                        new Loot(1, "starboundmc:tungsten_ingot", 3),
                        new Loot(2, "minecraft:iron_ingot", 4),
                        new Loot(3, "starboundmc:emergency_food_can", 2)));
    }

    /**
     * Ore processing plant: a grated, hazard-banded working floor under an open
     * frame, still holding an intact alloy furnace and voxel refinery.
     */
    private static Structure processingPlant()
    {
        List<Block> blocks = new ArrayList<>();
        for (int x = -4; x <= 4; x++)
        {
            for (int z = -4; z <= 4; z++)
            {
                boolean perimeter = Math.abs(x) == 4 || Math.abs(z) == 4;
                blocks.add(new Block(x, 0, z, perimeter ? Kind.HAZARD : Kind.GRATE));
            }
        }
        // Structural frame: corner and mid-wall posts, an open roof of beams.
        for (int x : new int[] { -4, 0, 4 })
        {
            for (int z : new int[] { -4, 0, 4 })
            {
                if (Math.abs(x) != 4 && Math.abs(z) != 4)
                    continue;
                for (int y = 1; y <= 5; y++)
                    blocks.add(new Block(x, y, z, Kind.REINFORCED));
            }
        }
        // Roof beams bridging the posts, with two cells left for the work
        // lights. Cells already occupied by a post top are skipped so the layout
        // never places the same block twice.
        for (int x = -3; x <= 3; x++)
            blocks.add(new Block(x, 5, 0, Kind.REINFORCED));
        for (int z = -3; z <= 3; z++)
        {
            if (z == 0 || z == -2 || z == 2)
                continue;
            blocks.add(new Block(0, 5, z, Kind.REINFORCED));
        }
        blocks.add(new Block(0, 5, -2, Kind.LIGHT));
        blocks.add(new Block(0, 5, 2, Kind.LIGHT));
        // Working machines: powered down, but intact and usable.
        blocks.add(new Block(-2, 1, -2, Kind.REFINERY));
        blocks.add(new Block(2, 1, -2, Kind.FURNACE));
        blocks.add(new Block(-2, 1, 2, Kind.CRATE_LOOT));
        blocks.add(new Block(2, 1, 2, Kind.CRATE));
        return new Structure("processing_plant", 4, 4, 7, blocks,
                List.of(new Loot(0, "minecraft:raw_iron", 6),
                        new Loot(1, "starboundmc:raw_tungsten", 3)));
    }

    /** Comms tower: a braced mast carrying the outpost's beacon head. */
    private static Structure commsTower()
    {
        List<Block> blocks = new ArrayList<>();
        for (int x = -2; x <= 2; x++)
        {
            for (int z = -2; z <= 2; z++)
            {
                boolean corner = Math.abs(x) == 2 && Math.abs(z) == 2;
                blocks.add(new Block(x, 0, z, corner ? Kind.HAZARD : Kind.GRATE));
            }
        }
        // Four legs with periodic cross-braces and mast lighting.
        for (int y = 1; y <= 11; y++)
        {
            for (int sx : new int[] { -1, 1 })
            {
                for (int sz : new int[] { -1, 1 })
                    blocks.add(new Block(sx, y, sz, Kind.REINFORCED));
            }
            if (y % 4 == 0)
            {
                blocks.add(new Block(0, y, -1, Kind.GRATE));
                blocks.add(new Block(0, y, 1, Kind.GRATE));
                blocks.add(new Block(-1, y, 0, Kind.GRATE));
                blocks.add(new Block(1, y, 0, Kind.GRATE));
                // Mast light in the centre of the brace level, so it is
                // face-adjacent to the braces rather than floating in the core.
                blocks.add(new Block(0, y, 0, Kind.LIGHT));
            }
        }
        // Beacon head: deck, emitter and crystal running lights.
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
                blocks.add(new Block(x, 12, z, Kind.REINFORCED));
        }
        blocks.add(new Block(0, 13, 0, Kind.BEACON));
        blocks.add(new Block(-1, 13, -1, Kind.CRYSTAL));
        blocks.add(new Block(1, 13, 1, Kind.CRYSTAL));
        return new Structure("comms_tower", 2, 2, 15, blocks, List.of());
    }

    /** Supply cache: a small sealed locker hut holding one intact crate. */
    private static Structure supplyCache()
    {
        List<Block> blocks = new ArrayList<>();
        for (int x = -2; x <= 2; x++)
        {
            for (int z = -2; z <= 2; z++)
            {
                blocks.add(new Block(x, 0, z, Kind.HULL));
                // The centre ceiling cell is the fixture, not extra plating.
                boolean fixture = x == 0 && z == 0;
                blocks.add(new Block(x, 4, z, fixture ? Kind.LIGHT : Kind.REINFORCED));
            }
        }
        for (int y = 1; y <= 3; y++)
        {
            for (int x = -2; x <= 2; x++)
            {
                for (int z = -2; z <= 2; z++)
                {
                    if (Math.abs(x) != 2 && Math.abs(z) != 2)
                        continue;
                    if (z == 2 && x == 0)
                        continue; // airlock doorway
                    boolean window = y == 2 && Math.abs(x) == 2 && z == 0;
                    blocks.add(new Block(x, y, z, window ? Kind.WINDOW : Kind.HULL));
                }
            }
        }
        blocks.add(new Block(0, 1, 2, Kind.DOOR));
        blocks.add(new Block(-1, 1, -1, Kind.CRATE_LOOT));
        return new Structure("supply_cache", 2, 2, 6, blocks,
                List.of(new Loot(0, "starboundmc:titanium_ingot", 2),
                        new Loot(1, "minecraft:redstone", 5)));
    }

    /**
     * Wilderness mineral claim: crystal breaking through the regolith with the
     * crew's claim pylon. Deliberately places no fabricated floor or rock mound,
     * so the seam still reads as geology on the natural surface.
     */
    private static Structure crystalOutcrop()
    {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block(0, 1, 0, Kind.CRYSTAL));
        blocks.add(new Block(1, 1, 0, Kind.CRYSTAL));
        blocks.add(new Block(-1, 1, 0, Kind.CRYSTAL));
        blocks.add(new Block(0, 1, 1, Kind.CRYSTAL));
        blocks.add(new Block(0, 1, -1, Kind.CRYSTAL));
        blocks.add(new Block(0, 2, 0, Kind.CRYSTAL));
        // Claim pylon on the seam rim. Everything rests on the levelled ground.
        blocks.add(new Block(2, 1, 0, Kind.REINFORCED));
        blocks.add(new Block(2, 2, 0, Kind.REINFORCED));
        blocks.add(new Block(2, 3, 0, Kind.LIGHT));
        blocks.add(new Block(-2, 1, 0, Kind.REINFORCED));
        blocks.add(new Block(-2, 2, 0, Kind.CRYSTAL));
        return new Structure("crystal_outcrop", 2, 2, 4, blocks, List.of());
    }

    /**
     * Scrapper camp: the stripped remains of a work site. Deliberately not a
     * habitat - a grated floor and three surviving frame posts, no sealed
     * volume and no light, which is what a stripped claim looks like.
     */
    private static Structure scrapperCamp()
    {
        List<Block> blocks = new ArrayList<>();
        for (int x = -3; x <= 3; x++)
        {
            for (int z = -3; z <= 3; z++)
            {
                if (Math.abs(x) + Math.abs(z) > 4)
                    continue;
                blocks.add(new Block(x, 0, z, Kind.GRATE));
            }
        }
        for (int[] post : new int[][] { {-3, -3}, {3, -3}, {3, 3} })
        {
            for (int y = 1; y <= 3; y++)
                blocks.add(new Block(post[0], y, post[1], Kind.REINFORCED));
        }
        // One leaning section of hull and the site's last crate.
        blocks.add(new Block(-3, 1, 3, Kind.HULL));
        blocks.add(new Block(-3, 2, 3, Kind.HULL));
        blocks.add(new Block(1, 1, -3, Kind.CRATE));
        blocks.add(new Block(-1, 1, -1, Kind.HULL));
        return new Structure("scrapper_camp", 3, 3, 5, blocks, List.of());
    }

    /** Chunk-local centre offset, deterministic per structure id. */
    public static int centerJitter(String id, int chunkX, int chunkZ)
    {
        long h = hash(chunkX, chunkZ, id.hashCode() * 0x9E3779B97F4A7C15L);
        return (int) Math.round((unit(h) - 0.5) * 2.0 * MAX_CENTER_JITTER);
    }

    private static long hash(int x, int z, long salt)
    {
        long h = (long) x * 0x9E3779B97F4A7C15L ^ (long) z * 0xC2B2AE3D27D4EB4FL ^ salt;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED55814DL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    private static double unit(long seed)
    {
        return (seed & 0xFFFFFFL) / (double) 0x1000000L;
    }
}
