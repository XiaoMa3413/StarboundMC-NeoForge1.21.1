package com.starboundmc.client;

import com.starboundmc.warp.FlightPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThrusterVisualStateTest {
    @Test void synchronizedHoldAndReconnectCannotLeaveAStaleFlame() {
        ClientPlanetState.resetConnectionState();
        try {
            var origin = com.starboundmc.warp.UniverseNavigation.universeDock("sys1:lush");
            var velocity = new com.starboundmc.space.UniverseDelta(0, 0, 1);
            ClientPlanetState.applyFlightSnapshot(1, 0, FlightPhase.HYPERSPACE, origin,
                    velocity, 0, 0, 0, 200, 400, "sys1:molten", false);
            assertEquals(1, ClientPlanetState.thrusterIntensity());
            ClientPlanetState.applyFlightSnapshot(2, 1, FlightPhase.HYPERSPACE, origin,
                    velocity, 0, 0, 0, 200, 400, "sys1:molten", true);
            assertEquals(0, ClientPlanetState.thrusterIntensity());
            ClientPlanetState.applyFlightSnapshot(1, 0, FlightPhase.HYPERSPACE, origin,
                    velocity, 0, 0, 0, 200, 400, "sys1:molten", false);
            assertEquals(0, ClientPlanetState.thrusterIntensity(), "late snapshot cannot re-light held flight");
        } finally {
            ClientPlanetState.resetConnectionState();
        }
        assertEquals(0, ClientPlanetState.thrusterIntensity());
    }

    @Test void dockingAndCrewHoldAlwaysExtinguishExhaust() {
        for (var phase : FlightPhase.values())
            assertEquals(0, ThrusterVisualState.intensity(phase, 200, 400, true));
        assertEquals(0, ThrusterVisualState.intensity(FlightPhase.DOCKED, 200, 400, false));
        assertEquals(0, ThrusterVisualState.intensity(FlightPhase.HYPERSPACE, Double.NaN, 400, false));
    }

    @Test void ignitionAndShutdownAreContinuousAcrossPhaseBoundaries() {
        assertEquals(0, value(FlightPhase.TURN, 0));
        assertEquals(value(FlightPhase.TURN, 50), value(FlightPhase.ACCELERATE, 50), 1e-5);
        assertEquals(value(FlightPhase.ACCELERATE, 110), value(FlightPhase.HYPERSPACE, 110), 1e-5);
        assertEquals(value(FlightPhase.HYPERSPACE, 290), value(FlightPhase.DECELERATE, 290), 1e-5);
        assertEquals(value(FlightPhase.DECELERATE, 350), value(FlightPhase.ARRIVE, 350), 1e-5);
        assertEquals(0, value(FlightPhase.ARRIVE, 400));
        assertTrue(value(FlightPhase.ACCELERATE, 60) < value(FlightPhase.ACCELERATE, 100));
        assertTrue(value(FlightPhase.ARRIVE, 360) > value(FlightPhase.ARRIVE, 395));
    }

    @Test void shortRoutesAlsoHaveAVisibleCruiseAndACompleteShutdown() {
        assertEquals(1, ThrusterVisualState.intensity(FlightPhase.CRUISE, 109, 220, false));
        assertEquals(.45f, ThrusterVisualState.intensity(FlightPhase.ARRIVE, 170, 220, false), 1e-5);
        assertEquals(0, ThrusterVisualState.intensity(FlightPhase.ARRIVE, 220, 220, false));
        for (var phase : FlightPhase.values()) for (int t = -10; t < 450; t++) {
            float value = value(phase, t);
            assertTrue(value >= 0 && value <= 1);
        }
    }

    private static float value(FlightPhase phase, double elapsed) {
        return ThrusterVisualState.intensity(phase, elapsed, 400, false);
    }
}
