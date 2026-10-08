package com.starboundmc.client.space;

import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpRenderVisibilityTest
{
    @Test
    void shortSublightRoutesNeverShowHyperspaceStreaks()
    {
        assertFalse(WarpRenderVisibility.shouldRenderStreaks(
                FlightPhase.ACCELERATE, ShipFlightController.SHORT_ROUTE_TICKS));
    }

    @Test
    void longRoutesShowStreaksOnlyInExistingWarpPhases()
    {
        int longRouteTicks = ShipFlightController.SHORT_ROUTE_TICKS + 1;
        assertTrue(WarpRenderVisibility.shouldRenderStreaks(FlightPhase.ACCELERATE, longRouteTicks));
        assertTrue(WarpRenderVisibility.shouldRenderStreaks(FlightPhase.HYPERSPACE, longRouteTicks));
        assertTrue(WarpRenderVisibility.shouldRenderStreaks(FlightPhase.DECELERATE, longRouteTicks));
        assertFalse(WarpRenderVisibility.shouldRenderStreaks(FlightPhase.TURN, longRouteTicks));
        assertFalse(WarpRenderVisibility.shouldRenderStreaks(FlightPhase.CRUISE, longRouteTicks));
        assertFalse(WarpRenderVisibility.shouldRenderStreaks(FlightPhase.ARRIVE, longRouteTicks));
        assertFalse(WarpRenderVisibility.shouldRenderStreaks(FlightPhase.DOCKED, longRouteTicks));
    }
}
