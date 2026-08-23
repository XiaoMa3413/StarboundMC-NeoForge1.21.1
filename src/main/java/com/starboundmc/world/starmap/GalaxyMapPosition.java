package com.starboundmc.world.starmap;

/**
 * An art-directed position on the galaxy map, stored independently from the
 * physical navigation coordinates of a star system.
 *
 * <p>Both coordinates are normalized to {@code [0, 1]}. Keeping this value in
 * the system definition lets the UI change resolution or viewport behaviour
 * without changing the authored composition.</p>
 */
public record GalaxyMapPosition(double normalizedX, double normalizedY)
{
    private static final double FALLBACK_MARGIN = 0.14D;
    private static final double FALLBACK_SPAN = 1.0D - FALLBACK_MARGIN * 2.0D;

    public GalaxyMapPosition
    {
        requireNormalized("normalizedX", normalizedX);
        requireNormalized("normalizedY", normalizedY);
    }

    /** Creates a normalized position from the centre of a legacy canvas pixel. */
    public static GalaxyMapPosition fromPixelCenter(int x, int y, int width, int height)
    {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Galaxy map dimensions must be positive");
        if (x < 0 || x >= width || y < 0 || y >= height)
            throw new IllegalArgumentException("Galaxy map pixel must be inside the canvas");
        return new GalaxyMapPosition((x + 0.5D) / width, (y + 0.5D) / height);
    }

    /**
     * Stable fallback for temporary or test systems that have no authored map
     * position yet. The margin keeps their marker and label away from edges.
     */
    public static GalaxyMapPosition fallback(String systemId)
    {
        int hash = systemId == null ? 0 : systemId.hashCode();
        int mixedX = mix(hash ^ 0x68BC21EB);
        int mixedY = mix(hash ^ 0x02E5BE93);
        return new GalaxyMapPosition(
                FALLBACK_MARGIN + toUnitInterval(mixedX) * FALLBACK_SPAN,
                FALLBACK_MARGIN + toUnitInterval(mixedY) * FALLBACK_SPAN);
    }

    /** Converts the normalized position back to a fixed authoring canvas. */
    public int pixelX(int width)
    {
        return pixelCenter(normalizedX, width);
    }

    /** Converts the normalized position back to a fixed authoring canvas. */
    public int pixelY(int height)
    {
        return pixelCenter(normalizedY, height);
    }

    private static int pixelCenter(double normalized, int size)
    {
        if (size <= 0)
            throw new IllegalArgumentException("Galaxy map dimension must be positive");
        return Math.max(0, Math.min(size - 1,
                (int) Math.round(normalized * size - 0.5D)));
    }

    private static void requireNormalized(String name, double value)
    {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D)
            throw new IllegalArgumentException(name + " must be finite and within [0, 1]");
    }

    private static int mix(int value)
    {
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        return value ^ value >>> 16;
    }

    private static double toUnitInterval(int value)
    {
        return Integer.toUnsignedLong(value) / (double) 0xFFFFFFFFL;
    }
}
