package com.starboundmc.client;

import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;

/** Pure, seek-safe exhaust envelope driven by the synchronized flight clock. */
public final class ThrusterVisualState {
    private ThrusterVisualState() {}

    public static float intensity(FlightPhase phase, double elapsed, int total, boolean held) {
        if (held || phase == null || phase == FlightPhase.DOCKED || total <= 0
                || !Double.isFinite(elapsed) || elapsed <= 0 || elapsed >= total) return 0;
        double departEnd = Math.min(ShipFlightController.DEPART_TICKS,
                total - ShipFlightController.DECEL_TICKS - ShipFlightController.ARRIVE_TICKS);
        double turnEnd = Math.min(ShipFlightController.TURN_TICKS, departEnd);
        double arrivalStart = total - ShipFlightController.ARRIVE_TICKS;
        double decelStart = arrivalStart - ShipFlightController.DECEL_TICKS;
        return switch (phase) {
            case TURN -> .35f * smooth(elapsed / Math.max(1, turnEnd));
            case ACCELERATE -> .35f + .65f * smooth((elapsed - turnEnd) / Math.max(1, departEnd - turnEnd));
            case CRUISE, HYPERSPACE -> 1;
            case DECELERATE -> 1 - .55f * smooth((elapsed - decelStart) / ShipFlightController.DECEL_TICKS);
            case ARRIVE -> .45f * (1 - smooth((elapsed - arrivalStart) / ShipFlightController.ARRIVE_TICKS));
            case DOCKED -> 0;
        };
    }

    private static float smooth(double value) {
        double t = Math.max(0, Math.min(1, value));
        return (float) (t * t * (3 - 2 * t));
    }
}
