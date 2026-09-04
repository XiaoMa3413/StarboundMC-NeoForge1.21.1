package com.starboundmc.client;

import com.starboundmc.client.WarpVisualTiming;
import com.starboundmc.warp.ShipFlightController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetWarpTransitionTest
{
    @Test
    void arrivalPlanetStartsTransparentAndReachesFullOpacityQuickly()
    {
        float threshold = WarpVisualTiming.ARRIVAL_FADE_START;

        assertEquals(0.0F, PlanetRenderer.arrivalPlanetAlpha(threshold), 1.0E-6F);
        assertTrue(PlanetRenderer.arrivalPlanetAlpha(threshold + 0.04F) > 0.0F);
        assertEquals(1.0F, PlanetRenderer.arrivalPlanetAlpha(
                threshold + 0.08F), 1.0E-6F);
    }

    @Test
    void shortRoutesDoNotUseTheLongRoutePlanetCrossfade()
    {
        assertEquals(ShipFlightController.SHORT_ROUTE_TICKS,
                new ShipFlightController(com.starboundmc.world.Planet.LUSH,
                        com.starboundmc.world.Planet.MOLTEN).getTotalTicks());
    }
}
