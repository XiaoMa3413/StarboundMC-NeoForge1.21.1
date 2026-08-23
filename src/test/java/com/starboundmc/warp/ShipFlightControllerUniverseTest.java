package com.starboundmc.warp;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.Planet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipFlightControllerUniverseTest
{
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
}
