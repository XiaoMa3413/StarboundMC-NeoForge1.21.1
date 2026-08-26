package com.starboundmc.client.starmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapWarpIndicatorTest {
    @Test
    void pulseRemainsReadableAndReachesBothExtremes() {
        float minimum = Float.POSITIVE_INFINITY;
        float maximum = Float.NEGATIVE_INFINITY;
        for (int i = 0; i <= 2_000; i++) {
            double renderClock = i / 20.0D;
            float intensity = StarmapWarpIndicator.pulseIntensity(renderClock);
            int color = StarmapWarpIndicator.pulseColor(renderClock, 0xFF63E2DF);
            minimum = Math.min(minimum, intensity);
            maximum = Math.max(maximum, intensity);
            assertTrue(intensity >= 0.55F && intensity <= 1.0F,
                    () -> "intensity outside pulse bounds: " + intensity);
            assertEquals(0x0063E2DF, color & 0x00FFFFFF);
            assertTrue((color >>> 24) >= 140 && (color >>> 24) <= 255);
        }

        assertEquals(0.55F, minimum, 0.001F);
        assertEquals(1.0F, maximum, 0.001F);
    }
}
