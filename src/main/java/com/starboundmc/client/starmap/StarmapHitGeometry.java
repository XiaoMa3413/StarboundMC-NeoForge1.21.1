package com.starboundmc.client.starmap;

/** Pure hit-area policy shared by moving LDLib2 celestial nodes. */
final class StarmapHitGeometry {
    private StarmapHitGeometry() {}

    static float radius(StarmapLevel level, boolean star, boolean moon,
                        boolean focusedPlanet, float scale) {
        if (star) {
            return level == StarmapLevel.GALAXY
                    ? Math.max(14.0F, 20.0F * scale)
                    : Math.max(18.0F, 24.0F * scale);
        }
        if (level == StarmapLevel.PLANET) {
            return focusedPlanet
                    ? Math.max(22.0F, 30.0F * scale)
                    : Math.max(10.0F, 14.0F * scale);
        }
        return moon
                ? Math.max(10.0F, 14.0F * scale)
                : Math.max(14.0F, 18.0F * scale);
    }

    static boolean contains(double pointX, double pointY, float centerX, float centerY,
                            float radius) {
        return Math.hypot(pointX - centerX, pointY - centerY) <= radius;
    }
}
