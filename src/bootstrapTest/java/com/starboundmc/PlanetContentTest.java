package com.starboundmc;

import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.StarSystemDefinition;
import com.starboundmc.world.universe.UniverseTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for the unlocked gas giant / rocky moon content:
 * star-map wiring, virtual-space berth geometry, landing rules and the
 * client-side per-body data tables that would otherwise NPE at runtime.
 *
 * <p>Body identity is an entry id, so these tests ask the universe rather than
 * iterating an enum. The invariants are unchanged: every shipped body must be
 * reachable except the gas giant, which stays orbit-only.</p>
 */
final class PlanetContentTest {
    private static final String GIANT = "sys1:gasgiant";
    private static final String MOON = "sys1:rockymoon";

    @Test
    void gasGiantAndRockyMoonAreReachableWithCorrectThreats() {
        CelestialBodyDefinition giant = UniverseTestSupport.body(GIANT);
        CelestialBodyDefinition moon = UniverseTestSupport.body(MOON);
        assertNotNull(giant);
        assertNotNull(moon);
        assertTrue(giant.isNavigable(), "the gas giant must be flyable to");
        assertTrue(moon.isNavigable(), "the rocky moon must be flyable to");
        assertEquals(8, giant.threatLevel());
        assertEquals(5, moon.threatLevel());
        // The moon stays attached to the giant; the giant remains a primary.
        assertEquals(GIANT, moon.parentEntryId().orElse(null));
        assertTrue(giant.parentEntryId().isEmpty(), "the giant must not orbit anything");
        assertTrue(moon.parentEntryId().isPresent(), "the moon must orbit the giant");
        // No locked placeholders remain in the shipped registry.
        for (var system : UniverseTestSupport.universe().allSystems())
            for (CelestialBodyDefinition entry : system.bodies())
                assertTrue(entry.isNavigable(), "locked body still shipped: " + entry.entryId());
    }

    /**
     * Parent links are what the star map draws moon orbits from, so the pairings the
     * shipped universe relies on are pinned here.
     */
    @Test
    void parentLinksResolveTheMappedPairs() {
        assertEquals(GIANT, parentOf(MOON));
        assertEquals("sys1:lush", parentOf("sys1:molten"));
        assertNull(parentOf(GIANT));
        assertNull(parentOf("sys1:lush"));
        assertNull(parentOf("sys1:barren"));
        assertNull(parentOf("sys2:frozen"));
    }

    /**
     * Every body sharing a system's sky is a companion, which is what the texture
     * preload walks so a primary and its moon do not decode one after the other.
     */
    @Test
    void companionsAreTheOtherDrawableBodiesInTheSameSystem() {
        List<String> lushCompanions = companionIds("sys1:lush");
        assertTrue(lushCompanions.contains("sys1:molten"), "the moon shares its sky");
        assertTrue(lushCompanions.contains(GIANT), "the giant shares the system sky");
        assertTrue(lushCompanions.contains(MOON), "the moon shares the system sky");
        assertFalse(lushCompanions.contains("sys1:lush"), "a body is not its own companion");
        assertFalse(lushCompanions.contains("sys2:frozen"), "another system is not a companion");
    }

    @Test
    void everyBerthPreservesTheApprovedAngularSize() {
        for (String entryId : UniverseTestSupport.navigableEntryIds()) {
            double radius = UniverseNavigation.radius(entryId);
            assertTrue(radius > 0.0);
            assertNotNull(UniverseNavigation.vDock(entryId), entryId + " has no dock");
            assertNotNull(UniverseNavigation.qPos(entryId), entryId + " has no body position");
            // The dock offset is the approved 1.6126 r so the body subtends
            // asin(1/1.6126) ~ 38.4 degrees from its own berth.
            double dockDistance = UniverseNavigation.qPos(entryId)
                    .distanceTo(UniverseNavigation.vDock(entryId));
            assertEquals(1.6126 * radius, dockDistance, 0.05 * radius,
                    entryId + " berth breaks the 38.4-degree contract");
        }
    }

    @Test
    void rockyMoonSitsAtTheCompressedSevenRadiusOrbit() {
        var delta = UniverseNavigation.universeBodyPosition(MOON)
                .deltaTo(UniverseNavigation.universeBodyPosition(GIANT));
        double distance = Math.sqrt(delta.x() * delta.x() + delta.y() * delta.y()
                + delta.z() * delta.z());
        // 7 primary radii (half the old compression) plus a small vertical
        // offset: keeps the giant-moon flight sublight and makes the giant
        // dominate the moon's sky.
        assertEquals(UniverseNavigation.radius(GIANT) * 7.0,
                Math.hypot(delta.x(), delta.z()), 1.0);
        assertTrue(distance > 6.0 * UniverseNavigation.radius(GIANT),
                "moon orbits far outside the rings");
    }

    /**
     * Routes at or under LONG_ROUTE_MIN dock-to-dock run as an 11-second
     * sublight cruise; anything longer enters hyperspace. The giant-moon pair
     * is authored to stay sublight like the lush-molten reference pair.
     */
    @Test
    void giantMoonPairRunsAsASublightRoute() {
        double giantMoon = UniverseNavigation.flightDistance(GIANT, MOON);
        double lushMolten = UniverseNavigation.flightDistance("sys1:lush", "sys1:molten");
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
        for (String entryId : UniverseTestSupport.navigableEntryIds()) {
            StarSystemDefinition system = UniverseTestSupport.system(
                    UniverseTestSupport.systemIdOfEntry(entryId));
            assertNotNull(system, entryId + " has no system");
            double berthDistance = Math.sqrt(UniverseNavigation.universeDock(entryId)
                    .distanceToSqr(system.navigationCenter()));
            assertTrue(berthDistance <= system.planetFieldRadius(),
                    entryId + " berth at " + String.format("%.0f", berthDistance)
                            + " is outside its system's planet field "
                            + String.format("%.0f", system.planetFieldRadius()));
        }
    }

    @Test
    void onlyTheGasGiantRefusesSurfaceLanding() {
        assertFalse(UniverseTestSupport.body(GIANT).isLandable());
        for (String entryId : UniverseTestSupport.navigableEntryIds())
            if (!entryId.equals(GIANT))
                assertTrue(UniverseTestSupport.body(entryId).isLandable(),
                        entryId + " must remain landable");
        assertEquals(8, UniverseTestSupport.body(GIANT).threatLevel());
        assertEquals(5, UniverseTestSupport.body(MOON).threatLevel());
    }

    /**
     * Every navigable body must be drawable, because the renderer reads a body's
     * position and radius from the same profile that makes it navigable. A body
     * with one but not the other would either vanish at its berth or crash the
     * placement code, and neither is visible at compile time.
     */
    @Test
    void everyNavigableBodyIsAlsoDrawable() {
        for (CelestialBodyDefinition body : UniverseTestSupport.universe().navigableBodies()) {
            assertTrue(body.isSpaceRendered(),
                    body.entryId() + " is flyable but not drawable");
            assertTrue(body.spaceVisual().isPresent(),
                    body.entryId() + " is flyable but has no space visual");
        }
    }

    /** Every body drawn in the cockpit window must be placeable. */
    @Test
    void everyDrawnBodyHasFlightGeometry() {
        List<CelestialBodyDefinition> drawn = UniverseTestSupport.universe().spaceRenderedBodies();
        assertFalse(drawn.isEmpty(), "the renderer would draw an empty sky");
        for (CelestialBodyDefinition body : drawn) {
            assertNotNull(UniverseNavigation.universeBodyPosition(body.entryId()),
                    body.entryId() + " is drawn but has no position");
            assertTrue(UniverseNavigation.radius(body.entryId()) > 0.0,
                    body.entryId() + " is drawn but has no radius");
        }
    }

    private static String parentOf(String entryId) {
        return UniverseTestSupport.body(entryId).parentEntryId().orElse(null);
    }

    private static List<String> companionIds(String entryId) {
        return UniverseNavigation.companionBodies(entryId).stream()
                .map(CelestialBodyDefinition::entryId).toList();
    }
}
