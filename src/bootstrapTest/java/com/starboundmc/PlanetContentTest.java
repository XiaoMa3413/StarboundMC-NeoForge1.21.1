package com.starboundmc;

import com.starboundmc.client.PlanetRenderer;
import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.ShipSpace;
import com.starboundmc.world.Planet;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for the unlocked gas giant / rocky moon content:
 * star-map wiring, virtual-space berth geometry, landing rules and the
 * client-side per-planet data tables that would otherwise NPE at runtime.
 */
final class PlanetContentTest {
    @Test
    void gasGiantAndRockyMoonAreReachableWithCorrectThreats() {
        PlanetEntry giant = StarSystems.entryById("sys1:gasgiant");
        PlanetEntry moon = StarSystems.entryById("sys1:rockymoon");
        assertNotNull(giant);
        assertNotNull(moon);
        assertEquals(Planet.GAS_GIANT, giant.getDestination());
        assertEquals(Planet.ROCKY_MOON, moon.getDestination());
        assertTrue(giant.isReachable());
        assertTrue(moon.isReachable());
        assertEquals(8, giant.getThreatLevel());
        assertEquals(5, moon.getThreatLevel());
        // The moon stays attached to the giant; the giant remains a primary.
        assertEquals("sys1:gasgiant", moon.getParentEntryId());
        assertFalse(moon.isMoon() == giant.isMoon());
        assertFalse(giant.isMoon());
        // No locked placeholders remain in the shipped registry.
        for (var system : StarSystems.all())
            for (PlanetEntry entry : system.getEntries())
                assertTrue(entry.isReachable(), "locked body still shipped: " + entry.getEntryId());
    }

    @Test
    void companionsResolveThroughParentLinks() {
        assertSame(Planet.ROCKY_MOON, StarSystems.companionOf(Planet.GAS_GIANT));
        assertSame(Planet.GAS_GIANT, StarSystems.companionOf(Planet.ROCKY_MOON));
        assertSame(Planet.MOLTEN, StarSystems.companionOf(Planet.LUSH));
        assertSame(Planet.LUSH, StarSystems.companionOf(Planet.MOLTEN));
        assertEquals(null, StarSystems.companionOf(Planet.BARREN));
        assertEquals(null, StarSystems.companionOf(Planet.FROZEN));
    }

    @Test
    void everyBerthPreservesTheApprovedAngularSize() {
        for (Planet planet : Planet.values()) {
            double radius = ShipSpace.radius(planet);
            assertTrue(radius > 0.0);
            assertNotNull(ShipSpace.vDock(planet), planet + " has no dock");
            assertNotNull(ShipSpace.qPos(planet), planet + " has no body position");
            // The dock offset is the approved 1.6126 r so the body subtends
            // asin(1/1.6126) ≈ 38.4 degrees from its own berth.
            double dockDistance = ShipSpace.qPos(planet).distanceTo(ShipSpace.vDock(planet));
            assertEquals(1.6126 * radius, dockDistance, 0.05 * radius,
                    planet + " berth breaks the 38.4-degree contract");
        }
    }

    @Test
    void rockyMoonSitsAtTheCompressedSevenRadiusOrbit() {
        var delta = ShipSpace.universeBodyPosition(Planet.ROCKY_MOON)
                .deltaTo(ShipSpace.universeBodyPosition(Planet.GAS_GIANT));
        double distance = Math.sqrt(delta.x() * delta.x() + delta.y() * delta.y()
                + delta.z() * delta.z());
        // 7 primary radii (half the old compression) plus a small vertical
        // offset: keeps the giant-moon flight sublight and makes the giant
        // dominate the moon's sky.
        assertEquals(ShipSpace.radius(Planet.GAS_GIANT) * 7.0,
                Math.hypot(delta.x(), delta.z()), 1.0);
        assertTrue(distance > 6.0 * ShipSpace.radius(Planet.GAS_GIANT),
                "moon orbits far outside the rings");
    }

    /**
     * Routes at or under LONG_ROUTE_MIN dock-to-dock run as an 11-second
     * sublight cruise; anything longer enters hyperspace. The giant-moon pair
     * is authored to stay sublight like the lush-molten reference pair.
     */
    @Test
    void giantMoonPairRunsAsASublightRoute() {
        double giantMoon = ShipSpace.flightDistance(Planet.GAS_GIANT, Planet.ROCKY_MOON);
        double lushMolten = ShipSpace.flightDistance(Planet.LUSH, Planet.MOLTEN);
        assertTrue(lushMolten <= ShipFlightController.LONG_ROUTE_MIN,
                "lush-molten reference pair must stay sublight");
        assertTrue(giantMoon <= ShipFlightController.LONG_ROUTE_MIN,
                "giant-moon route must stay sublight, was " + String.format("%.0f", giantMoon));
    }

    /**
     * The ship-sky planet gate culls bodies outside their system's planet
     * field; a berth outside that field renders as the infamous reverse LOD
     * (sphere -> point -> gone) during the approach and vanishes at the berth
     * itself. Every dock must sit inside its own system's field.
     */
    @Test
    void everyBerthSitsInsideItsSystemPlanetField() {
        for (Planet planet : Planet.values()) {
            StarSystem system = StarSystems.systemOfPlanet(planet);
            assertNotNull(system, planet + " has no system");
            double berthDistance = Math.sqrt(ShipSpace.universeDock(planet)
                    .distanceToSqr(system.getUniverseNavigationCenter()));
            assertTrue(berthDistance <= system.getPlanetFieldRadius(),
                    planet + " berth at " + String.format("%.0f", berthDistance)
                            + " is outside its system's planet field "
                            + String.format("%.0f", system.getPlanetFieldRadius()));
        }
    }

    @Test
    void onlyTheGasGiantRefusesSurfaceLanding() {
        assertFalse(Planet.GAS_GIANT.canLand());
        for (Planet planet : Planet.values())
            if (planet != Planet.GAS_GIANT)
                assertTrue(planet.canLand(), planet + " must remain landable");
        assertEquals(8, Planet.GAS_GIANT.threatLevel());
        assertEquals(5, Planet.ROCKY_MOON.threatLevel());
    }

    /**
     * PlanetRenderer keeps per-planet EnumMaps filled in a static block; a
     * missing entry is a runtime NPE with no compile-time protection, so the
     * coverage is asserted here via reflection.
     */
    @Test
    void planetRendererTablesCoverEveryPlanet() throws Exception {
        Set<Planet> all = Set.of(Planet.values());
        for (String name : new String[] { "ATMOSPHERE_COLORS", "ATMOSPHERE_PEAK", "BODY_ORIENTATION",
                "FIXED_SUN_DIRECTIONS", "STELLAR_CORONA_COLORS", "PLANET_TEXTURES" }) {
            Field field = PlanetRenderer.class.getDeclaredField(name);
            field.setAccessible(true);
            Map<?, ?> table = (Map<?, ?>) field.get(null);
            assertEquals(all.size(), table.size(), name + " is missing entries");
            for (Planet planet : Planet.values())
                assertTrue(table.containsKey(planet), name + " misses " + planet);
        }
    }
}
