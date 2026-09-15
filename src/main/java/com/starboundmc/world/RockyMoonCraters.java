package com.starboundmc.world;

/**
 * Impact-crater field for the rocky moon's regolith.
 *
 * <p>The moon's biomes are named "craters" and "wastes", but the terrain was
 * only retextured noise. This adds the missing landform: a deterministic field
 * of overlapping impact bowls with raised rims and soft ejecta blankets, built
 * on a jittered lattice so the whole surface is scoured without any single
 * crater being tied to a chunk seed.
 *
 * <p>{@link #deformation} is a pure function of world X/Z: the surface height
 * offset in blocks (negative inside a bowl, small positive on a rim). Because
 * it is computed from hashed lattice cells, seams land identically on both
 * sides of a chunk border and no per-chunk state has to be carried across the
 * generator's worker threads.
 *
 * <p>Deliberately free of Minecraft types so bootstrap-free unit tests can
 * exercise the shape directly.
 */
public final class RockyMoonCraters
{
    /** Lattice cell size (blocks): at most one crater is seeded per cell. */
    private static final int CELL = 56;
    /** Probability that a lattice cell carries a crater. */
    private static final double CRATER_CHANCE = 0.62;
    /** Crater radii, in blocks: mostly small, a few broad basins. */
    private static final int RADIUS_MIN = 7;
    private static final int RADIUS_MAX = 26;
    /** Bowl depth, in blocks: proportional to radius but capped. */
    private static final int DEPTH_MIN = 2;
    private static final int DEPTH_MAX = 7;
    /** Rim lifts the edge by this fraction of the bowl depth. */
    private static final double RIM_FRACTION = 0.42;
    /** Rim/ejecta extends to this multiple of the crater radius. */
    private static final double RIM_OUTER = 1.30;
    /** Hard clamp on the summed field so a crater cluster cannot spike. */
    private static final int MIN_DELTA = -9;
    private static final int MAX_DELTA = 4;

    private RockyMoonCraters()
    {
    }

    /** Surface height offset in blocks at world (x, z). */
    public static int deformation(int x, int z)
    {
        int cellX = Math.floorDiv(x, CELL);
        int cellZ = Math.floorDiv(z, CELL);
        double delta = 0.0;
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
                delta += craterAt(cellX + dx, cellZ + dz, x, z);
            }
        }
        int rounded = (int) Math.round(delta);
        return Math.max(MIN_DELTA, Math.min(MAX_DELTA, rounded));
    }

    /**
     * Contribution of the crater seeded in one lattice cell, if any. The centre
     * is jittered inside a reduced cell so neighbouring craters rarely overlap
     * identically; the radius and depth come from independent hashes so a broad
     * basin is not automatically a deep one.
     */
    private static double craterAt(int cellX, int cellZ, int x, int z)
    {
        long seed = hash(cellX, cellZ, 0x9E3779B97F4A7C15L);
        if (unit(seed) > CRATER_CHANCE)
            return 0.0;

        long seedR = hash(cellX, cellZ, 0xC2B2AE3D27D4EB4FL);
        long seedD = hash(cellX, cellZ, 0x165667B19E3779F9L);
        // Jitter keeps the centre at least RIM_OUTER*(radius) from the cell edge
        // is not required: the 3x3 scan above already collects every crater
        // whose influence can reach the sample.
        double centerX = (cellX + 0.5 + (unit(seedR) - 0.5) * 0.7) * CELL;
        double centerZ = (cellZ + 0.5 + (unit(seedD) - 0.5) * 0.7) * CELL;

        double radius = RADIUS_MIN + unit(seedR) * (RADIUS_MAX - RADIUS_MIN);
        double depth = DEPTH_MIN + unit(seedD) * (DEPTH_MAX - DEPTH_MIN);
        // Broad basins stay shallow; small craters stay bowl-shaped.
        depth = Math.min(depth, radius * 0.55);

        double ddx = x - centerX;
        double ddz = z - centerZ;
        double distance = Math.sqrt(ddx * ddx + ddz * ddz);
        double outer = radius * RIM_OUTER;
        if (distance >= outer)
            return 0.0;

        if (distance < radius)
        {
            // Parabolic bowl with a flatter floor near the centre.
            double t = distance / radius;
            double profile = 1.0 - t * t;
            return -depth * profile;
        }

        // Raised rim, tapering into the ejecta blanket.
        double t = (distance - radius) / (outer - radius);
        double rim = depth * RIM_FRACTION * (1.0 - t) * (1.0 - t);
        return rim;
    }

    /** Largest possible bowl depth, for generator clamp checks and tests. */
    public static int maxDepth()
    {
        return -MIN_DELTA;
    }

    /** Whether a column is inside a bowl (used to bias crater floors to stone). */
    public static boolean isBowl(int x, int z)
    {
        return deformation(x, z) < 0;
    }

    /** Avalanche-mixed 64-bit hash, salted per purpose. */
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
