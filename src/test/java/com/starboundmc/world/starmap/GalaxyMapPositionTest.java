package com.starboundmc.world.starmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalaxyMapPositionTest
{
    @Test
    void pixelCenterRoundTripPreservesAuthoredPosition()
    {
        GalaxyMapPosition position = GalaxyMapPosition.fromPixelCenter(182, 129, 250, 220);

        assertEquals(182, position.pixelX(250));
        assertEquals(129, position.pixelY(220));
    }

    @Test
    void fallbackIsStableAndKeptAwayFromCanvasEdges()
    {
        GalaxyMapPosition first = GalaxyMapPosition.fallback("unconfigured-system");
        GalaxyMapPosition repeated = GalaxyMapPosition.fallback("unconfigured-system");
        GalaxyMapPosition other = GalaxyMapPosition.fallback("another-system");

        assertEquals(first, repeated);
        assertNotEquals(first, other);
        assertTrue(first.normalizedX() >= 0.14D && first.normalizedX() <= 0.86D);
        assertTrue(first.normalizedY() >= 0.14D && first.normalizedY() <= 0.86D);
    }
}
