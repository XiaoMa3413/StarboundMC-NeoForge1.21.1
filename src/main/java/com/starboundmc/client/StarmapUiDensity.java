package com.starboundmc.client;

/** Selects and converts the star map's internal high-density UI coordinates. */
public final class StarmapUiDensity
{
    public static final int MAX_DENSITY = 2;

    private final int factor;

    private StarmapUiDensity(int factor)
    {
        if (factor < 1 || factor > MAX_DENSITY)
            throw new IllegalArgumentException("UI density must be within [1, 2]");
        this.factor = factor;
    }

    /** Uses 2x only when Minecraft's GUI scale leaves enough physical pixels. */
    public static StarmapUiDensity forGuiScale(double guiScale)
    {
        if (!Double.isFinite(guiScale) || guiScale <= 0.0D)
            throw new IllegalArgumentException("GUI scale must be finite and positive");
        return new StarmapUiDensity(guiScale >= 2.0D ? MAX_DENSITY : 1);
    }

    public int factor()
    {
        return factor;
    }

    public boolean enabled()
    {
        return factor > 1;
    }

    public int virtual(int logicalCoordinate)
    {
        return Math.multiplyExact(logicalCoordinate, factor);
    }

    /**
     * Centre of a logical raster pixel in virtual-pixel space. A projected
     * star-map coordinate identifies a pixel, not the boundary before it.
     */
    public double virtualPixelCenter(int logicalPixelCoordinate)
    {
        return (logicalPixelCoordinate + 0.5D) * factor;
    }

    /** Top-left coordinate for a virtual-size sprite centred on a logical pixel. */
    public int centeredOrigin(int logicalPixelCoordinate, int virtualSize)
    {
        if (virtualSize <= 0)
            throw new IllegalArgumentException("Virtual sprite size must be positive");
        return Math.toIntExact(Math.round(
                virtualPixelCenter(logicalPixelCoordinate) - virtualSize / 2.0D));
    }

    public double virtual(double logicalCoordinate)
    {
        return logicalCoordinate * factor;
    }

    public double logical(double virtualCoordinate)
    {
        return virtualCoordinate / factor;
    }

    public float inverseScale()
    {
        return 1.0F / factor;
    }
}
