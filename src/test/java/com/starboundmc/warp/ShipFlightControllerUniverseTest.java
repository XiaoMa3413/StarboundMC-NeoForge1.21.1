package com.starboundmc.warp;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.Planet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipFlightControllerUniverseTest
{
    @Test
    void expandedCrossSystemRouteKeepsTheErgonomicDurationCap()
    {
        ShipFlightController controller = new ShipFlightController(Planet.LUSH, Planet.FROZEN);

        assertEquals(ShipFlightController.LONG_ROUTE_MAX_TICKS, controller.getTotalTicks());
    }

    @Test
    void expandedDistanceIsAbsorbedByHyperspaceInsteadOfTheSublightLegs()
    {
        ShipFlightController controller = new ShipFlightController(Planet.LUSH, Planet.FROZEN);
        int total = controller.getTotalTicks();
        int hyperspaceStart = ShipFlightController.TURN_TICKS + ShipFlightController.ACCEL_TICKS;
        int hyperspaceEnd = total - ShipFlightController.DECEL_TICKS - ShipFlightController.ARRIVE_TICKS;
        UniversePosition start = ShipSpace.universeDock(Planet.LUSH);
        UniversePosition target = ShipSpace.universeDock(Planet.FROZEN);
        UniversePosition departureBoundary = ShipFlightController.sampleUniversePosition(
                Planet.LUSH, Planet.FROZEN, total, hyperspaceStart);
        UniversePosition arrivalBoundary = ShipFlightController.sampleUniversePosition(
                Planet.LUSH, Planet.FROZEN, total, hyperspaceEnd);

        assertTrue(distance(start, departureBoundary) >= 30.0);
        assertTrue(distance(start, departureBoundary) <= 55.0);
        assertTrue(distance(arrivalBoundary, target) >= 30.0);
        assertTrue(distance(arrivalBoundary, target) <= 55.0);
        assertTrue(distance(departureBoundary, arrivalBoundary) >= 37_000.0,
                "the enlarged interstellar distance belongs inside hyperspace");

        assertTrue(apparentDiameter(Planet.LUSH, departureBoundary) >= 10.0,
                "the departure planet must remain visually substantial until hyperspace");
        assertTrue(apparentDiameter(Planet.FROZEN, arrivalBoundary) >= 10.0,
                "the destination planet must already be readable when sublight approach begins");
    }

    @Test
    void speedDoesNotJumpAtTheSublightHyperspaceBoundaries()
    {
        ShipFlightController controller = new ShipFlightController(Planet.LUSH, Planet.FROZEN);
        int total = controller.getTotalTicks();
        int hyperspaceStart = ShipFlightController.TURN_TICKS + ShipFlightController.ACCEL_TICKS;
        int hyperspaceEnd = total - ShipFlightController.DECEL_TICKS - ShipFlightController.ARRIVE_TICKS;

        assertBoundarySpeedContinuity(total, hyperspaceStart);
        assertBoundarySpeedContinuity(total, hyperspaceEnd);
    }

    @Test
    void windowVisibleSublightLegsStayBelowTheComfortSpeedLimit()
    {
        ShipFlightController controller = new ShipFlightController(Planet.LUSH, Planet.FROZEN);
        int total = controller.getTotalTicks();
        int hyperspaceStart = ShipFlightController.TURN_TICKS + ShipFlightController.ACCEL_TICKS;
        int hyperspaceEnd = total - ShipFlightController.DECEL_TICKS - ShipFlightController.ARRIVE_TICKS;

        assertMaxSpeed(total, 0, hyperspaceStart, 25.0);
        assertMaxSpeed(total, hyperspaceEnd, total, 25.0);
    }

    @Test
    void allCurrentRoutesKeepTheirExistingSectorZeroLocalCoordinates()
    {
        for (Planet from : Planet.values())
        {
            for (Planet to : Planet.values())
            {
                if (from == to)
                    continue;
                ShipFlightController controller = new ShipFlightController(from, to);
                int total = controller.getTotalTicks();

                assertEquals(ShipSpace.universeDock(from),
                        ShipFlightController.sampleUniversePosition(from, to, total, 0.0));
                assertEquals(ShipSpace.universeDock(to),
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
        ShipFlightController controller = new ShipFlightController(Planet.LUSH, Planet.FROZEN);
        UniversePosition before = controller.getUniversePosition();
        controller.tick();
        UniversePosition after = controller.getUniversePosition();
        UniverseDelta expectedVelocity = before.deltaTo(after).scale(ShipFlightController.TPS);
        assertEquals(expectedVelocity, controller.getUniverseVelocity());

        int elapsed = controller.getTotalTicks() / 2;
        UniversePosition expectedPosition = ShipFlightController.sampleUniversePosition(
                Planet.LUSH, Planet.FROZEN, controller.getTotalTicks(), elapsed);
        ShipFlightController restored = new ShipFlightController(
                Planet.LUSH, Planet.FROZEN, expectedPosition, elapsed,
                FlightPhase.HYPERSPACE, 0.0, 0.0, 0.0);

        assertEquals(expectedPosition, restored.getUniversePosition());
        assertEquals(expectedPosition.toLocalVec3(), restored.getPos());
    }

    private static void assertBoundarySpeedContinuity(int total, int boundaryTick)
    {
        UniversePosition before = ShipFlightController.sampleUniversePosition(
                Planet.LUSH, Planet.FROZEN, total, boundaryTick - 1);
        UniversePosition boundary = ShipFlightController.sampleUniversePosition(
                Planet.LUSH, Planet.FROZEN, total, boundaryTick);
        UniversePosition after = ShipFlightController.sampleUniversePosition(
                Planet.LUSH, Planet.FROZEN, total, boundaryTick + 1);
        double incoming = distance(before, boundary);
        double outgoing = distance(boundary, after);

        assertTrue(Math.abs(outgoing - incoming) <= 1.5,
                "phase transition must preserve route speed: " + incoming + " -> " + outgoing);
    }

    private static void assertMaxSpeed(int total, int firstTick, int lastTick,
                                       double maximumUnitsPerSecond)
    {
        UniversePosition previous = ShipFlightController.sampleUniversePosition(
                Planet.LUSH, Planet.FROZEN, total, firstTick);
        double maximum = 0.0;
        for (int tick = firstTick + 1; tick <= lastTick; tick++)
        {
            UniversePosition current = ShipFlightController.sampleUniversePosition(
                    Planet.LUSH, Planet.FROZEN, total, tick);
            maximum = Math.max(maximum, distance(previous, current) * ShipFlightController.TPS);
            previous = current;
        }
        assertTrue(maximum <= maximumUnitsPerSecond,
                "window-visible sublight speed was " + maximum + " units/s");
    }

    private static double apparentDiameter(Planet planet, UniversePosition observer)
    {
        double distance = distance(observer, ShipSpace.universeBodyPosition(planet));
        double ratio = Math.min(1.0, ShipSpace.radius(planet) / distance);
        return Math.toDegrees(2.0 * Math.asin(ratio));
    }

    private static double distance(UniversePosition from, UniversePosition to)
    {
        return Math.sqrt(from.distanceToSqr(to));
    }
}
