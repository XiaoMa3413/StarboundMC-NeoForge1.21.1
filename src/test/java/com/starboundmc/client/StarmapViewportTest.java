package com.starboundmc.client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapViewportTest
{
    @ParameterizedTest
    @CsvSource({
            "6,46,250,220,0.50,62,84",
            "10,50,375,330,1.50,182,129",
            "18,70,625,550,2.50,125,110"
    })
    void fixedViewportMatchesLegacyPixelCenterProjection(
            int x, int y, int width, int height, float scale, int baseX, int baseY)
    {
        StarmapViewport viewport = StarmapViewport.fixed(
                StarmapGeometry.BASE_WIDTH, StarmapGeometry.BASE_HEIGHT,
                x, y, width, height, scale);

        assertEquals(x + StarmapGeometry.projectPixelCenter(
                        baseX, width, StarmapGeometry.BASE_WIDTH),
                viewport.projectX(baseX));
        assertEquals(y + StarmapGeometry.projectPixelCenter(
                        baseY, height, StarmapGeometry.BASE_HEIGHT),
                viewport.projectY(baseY));
        assertEquals(Math.max(1, Math.round(13 * scale)), viewport.projectSize(13));
    }

    @ParameterizedTest
    @CsvSource({
            "6,46,125,110,0.50,62,84",
            "10,50,375,330,1.50,182,129",
            "18,70,625,550,2.50,125,110"
    })
    void inverseProjectionReturnsTheAuthoredPixelWithinRoundingPrecision(
            int x, int y, int width, int height, float scale, int baseX, int baseY)
    {
        StarmapViewport viewport = StarmapViewport.fixed(
                StarmapGeometry.BASE_WIDTH, StarmapGeometry.BASE_HEIGHT,
                x, y, width, height, scale);

        double toleranceX = StarmapGeometry.BASE_WIDTH / (2.0D * width) + 0.0001D;
        double toleranceY = StarmapGeometry.BASE_HEIGHT / (2.0D * height) + 0.0001D;
        assertEquals(baseX, viewport.unprojectX(viewport.projectX(baseX)), toleranceX);
        assertEquals(baseY, viewport.unprojectY(viewport.projectY(baseY)), toleranceY);
    }

    @ParameterizedTest
    @CsvSource({ "0,0", "8,46", "100,72" })
    void containmentUsesHalfOpenViewportBounds(int x, int y)
    {
        StarmapViewport viewport = StarmapViewport.fixed(250, 220, x, y, 125, 110, 0.5F);

        assertTrue(viewport.contains(x, y));
        assertTrue(viewport.contains(x + 124.999D, y + 109.999D));
        assertFalse(viewport.contains(x - 0.001D, y));
        assertFalse(viewport.contains(x + 125.0D, y + 10.0D));
        assertFalse(viewport.contains(x + 10.0D, y + 110.0D));
    }
}
