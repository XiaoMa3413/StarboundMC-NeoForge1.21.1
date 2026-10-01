package com.starboundmc.client.space;

/** Ray ordering for disjoint spherical bounds, including an observer inside one bound. */
public final class CelestialTransparencyOrder {
    private CelestialTransparencyOrder() {}

    public static double key(double centreDistanceSquared, double boundRadius) {
        // Along any common ray, entry*exit = distance²-radius². Disjoint positive
        // intervals therefore have the same order as these products. Centre
        // distance alone fails when a small body is behind a large body's limb.
        return centreDistanceSquared-boundRadius*boundRadius;
    }
}
