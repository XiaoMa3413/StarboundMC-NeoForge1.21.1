package com.starboundmc.warp;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.universe.UniverseTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipFlightControllerUniverseTest
{
    @Test
    void expandedCrossSystemRouteKeepsTheErgonomicDurationCap()
    {
        ShipFlightController controller = new ShipFlightController("sys1:lush", "sys2:frozen");

        assertEquals(ShipFlightController.LONG_ROUTE_MAX_TICKS, controller.getTotalTicks());
    }

    @Test
    void expandedDistanceIsAbsorbedByHyperspaceInsteadOfTheSublightLegs()
    {
        ShipFlightController controller = new ShipFlightController("sys1:lush", "sys2:frozen");
        int total = controller.getTotalTicks();
        int hyperspaceStart = ShipFlightController.TURN_TICKS + ShipFlightController.ACCEL_TICKS;
        int hyperspaceEnd = total - ShipFlightController.DECEL_TICKS - ShipFlightController.ARRIVE_TICKS;
        UniversePosition start = UniverseNavigation.universeDock("sys1:lush");
        UniversePosition target = UniverseNavigation.universeDock("sys2:frozen");
        UniversePosition departureBoundary = ShipFlightController.sampleUniversePosition(
                "sys1:lush", "sys2:frozen", total, hyperspaceStart);
        UniversePosition arrivalBoundary = ShipFlightController.sampleUniversePosition(
                "sys1:lush", "sys2:frozen", total, hyperspaceEnd);

        assertTrue(distance(start, departureBoundary) >= 30.0);
        assertTrue(distance(start, departureBoundary) <= 55.0);
        assertTrue(distance(arrivalBoundary, target) >= 30.0);
        assertTrue(distance(arrivalBoundary, target) <= 55.0);
        assertTrue(distance(departureBoundary, arrivalBoundary) >= 37_000.0,
                "the enlarged interstellar distance belongs inside hyperspace");

        assertTrue(apparentDiameter("sys1:lush", departureBoundary) >= 10.0,
                "the departure planet must remain visually substantial until hyperspace");
        assertTrue(apparentDiameter("sys2:frozen", arrivalBoundary) >= 10.0,
                "the destination planet must already be readable when sublight approach begins");
    }

    @Test
    void speedDoesNotJumpAtTheSublightHyperspaceBoundaries()
    {
        ShipFlightController controller = new ShipFlightController("sys1:lush", "sys2:frozen");
        int total = controller.getTotalTicks();
        int hyperspaceStart = ShipFlightController.TURN_TICKS + ShipFlightController.ACCEL_TICKS;
        int hyperspaceEnd = total - ShipFlightController.DECEL_TICKS - ShipFlightController.ARRIVE_TICKS;

        assertBoundarySpeedContinuity(total, hyperspaceStart);
        assertBoundarySpeedContinuity(total, hyperspaceEnd);
    }

    @Test
    void windowVisibleSublightLegsStayBelowTheComfortSpeedLimit()
    {
        ShipFlightController controller = new ShipFlightController("sys1:lush", "sys2:frozen");
        int total = controller.getTotalTicks();
        int hyperspaceStart = ShipFlightController.TURN_TICKS + ShipFlightController.ACCEL_TICKS;
        int hyperspaceEnd = total - ShipFlightController.DECEL_TICKS - ShipFlightController.ARRIVE_TICKS;

        assertMaxSpeed(total, 0, hyperspaceStart, 25.0);
        assertMaxSpeed(total, hyperspaceEnd, total, 25.0);
    }

    @Test
    void allCurrentRoutesKeepTheirExistingSectorZeroLocalCoordinates()
    {
        for (String from : UniverseTestSupport.navigableEntryIds())
        {
            for (String to : UniverseTestSupport.navigableEntryIds())
            {
                if (from.equals(to))
                    continue;
                ShipFlightController controller = new ShipFlightController(from, to);
                int total = controller.getTotalTicks();

                assertEquals(UniverseNavigation.universeDock(from),
                        ShipFlightController.sampleUniversePosition(from, to, total, 0.0));
                assertEquals(UniverseNavigation.universeDock(to),
                        ShipFlightController.sampleUniversePosition(from, to, total, total));

                for (int tick = 0; tick <= total; tick += 11)
                {
                    UniversePosition universe = ShipFlightController.sampleUniversePosition(from, to, total, tick);
                    assertEquals(SectorCoordinate.ZERO, universe.sector());
                    assertEquals(universe.toLocalVec3(), ShipFlightController.samplePosition(from, to, total, tick));
                }
            }
        }
    }

    @Test
    void tickVelocityUsesUniverseDeltaAndRestoredProgressIsDeterministic()
    {
        ShipFlightController controller = new ShipFlightController("sys1:lush", "sys2:frozen");
        UniversePosition before = controller.getUniversePosition();
        controller.tick();
        UniversePosition after = controller.getUniversePosition();
        UniverseDelta expectedVelocity = before.deltaTo(after).scale(ShipFlightController.TPS);
        assertEquals(expectedVelocity, controller.getUniverseVelocity());

        int elapsed = controller.getTotalTicks() / 2;
        UniversePosition expectedPosition = ShipFlightController.sampleUniversePosition(
                "sys1:lush", "sys2:frozen", controller.getTotalTicks(), elapsed);
        ShipFlightController restored = new ShipFlightController(
                "sys1:lush", "sys2:frozen", expectedPosition, elapsed,
                FlightPhase.HYPERSPACE, 0.0, 0.0, 0.0);

        assertEquals(expectedPosition, restored.getUniversePosition());
        assertEquals(expectedPosition.toLocalVec3(), restored.getPos());
    }

    private static void assertBoundarySpeedContinuity(int total, int boundaryTick)
    {
        UniversePosition before = ShipFlightController.sampleUniversePosition(
                "sys1:lush", "sys2:frozen", total, boundaryTick - 1);
        UniversePosition boundary = ShipFlightController.sampleUniversePosition(
                "sys1:lush", "sys2:frozen", total, boundaryTick);
        UniversePosition after = ShipFlightController.sampleUniversePosition(
                "sys1:lush", "sys2:frozen", total, boundaryTick + 1);
        double incoming = distance(before, boundary);
        double outgoing = distance(boundary, after);

        assertTrue(Math.abs(outgoing - incoming) <= 1.5,
                "phase transition must preserve route speed: " + incoming + " -> " + outgoing);
    }

    private static void assertMaxSpeed(int total, int firstTick, int lastTick,
                                       double maximumUnitsPerSecond)
    {
        UniversePosition previous = ShipFlightController.sampleUniversePosition(
                "sys1:lush", "sys2:frozen", total, firstTick);
        double maximum = 0.0;
        for (int tick = firstTick + 1; tick <= lastTick; tick++)
        {
            UniversePosition current = ShipFlightController.sampleUniversePosition(
                    "sys1:lush", "sys2:frozen", total, tick);
            maximum = Math.max(maximum, distance(previous, current) * ShipFlightController.TPS);
            previous = current;
        }
        assertTrue(maximum <= maximumUnitsPerSecond,
                "window-visible sublight speed was " + maximum + " units/s");
    }

    private static double apparentDiameter(String planet, UniversePosition observer)
    {
        double distance = distance(observer, UniverseNavigation.universeBodyPosition(planet));
        double ratio = Math.min(1.0, UniverseNavigation.radius(planet) / distance);
        return Math.toDegrees(2.0 * Math.asin(ratio));
    }

    private static double distance(UniversePosition from, UniversePosition to)
    {
        return Math.sqrt(from.distanceToSqr(to));
    }
}
