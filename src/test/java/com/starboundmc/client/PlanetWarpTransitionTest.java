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
                new ShipFlightController("sys1:lush", "sys1:molten").getTotalTicks());
    }
}
