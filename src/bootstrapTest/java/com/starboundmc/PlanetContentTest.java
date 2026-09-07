package com.starboundmc;

import com.starboundmc.client.PlanetRenderer;
import com.starboundmc.warp.ShipSpace;
import com.starboundmc.world.Planet;
import com.starboundmc.world.starmap.PlanetEntry;
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
    void rockyMoonSitsAtTheCompressedFourteenRadiusOrbit() {
        var delta = ShipSpace.universeBodyPosition(Planet.ROCKY_MOON)
                .deltaTo(ShipSpace.universeBodyPosition(Planet.GAS_GIANT));
        double distance = Math.sqrt(delta.x() * delta.x() + delta.y() * delta.y()
                + delta.z() * delta.z());
        // 14 primary radii plus a small vertical offset, same as Lush/Molten.
        assertEquals(ShipSpace.radius(Planet.GAS_GIANT) * 14.0,
                Math.hypot(delta.x(), delta.z()), 1.0);
        assertTrue(distance > 12.0 * ShipSpace.radius(Planet.GAS_GIANT),
                "moon orbits far outside the rings");
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
