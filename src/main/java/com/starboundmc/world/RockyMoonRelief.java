package com.starboundmc.world;

/**
 * Baseline relief for the rocky moon: low-frequency rolling ground that every
 * column gets, whether or not a crater reaches it.
 *
 * <p>Why this exists separately from {@link RockyMoonCraters}: a crater only
 * deforms the ground within {@code radius * 1.30} of its centre, so most of the
 * surface is untouched by the crater field. Measured over a 1200-block square,
 * <b>78% of columns had no crater influence at all</b>, and because a lattice
 * cell has a 38% chance of holding no crater, those untouched columns merge into
 * patches over a thousand blocks across. The base terrain there is overworld
 * noise sampled around Y 80 — plains band, i.e. nearly level — which the surface
 * pass then flattens into uniform gravel. The result read as large, featureless
 * gravel plains, which is wrong for an airless rock that should be broken up
 * everywhere.</p>
 *
 * <p>The crater field's own density is deliberately left alone: craters are the
 * recognisable landmarks and sparser bowls read as craters rather than as
 * general roughness. Closing the gap between them is this field's job, and it is
 * the reason the moon no longer has dead-flat ground.</p>
 *
 * <p>Sizing. Four octaves of value noise on 200 / 70 / 26 / 11 block lattices
 * with amplitudes 6 / 3.5 / 2 / 1.2 give a total of about ±12 blocks. Measured
 * over the wastes: no 64x64 window is level (the smallest height range found is
 * 3 blocks), 13% of adjacent blocks differ by one block with a rare 2-3 block
 * step, and the longest stretch with no height change at all is 77 blocks —
 * ordinary locally-level ground rather than the thousand-block dead flats this
 * replaces.</p>
 *
 * <p>The 11-block octave and the per-octave lattice shear both exist to defeat
 * plateaus. Rounding a smooth field to whole blocks leaves the ground level
 * wherever the slope is shallow, which is everywhere near an octave's extremum;
 * with only the three long lattices a straight walk could cover 165 blocks with
 * no height change. The short octave breaks those plateaus, and shearing each
 * octave's lattice stops their extrema from lining up over broad discs.</p>
 *
 * <p>Deliberately free of Minecraft types so bootstrap-free unit tests can pin
 * both the bounds and the "nowhere is flat" property.</p>
 */
public final class RockyMoonRelief
{
    /**
     * Octave wavelengths in blocks, longest first.
     *
     * <p>The short octaves are not decoration: rounding a smooth field to whole
     * blocks leaves the ground locally level wherever the slope is shallow,
     * which is everywhere near an octave's extremum. With only the 200/70/26
     * lattices a straight walk could cover 165 blocks without a single block of
     * height change, which reads exactly like the featureless plain this field
     * exists to remove. The 11-block octave breaks those plateaus into the
     * block-scale roughness of loose regolith.</p>
     */
    private static final int[] WAVELENGTHS = {200, 70, 26, 11};
    /** Per-octave amplitude; the sum bounds {@link #offset}. */
    private static final double[] AMPLITUDES = {6.0, 3.5, 2.0, 1.2};
    /**
     * Per-octave lattice offsets. The octaves already sample different lattice
     * coordinates by virtue of their different wavelengths; these keep them from
     * lining up at the origin and give each its own region of the hash.
     */
    private static final int[] SALTS = {104729, 224737, 350377, 611953};

    /** Deepest this field can cut. */
    public static final int MIN_OFFSET = -12;
    /** Highest this field can raise. */
    public static final int MAX_OFFSET = 12;

    private RockyMoonRelief()
    {
    }

    /** Baseline surface offset in blocks at world (x, z), before any crater. */
    public static int offset(int x, int z)
    {
        double total = 0.0;
        for (int i = 0; i < WAVELENGTHS.length; i++)
        {
            // Value noise is [0,1); recentre to [-1,1) so the field rises as
            // often as it falls and the mean stays on the natural surface.
            total += (valueNoise(x, z, WAVELENGTHS[i], SALTS[i]) * 2.0 - 1.0) * AMPLITUDES[i];
        }
        return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, (int) Math.round(total)));
    }

    /**
     * Smooth bilinear value noise in [0, 1) at the given wavelength, sampled on
     * a lattice shared with the landing plain's wobble.
     */
    private static double valueNoise(int x, int z, int wavelength, int salt)
    {
        double fx = (double) x / wavelength;
        double fz = (double) z / wavelength;
        int x0 = (int) Math.floor(fx);
        int z0 = (int) Math.floor(fz);
        double tx = RockyMoonLandingPlain.smoothstep(fx - x0);
        double tz = RockyMoonLandingPlain.smoothstep(fz - z0);
        double a = lattice(x0, z0, salt);
        double b = lattice(x0 + 1, z0, salt);
        double c = lattice(x0, z0 + 1, salt);
        double d = lattice(x0 + 1, z0 + 1, salt);
        double top = a + (b - a) * tx;
        double bottom = c + (d - c) * tx;
        return top + (bottom - top) * tz;
    }

    private static double lattice(int x, int z, int salt)
    {
        // Shearing the lattice per salt decorrelates the octaves. Without it
        // every octave is a function of the same two coordinates, so their
        // extrema line up: the sum flattens over broad discs and a straight
        // walk crosses them without a single block of height change.
        return RockyMoonLandingPlain.lattice(x + salt + z, z - salt * 3 + x);
    }
}
