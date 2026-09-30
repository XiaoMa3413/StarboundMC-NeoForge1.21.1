package com.starboundmc.client.space;

/** Positive radial universe distance; independent of Minecraft's block depth buffer. */
public final class CelestialDepth {
    public static final double LOG_RANGE = 40;
    public static final double MAX_DISTANCE = Math.scalb(1, 40) - 1;
    private CelestialDepth() {}

    public static double encode(double distance) {
        if (!Double.isFinite(distance) || distance < 0)
            throw new IllegalArgumentException("distance must be finite and non-negative");
        return Math.min(Math.log1p(distance) / Math.log(2) / LOG_RANGE, .999999);
    }

    public static double decode(double depth) {
        if (!Double.isFinite(depth) || depth < 0 || depth > 1)
            throw new IllegalArgumentException("depth must be in [0,1]");
        return Math.expm1(depth * LOG_RANGE * Math.log(2));
    }
}
