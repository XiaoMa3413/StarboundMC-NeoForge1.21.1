package com.starboundmc.client.space;

import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;

/** Pure route/phase gate for the existing hyperspace streak renderer. */
public final class WarpRenderVisibility
{
    private WarpRenderVisibility()
    {
    }

    public static boolean shouldRenderStreaks(FlightPhase phase, int durationTicks)
    {
        return durationTicks > ShipFlightController.SHORT_ROUTE_TICKS
                && (phase == FlightPhase.HYPERSPACE
                    || phase == FlightPhase.DECELERATE
                    || phase == FlightPhase.ACCELERATE);
    }
}
