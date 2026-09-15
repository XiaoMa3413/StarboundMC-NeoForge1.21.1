package com.starboundmc.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Authorable layouts for the rocky moon's dead mining outposts.
 *
 * <p>The moon is described as the gas giant's mining colony, but nothing on its
 * surface said so. These are the structures that carry the setting: a beacon
 * pad at the teleporter landing site, abandoned habitation shacks, and fuel
 * crystal outcrops with the scaffold the miners left over them.
 *
 * <p>Layouts are pure data in structure-local coordinates so they can be unit
 * tested without a Minecraft bootstrap. {@link #forChunk(int, int)} is a
 * deterministic hash lookup, so both sides of a chunk border agree and the
 * generator's worker threads share no state. Every structure fits inside its
 * own 16x16 chunk, which keeps the write inside the generating chunk's region.
 *
 * <p><b>Airless-moon rules.</b> The moon has no atmosphere, which forbids two
 * things this file deliberately does not do:
 * <ul>
 *   <li><b>No combustion.</b> Torches and lanterns are open flame and need an
 *       oxidizer, so no flame block appears in any layout. Light comes from the
 *       mod's own {@code CRYSTAL} (self-luminous fuel crystal, the very thing
 *       the outpost mined) and from the salvaged ship engine.</li>
 *   <li><b>Nothing may float.</b> Blocks without a {@code canSurvive} check
 *       (chains, iron bars) happily hang in mid-air forever, so every bar/post
 *       is placed over solid ground and no chain is used at all.</li>
 * </ul>
 */
public final class RockyMoonOutposts
{
    /**
     * Block kinds; the chunk generator resolves these to actual block states.
     * There is intentionally no flame kind (see the airless-moon rules above).
     */
    public enum Kind
    {
        IRON, BRICK, CRACKED, COBBLE, DEEPSLATE, GLASS_PANE,
        IRON_BARS, ANVIL, CAULDRON, CRAFTING, CRYSTAL, ENGINE
    }

    /** One block at a structure-local offset; y is relative to the pad surface. */
    public record Block(int x, int y, int z, Kind kind)
    {
    }

    /**
     * A complete outpost layout. {@code halfX}/{@code halfZ} bound the footprint
     * that has to be levelled, and {@code clearHeight} is how far above the pad
     * the air carve must run before the blocks are stamped.
     */
    public record Structure(String id, int halfX, int halfZ, int clearHeight, List<Block> blocks)
    {
        public Structure
        {
            blocks = List.copyOf(blocks);
        }
    }

    /** Largest footprint half-extent any layout may request. */
    public static final int MAX_HALF_EXTENT = 5;
    /** Chunk-local jitter applied to a structure centre (keeps it inside the chunk). */
    public static final int MAX_CENTER_JITTER = 2;

    private static final double OUTCROP_CHANCE = 0.16;
    private static final double CAMP_CHANCE = 0.09;

    private RockyMoonOutposts()
    {
    }

    /** The beacon pad stamped at the teleporter landing point. */
    public static Structure spawnBeacon()
    {
        List<Block> blocks = new ArrayList<>();
        // Recessed centre cross is part of the pad surface, so the plate loop
        // skips those cells instead of double-placing them.
        for (int x = -4; x <= 4; x++)
        {
            for (int z = -4; z <= 4; z++)
            {
                if (Math.abs(x) == 4 && Math.abs(z) == 4)
                    continue;
                if (isBeaconCross(x, z))
                    continue;
                blocks.add(new Block(x, 0, z, Kind.IRON));
            }
        }
        for (int i = -2; i <= 2; i++)
        {
            blocks.add(new Block(i, 0, 0, Kind.DEEPSLATE));
            if (i != 0)
                blocks.add(new Block(0, 0, i, Kind.DEEPSLATE));
        }
        // Corner marker posts topped with a glimmering fuel crystal. Two reasons
        // this is not a lantern: there is no oxygen to burn one, and the outpost
        // mined exactly these crystals, so crystal markers are what the crew
        // would actually have built.
        for (int sx : new int[] { -3, 3 })
        {
            for (int sz : new int[] { -3, 3 })
            {
                blocks.add(new Block(sx, 1, sz, Kind.IRON));
                blocks.add(new Block(sx, 2, sz, Kind.CRYSTAL));
            }
        }
        // Central beacon mast with a scavenged ship engine as the emitter.
        // Deliberately off the centre cell: the teleporter lands on the pad's
        // recessed cross at (0, 0), and findSurfaceSpawn scans that exact column
        // first, so a mast there would drop the player on top of the engine.
        blocks.add(new Block(2, 1, 2, Kind.DEEPSLATE));
        blocks.add(new Block(2, 2, 2, Kind.DEEPSLATE));
        blocks.add(new Block(2, 3, 2, Kind.ENGINE));
        return new Structure("beacon_pad", 4, 4, 5, blocks);
    }

    private static boolean isBeaconCross(int x, int z)
    {
        return (x == 0 && Math.abs(z) <= 2) || (z == 0 && Math.abs(x) <= 2);
    }

    /** Deterministic structure for a non-beacon chunk, or null when it has none. */
    public static Structure forChunk(int chunkX, int chunkZ)
    {
        if (chunkX == 0 && chunkZ == 0)
            return null;
        double roll = unit(hash(chunkX, chunkZ, 0x51ED270B3D1A5E69L));
        if (roll < OUTCROP_CHANCE)
            return crystalOutcrop();
        if (roll < OUTCROP_CHANCE + CAMP_CHANCE)
            return miningCamp();
        return null;
    }

    private static Structure crystalOutcrop()
    {
        List<Block> blocks = new ArrayList<>();
        // Low mound of broken rock; crystals break through the top (y >= 1) so
        // they never double-place the mound's own cells.
        for (int x = -2; x <= 2; x++)
        {
            for (int z = -2; z <= 2; z++)
            {
                if (Math.abs(x) + Math.abs(z) > 2)
                    continue;
                blocks.add(new Block(x, 0, z, Kind.COBBLE));
            }
        }
        blocks.add(new Block(0, 1, 0, Kind.CRYSTAL));
        blocks.add(new Block(1, 1, 0, Kind.CRYSTAL));
        blocks.add(new Block(-1, 1, 0, Kind.CRYSTAL));
        blocks.add(new Block(0, 1, 1, Kind.CRYSTAL));
        blocks.add(new Block(0, 1, -1, Kind.CRYSTAL));
        blocks.add(new Block(0, 2, 0, Kind.CRYSTAL));
        // Claim markers: solid deepslate posts on the mound's rim, each topped
        // by the crystal the claim is for. Everything rests on the mound below
        // it, so nothing can float the way a chain or a lone bar would.
        blocks.add(new Block(2, 1, 0, Kind.DEEPSLATE));
        blocks.add(new Block(2, 2, 0, Kind.DEEPSLATE));
        blocks.add(new Block(2, 3, 0, Kind.CRYSTAL));
        blocks.add(new Block(-2, 1, 0, Kind.DEEPSLATE));
        blocks.add(new Block(-2, 2, 0, Kind.CRYSTAL));
        return new Structure("crystal_outcrop", 2, 2, 4, blocks);
    }

    private static Structure miningCamp()
    {
        List<Block> blocks = new ArrayList<>();
        // 7x7 rock slab floor.
        for (int x = -3; x <= 3; x++)
        {
            for (int z = -3; z <= 3; z++)
            {
                Kind floor = ((x + z) & 1) == 0 ? Kind.COBBLE : Kind.BRICK;
                blocks.add(new Block(x, 0, z, floor));
            }
        }
        // Ruined perimeter wall, two high, with a doorway on +Z and missing
        // blocks along the top so it reads abandoned, not pristine.
        for (int x = -3; x <= 3; x++)
        {
            for (int z = -3; z <= 3; z++)
            {
                if (x > -3 && x < 3 && z > -3 && z < 3)
                    continue;
                if (z == 3 && (x == -1 || x == 0))
                    continue; // doorway
                boolean cracked = unit(hash(x, z, 0x2545F4914F6CDD1DL)) < 0.35;
                blocks.add(new Block(x, 1, z, cracked ? Kind.CRACKED : Kind.COBBLE));
                boolean windowSlit = Math.abs(x) == 3 && z == 0;
                if (!windowSlit && (!cracked || unit(hash(x, z, 0x1B873593L)) < 0.6))
                    blocks.add(new Block(x, 2, z, Kind.COBBLE));
            }
        }
        // Window slits on the side walls.
        blocks.add(new Block(-3, 2, 0, Kind.GLASS_PANE));
        blocks.add(new Block(3, 2, 0, Kind.GLASS_PANE));
        // Roof beams across the span, open to the sky between them. The centre
        // cell is placed once (by the x-beam); the crystal work-light rests on
        // that crossing so it is supported rather than floating.
        for (int x = -3; x <= 3; x++)
            blocks.add(new Block(x, 3, 0, Kind.IRON_BARS));
        for (int z = -3; z <= 3; z++)
            if (z != 0)
                blocks.add(new Block(0, 3, z, Kind.IRON_BARS));
        blocks.add(new Block(0, 4, 0, Kind.CRYSTAL));
        // Work benches the crew left behind. The dark, empty cauldron is
        // deliberate: liquid water cannot last in vacuum, and this one has
        // been dry since the crew left.
        blocks.add(new Block(-2, 1, -2, Kind.CRAFTING));
        blocks.add(new Block(2, 1, -2, Kind.ANVIL));
        blocks.add(new Block(-2, 1, 2, Kind.CAULDRON));
        blocks.add(new Block(2, 1, 2, Kind.CRYSTAL));
        return new Structure("mining_camp", 3, 3, 5, blocks);
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
