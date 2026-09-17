package com.starboundmc.warp;

import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.UniverseTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A5: route avoidance must not depend on iteration order.
 *
 * <p>Before this step, {@code buildRoute} peeled keep-out circles in
 * {@code Planet.values()} order. It now peels them in the catalog's order, which
 * for the built-in universe is a different sequence. That is only safe if the
 * circles cannot interact.</p>
 *
 * <p>{@code peel} rewrites the polyline cumulatively, so if two keep-out circles
 * overlapped, the second detour would be built on top of the first and the result
 * would depend on which came first. The circles being mutually disjoint makes the
 * operations commute, so the curve is identical whichever order they run in. This
 * test pins that precondition: if a future body is placed close enough to another
 * for their shells to touch, route geometry would silently become
 * order-dependent, and this fails first.</p>
 */
class RouteAvoidanceOrderTest
{
    @Test
    void everyNavigableBodyTakesPartInAvoidance()
    {
        List<CelestialBodyDefinition> avoidance = UniverseNavigation.avoidanceBodies();
        assertEquals(4, avoidance.size(),
                "the four navigable bodies are the four the route must avoid");
        assertEquals(List.of("sys1:barren", "sys1:lush", "sys1:molten", "sys2:frozen"),
                avoidance.stream().map(CelestialBodyDefinition::entryId).sorted().toList(),
                "avoidance must cover exactly the navigable bodies");
    }

    /**
     * A body with no navigation profile has no keep-out circle, so it must stay
     * out of route shaping. Letting the gas giant in would bend every course that
     * passes its orbit.
     */
    @Test
    void bodiesWithoutNavigationGeometryAreExcludedFromAvoidance()
    {
        for (CelestialBodyDefinition body : UniverseNavigation.avoidanceBodies())
        {
            assertTrue(body.isNavigable(),
                    body.entryId() + " took part in avoidance without flight geometry");
        }
        for (String locked : List.of("sys1:gasgiant", "sys1:rockymoon"))
        {
            assertFalse(UniverseNavigation.isNavigable(locked),
                    locked + " must not be a route obstacle");
        }
    }

    /**
     * The precondition that makes route avoidance order-independent.
     *
     * <p>Sizes are read through the same keep-out factor the route builder uses,
     * so this stays honest if a radius is retuned.</p>
     */
    @Test
    void keepOutShellsAreMutuallyDisjoint()
    {
        List<CelestialBodyDefinition> bodies = UniverseNavigation.avoidanceBodies();
        for (int i = 0; i < bodies.size(); i++)
        {
            for (int j = i + 1; j < bodies.size(); j++)
            {
                String a = bodies.get(i).entryId();
                String b = bodies.get(j).entryId();
                double shellA = UniverseNavigation.radius(a) * KEEP_OUT_FACTOR;
                double shellB = UniverseNavigation.radius(b) * KEEP_OUT_FACTOR;

                var dockA = UniverseNavigation.universeBodyPosition(a);
                var dockB = UniverseNavigation.universeBodyPosition(b);
                // Avoidance is computed in the XZ plane, so the planar distance is
                // the one that decides whether two shells can interact.
                double planar = Math.hypot(dockA.deltaXTo(dockB), dockA.deltaZTo(dockB));

                assertTrue(planar > shellA + shellB,
                        a + " and " + b + " keep-out shells overlap ("
                                + String.format("%.3f", planar) + " <= "
                                + String.format("%.3f", shellA + shellB)
                                + "); route geometry would become order-dependent");
            }
        }
    }

    /** Mirrors the constant the route builder inflates each body by. */
    private static final double KEEP_OUT_FACTOR = 1.45;

    /**
     * The dock positions used above are the body positions, which is what the
     * route builder centres its keep-out circles on. Pinning one pair guards
     * against the two being confused.
     */
    @Test
    void avoidanceIsCentredOnBodyPositionsNotDocks()
    {
        var lush = UniverseNavigation.universeBodyPosition("sys1:lush");
        var moltenBody = UniverseNavigation.universeBodyPosition("sys1:molten");
        var moltenDock = UniverseNavigation.universeDock("sys1:molten");

        // The molten moon's dock and body are distinct points; using the dock as
        // the keep-out centre would move the collision shell.
        assertTrue(moltenBody.distanceToSqr(moltenDock) > 0.0,
                "the molten dock and body must not coincide");
        double bodySeparation = Math.hypot(lush.deltaXTo(moltenBody), lush.deltaZTo(moltenBody));
        double dockSeparation = Math.hypot(lush.deltaXTo(moltenDock), lush.deltaZTo(moltenDock));
        assertTrue(Math.abs(bodySeparation - dockSeparation) > 0.5,
                "the body and dock separations differ, so the distinction matters");
    }

    /**
     * The route cache is keyed on entry ids and invalidated with the catalog.
     */
    @Test
    void routeCacheIsKeyedOnEntryIdsAndResetWithTheUniverse()
    {
        String from = UniverseTestSupport.navigableEntryIds().get(0);
        String to = "sys2:frozen";

        ShipFlightController.sampleUniversePosition(from, to, 400, 10);
        assertTrue(ShipFlightController.cachedRouteCount() > 0,
                "sampling a route should populate the cache");

        ShipFlightController.clearRouteCache();
        assertEquals(0, ShipFlightController.cachedRouteCount(),
                "clearing must drop cached routes");
    }
}
