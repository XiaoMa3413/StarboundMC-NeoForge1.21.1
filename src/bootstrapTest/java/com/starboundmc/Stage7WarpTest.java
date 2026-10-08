package com.starboundmc;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.ShipWarpManager;
import java.util.ArrayList;
import java.util.List;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.UniverseTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage7WarpTest {
    @Test
    void everyRouteAdvancesThroughTheExpectedDeterministicPhases() {
        for (String from : UniverseTestSupport.navigableEntryIds()) {
            for (String to : UniverseTestSupport.navigableEntryIds()) {
                if (from == to) {
                    continue;
                }
                ShipFlightController flight = new ShipFlightController(from, to);
                List<FlightPhase> phases = new ArrayList<>();
                phases.add(flight.getPhase());
                while (!flight.isLanded()) {
                    FlightPhase previous = flight.getPhase();
                    flight.tick();
                    if (flight.getPhase() != previous) {
                        phases.add(flight.getPhase());
                    }
                }
                FlightPhase travel = flight.isShortRoute() ? FlightPhase.CRUISE : FlightPhase.HYPERSPACE;
                assertEquals(List.of(FlightPhase.TURN, FlightPhase.ACCELERATE, travel,
                        FlightPhase.DECELERATE, FlightPhase.ARRIVE), phases, from + " -> " + to);
                assertEquals(UniverseNavigation.universeDock(to), flight.getUniversePosition());
                assertEquals(new UniverseDelta(0.0, 0.0, 0.0), flight.getUniverseVelocity());
                assertEquals(0L, flight.getRemainingTicks());
            }
        }
    }

    @Test
    void allSampledRoutesStayOutsideEveryPlanetKeepOutShell() {
        for (String from : UniverseTestSupport.navigableEntryIds()) {
            for (String to : UniverseTestSupport.navigableEntryIds()) {
                if (from == to) {
                    continue;
                }
                ShipFlightController flight = new ShipFlightController(from, to);
                for (int tick = 0; tick <= flight.getTotalTicks(); tick++) {
                    var position = ShipFlightController.sampleUniversePosition(
                            from, to, flight.getTotalTicks(), tick);
                    for (String body : UniverseTestSupport.navigableEntryIds()) {
                        UniverseDelta delta = position.deltaTo(UniverseNavigation.universeBodyPosition(body));
                        double planarDistance = Math.hypot(delta.x(), delta.z());
                        assertTrue(planarDistance + 1.0e-6 >= UniverseNavigation.radius(body) * 1.44,
                                from + " -> " + to + " entered " + body + " shell at tick " + tick);
                    }
                }
            }
        }
    }

    @Test
    void routeDurationsAndFuelCostsPreserveGameplayContract() {
        ShipFlightController local = new ShipFlightController("sys1:lush", "sys1:molten");
        ShipFlightController crossSystem = new ShipFlightController("sys1:lush", "sys2:frozen");
        assertEquals(220, local.getTotalTicks());
        assertTrue(crossSystem.getTotalTicks() >= 360 && crossSystem.getTotalTicks() <= 560);
        assertEquals(20, ShipWarpManager.warpFuelCost("sys1:lush", "sys1:molten"));
        assertEquals(100, ShipWarpManager.warpFuelCost("sys1:lush", "sys2:frozen"));
    }

}
