package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CelestialDepthTest {
    @Test void distancesRemainOrderedAcrossNavigationScales() {
        double previous = -1;
        for (double distance : new double[] {0, .05, 1, 50, 280, 10000, 1e6, 1e9, 1e12}) {
            double depth = CelestialDepth.encode(distance);
            assertTrue(depth > previous && depth < 1);
            assertEquals(distance, CelestialDepth.decode(depth), Math.max(1e-10, distance * 1e-12));
            previous = depth;
        }
    }

    @Test void clearDepthIsReservedAndInputsAreValidated() {
        assertTrue(CelestialDepth.encode(1e16) < 1);
        assertThrows(IllegalArgumentException.class, () -> CelestialDepth.encode(-1));
        assertThrows(IllegalArgumentException.class, () -> CelestialDepth.encode(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> CelestialDepth.decode(1.01));
    }
}
