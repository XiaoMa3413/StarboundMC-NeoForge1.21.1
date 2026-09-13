package com.starboundmc.world;

import java.util.function.IntBinaryOperator;

/**
 * Geometry of the rocky moon's teleporter landing plain: a broad, gently
 * undulating gravel bench around {@link RockyMoonPlanet#DEFAULT_SPAWN}.
 *
 * <p>Two choices keep the plain from reading as an excavated crater. The bench
 * height is the average of the natural noise surface sampled around the
 * landing point (see {@link #computeBenchY}), so the terrain is levelled to
 * its own surroundings instead of sunk below them, and the plain's radii are
 * wobbled by angular noise so the boundary never draws a perfect circle.
 * Terrain then eases to the bench across a wide smoothstep skirt.</p>
 *
 * <p>Deliberately free of Minecraft types so bootstrap-free unit tests can
 * exercise the shape directly.</p>
 */
public final class RockyMoonLandingPlain
{
    /** Flat core radius around the landing point (blocks). */
    public static final int FLAT_RADIUS = 48;
    /** Nominal blend-skirt end; the true edge wobbles around this value. */
    public static final int EDGE_RADIUS = 104;
    /** Plain surface keeps a small stone-speckle ratio so it reads as regolith, not a carpet. */
    private static final double STONE_SPECKLE = 0.14;
    /** Edge wobble bounds relative to the nominal radii. */
    private static final double EDGE_MIN = 0.86;
    private static final double EDGE_MAX = 1.14;

    private RockyMoonLandingPlain()
    {
    }

    /**
     * Bench height for a world: the mean natural noise surface around the
     * landing point. Levelling to the local average is what keeps the plain a
     * flat among hills instead of a pit.
     */
    public static int computeBenchY(IntBinaryOperator naturalHeight)
    {
        int cx = RockyMoonPlanet.DEFAULT_SPAWN.getX();
        int cz = RockyMoonPlanet.DEFAULT_SPAWN.getZ();
        int[][] probes = {{0, 0}, {36, 0}, {-36, 0}, {0, 36}, {0, -36},
                {25, 25}, {-25, -25}, {25, -25}, {-25, 25}};
        long sum = 0;
        for (int[] probe : probes)
            sum += naturalHeight.applyAsInt(cx + probe[0], cz + probe[1]);
        return (int) Math.round(sum / (double) probes.length);
    }

    /** True when the column sits inside the landing plain's wobbled skirt. */
    public static boolean inPlain(int x, int z)
    {
        double dx = x - RockyMoonPlanet.DEFAULT_SPAWN.getX();
        double dz = z - RockyMoonPlanet.DEFAULT_SPAWN.getZ();
        return Math.sqrt(dx * dx + dz * dz) <= EDGE_RADIUS * radiusFactor(dx, dz);
    }

    /**
     * Target surface height for a column: the bench (plus slow undulation) on
     * the flat core, a smoothstep blend across the skirt, and the natural
     * height untouched outside it. {@code naturalY} is the column's current
     * top-solid block, {@code benchY} the world's {@link #computeBenchY}.
     */
    public static int targetY(int x, int z, int naturalY, int benchY)
    {
        double dx = x - RockyMoonPlanet.DEFAULT_SPAWN.getX();
        double dz = z - RockyMoonPlanet.DEFAULT_SPAWN.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double factor = radiusFactor(dx, dz);
        double edge = EDGE_RADIUS * factor;
        if (dist >= edge)
            return naturalY;
        double bench = benchY + undulation(x, z);
        double flat = FLAT_RADIUS * factor;
        if (dist <= flat)
            return (int) Math.round(bench);
        double t = smoothstep((dist - flat) / (edge - flat));
        return (int) Math.round(bench + (naturalY - bench) * t);
    }

    /**
     * Plain surface cover: gravel-dominant with a light stone speckle so it
     * reads as natural regolith instead of a uniform carpet.
     */
    public static boolean surfaceIsGravel(int x, int z)
    {
        return lattice(x, z) >= STONE_SPECKLE;
    }

    /**
     * Angular wobble of the plain radii: sampling smooth lattice noise along
     * the circle keeps the boundary continuous yet never circular, which is
     * what stops the levelled area from reading as a crater rim.
     */
    private static double radiusFactor(double dx, double dz)
    {
        double angle = Math.atan2(dz, dx);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double n = 0.6 * bilinear(cos * 2.0 + 60.0, sin * 2.0 + 60.0)
                + 0.4 * bilinear(cos * 5.0 + 17.0, sin * 5.0 + 17.0);
        return EDGE_MIN + (EDGE_MAX - EDGE_MIN) * n;
    }

    /** Smooth bilinear lattice noise at (possibly fractional) coordinates. */
    private static double bilinear(double x, double y)
    {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double tx = smoothstep(x - x0);
        double ty = smoothstep(y - y0);
        double a = lattice(x0, y0);
        double b = lattice(x0 + 1, y0);
        double c = lattice(x0, y0 + 1);
        double d = lattice(x0 + 1, y0 + 1);
        double top = a + (b - a) * tx;
        double bottom = c + (d - c) * tx;
        return top + (bottom - top) * ty;
    }

    /** Slow (17-block lattice) 0..2 undulation so the bench is flat but not lasered. */
    static double undulation(int x, int z)
    {
        int x0 = Math.floorDiv(x, 17);
        int z0 = Math.floorDiv(z, 17);
        double tx = smoothstep((x - x0 * 17) / 17.0);
        double tz = smoothstep((z - z0 * 17) / 17.0);
        double a = lattice(x0 * 7 + 3, z0 * 7 + 3);
        double b = lattice((x0 + 1) * 7 + 3, z0 * 7 + 3);
        double c = lattice(x0 * 7 + 3, (z0 + 1) * 7 + 3);
        double d = lattice((x0 + 1) * 7 + 3, (z0 + 1) * 7 + 3);
        double top = a + (b - a) * tx;
        double bottom = c + (d - c) * tx;
        return Math.floor((top + (bottom - top) * tz) * 3.0);
    }

    static double smoothstep(double t)
    {
        return t * t * (3.0 - 2.0 * t);
    }

    /** Avalanche-mixed lattice hash in [0, 1), shared with the moon's patch noise. */
    static double lattice(int x, int z)
    {
        long h = (long) x * 0x9E3779B97F4A7C15L ^ (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED55814DL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h & 0xFFFFFFL) / (double) 0x1000000L;
    }
}
