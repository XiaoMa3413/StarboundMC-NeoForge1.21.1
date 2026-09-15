package com.starboundmc.warp;

import com.starboundmc.world.GasGiantGeometry;
import com.starboundmc.world.Planet;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ship berths at the gas giant sit inside its own ring band, so a purely
 * planar avoidance route grazed the ring plane at |offset| &lt; 0.05 units and the
 * ship visibly ploughed through the annulus. These tests lock the fix in: the
 * ring plane's normal is derived from the same axial tilt the renderer uses,
 * and every route that crosses the disc stays clear of it.
 */
class GasGiantRingClearanceTest
{
    private static final double TILT = Math.toRadians(GasGiantGeometry.AXIAL_TILT_DEGREES);
    /** The ring plane's normal in the shared virtual frame. */
    private static final Vec3 NORMAL = new Vec3(0.0, Math.cos(TILT), Math.sin(TILT));

    @Test
    void ringPlaneNormalMatchesTheRenderersAxialTilt() {
        Vec3 normal = GasGiantGeometry.ringPlaneNormal();
        assertEquals(0.0, normal.x, 1.0e-9);
        assertEquals(Math.cos(TILT), normal.y, 1.0e-9);
        assertEquals(Math.sin(TILT), normal.z, 1.0e-9);
        assertEquals(1.0, normal.length(), 1.0e-9);
        // The band geometry the renderer bakes must be the same numbers.
        assertEquals(1.24F, GasGiantGeometry.RING_INNER_RADII, 1.0e-6);
        assertEquals(2.27F, GasGiantGeometry.RING_OUTER_RADII, 1.0e-6);
    }

    @Test
    void everyGiantRouteClearsTheRingPlane() {
        double radius = ShipSpace.radius(Planet.GAS_GIANT);
        Vec3 center = ShipSpace.qPos(Planet.GAS_GIANT);
        double inner = GasGiantGeometry.ringInnerDistance(radius);
        double outer = GasGiantGeometry.ringOuterDistance(radius);

        for (Planet from : Planet.values())
        {
            for (Planet to : Planet.values())
            {
                if (from == to)
                    continue;
                int total = new ShipFlightController(from, to).getTotalTicks();
                double closestOffset = Double.MAX_VALUE;
                boolean crossed = false;
                for (int tick = 0; tick <= total; tick++)
                {
                    Vec3 p = ShipFlightController.samplePosition(from, to, total, tick);
                    double planar = GasGiantGeometry.planarRadius(p, center);
                    if (planar < inner || planar > outer)
                        continue;
                    crossed = true;
                    closestOffset = Math.min(closestOffset,
                            Math.abs(p.subtract(center).dot(NORMAL)));
                }
                if (!crossed)
                    continue;
                assertTrue(closestOffset >= 0.75,
                        from + " -> " + to + " crosses the ring plane at only "
                                + String.format("%.3f", closestOffset) + " units");
            }
        }
    }

    @Test
    void onlyRoutesTouchingTheGiantCrossItsRings() {
        // The lift must not have dragged any unrelated route into the disc.
        double radius = ShipSpace.radius(Planet.GAS_GIANT);
        Vec3 center = ShipSpace.qPos(Planet.GAS_GIANT);
        double outer = GasGiantGeometry.ringOuterDistance(radius);
        for (Planet from : Planet.values())
        {
            for (Planet to : Planet.values())
            {
                if (from == to || from == Planet.GAS_GIANT || to == Planet.GAS_GIANT)
                    continue;
                int total = new ShipFlightController(from, to).getTotalTicks();
                for (int tick = 0; tick <= total; tick += 7)
                {
                    Vec3 p = ShipFlightController.samplePosition(from, to, total, tick);
                    assertTrue(GasGiantGeometry.planarRadius(p, center) > outer,
                            from + " -> " + to + " unexpectedly entered the ring disc");
                }
            }
        }
    }

    @Test
    void ringLiftNeverMovesTheDockEndpoints() {
        for (Planet[] pair : new Planet[][] {
                {Planet.LUSH, Planet.GAS_GIANT}, {Planet.GAS_GIANT, Planet.LUSH},
                {Planet.FROZEN, Planet.GAS_GIANT}, {Planet.GAS_GIANT, Planet.ROCKY_MOON},
                {Planet.ROCKY_MOON, Planet.GAS_GIANT}}) {
            int total = new ShipFlightController(pair[0], pair[1]).getTotalTicks();
            assertEquals(ShipSpace.universeDock(pair[0]),
                    ShipFlightController.sampleUniversePosition(pair[0], pair[1], total, 0.0));
            assertEquals(ShipSpace.universeDock(pair[1]),
                    ShipFlightController.sampleUniversePosition(pair[0], pair[1], total, total));
        }
    }
}
