package com.starboundmc.client.starmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StarmapStarfieldCacheTest {
    @Test
    void patternIsDeterministicForTheSameViewport() {
        var first = StarmapStarfieldCache.pattern(800, 500);
        var second = StarmapStarfieldCache.pattern(800, 500);

        assertEquals(first, second);
        assertEquals(StarmapStarfieldCache.starCount(800, 500), first.size());
    }

    @Test
    void densityKeepsABaselineAndScalesWithLargeViewports() {
        assertEquals(120, StarmapStarfieldCache.starCount(250, 220));
        assertEquals(319, StarmapStarfieldCache.starCount(1920, 1080));
    }

    @Test
    void generatedStarsStayInsideTheTextureBounds() {
        int width = 347;
        int height = 211;
        for (StarmapStarfieldCache.Star star : StarmapStarfieldCache.pattern(width, height)) {
            assertTrue(star.x() >= 0 && star.x() < width);
            assertTrue(star.y() >= 0 && star.y() < height);
            assertTrue(star.size() >= 1 && star.size() <= 3);
        }
    }
}
