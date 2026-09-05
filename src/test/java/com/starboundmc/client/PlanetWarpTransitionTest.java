package com.starboundmc.client;

import com.starboundmc.warp.ShipFlightController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanetWarpTransitionTest
{
    @Test
    void shortRoutesKeepTheirOriginalDuration()
    {
        assertEquals(ShipFlightController.SHORT_ROUTE_TICKS,
                new ShipFlightController(com.starboundmc.world.Planet.LUSH,
                        com.starboundmc.world.Planet.MOLTEN).getTotalTicks());
    }
}
