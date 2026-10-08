package com.starboundmc.client.space;

/** Infers a stellar radius from the authored reference apparent radius without moving the star. */
public final class StellarProjection {
    public static final float SHELL_DISTANCE = 320;
    public static final float MAX_SKY_RADIUS = 72;
    private StellarProjection() {}

    public static float skyRadius(float referenceSkyRadius, double referenceDistance, double distance) {
        if (!Float.isFinite(referenceSkyRadius) || referenceSkyRadius <= 0
                || !Double.isFinite(referenceDistance) || referenceDistance <= 0
                || !Double.isFinite(distance) || distance < 0)
            throw new IllegalArgumentException("invalid stellar projection");
        double physicalRadius = referenceDistance * referenceSkyRadius / Math.hypot(SHELL_DISTANCE, referenceSkyRadius);
        if (distance <= physicalRadius) return MAX_SKY_RADIUS;
        double projected = SHELL_DISTANCE * physicalRadius / Math.sqrt(distance * distance - physicalRadius * physicalRadius);
        return (float) Math.min(MAX_SKY_RADIUS, projected);
    }
}
