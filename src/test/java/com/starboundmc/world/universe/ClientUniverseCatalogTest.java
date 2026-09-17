package com.starboundmc.world.universe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A4: the client universe is always usable and never stale.
 *
 * <p>The star map reads universe data during resource reload, which happens
 * before any connection exists. So the two properties that matter are "there is
 * always a universe" and "a server's universe does not survive its session".
 * Both are asserted here rather than left to inspection.</p>
 */
class ClientUniverseCatalogTest
{
    @AfterEach
    void restoreBaseline()
    {
        ClientUniverseCatalog.reset();
    }

    @Test
    void catalogIsUsableBeforeAnyServerIsContacted()
    {
        // No refresh has run: this is the state during the main menu and during
        // the first resource reload.
        UniverseCatalog universe = ClientUniverseCatalog.current();
        assertNotNull(universe, "the client must always have a universe");
        assertFalse(universe.isEmpty(), "the baseline must not be empty");
        assertEquals(2, universe.systemCount());
        assertEquals(6, universe.bodyCount());
        assertFalse(ClientUniverseCatalog.isSynced(),
                "nothing has been synced yet, so the catalog is the built-in baseline");
    }

    /**
     * The baseline must carry the authored flight geometry, or a docked ship
     * renders against the wrong planet.
     *
     * <p>Expected values come from {@link NavigableBodyExpectations}: the geometry
     * used to be readable from {@code ShipSpace}, but migration step A5 moved it
     * into the definitions, so comparing against {@code ShipSpace} is no longer
     * possible or meaningful.</p>
     */
    @Test
    void baselineMatchesTheAuthoredUniverseGeometry()
    {
        UniverseCatalog universe = ClientUniverseCatalog.current();

        for (String entryId : NavigableBodyExpectations.entryIds())
        {
            NavigableBodyExpectations.Dock expected = NavigableBodyExpectations.dock(entryId);
            CelestialBodyDefinition body = universe.body(entryId).orElse(null);
            assertNotNull(body, "baseline is missing " + entryId);

            BodyNavigationProfile navigation = body.navigation().orElse(null);
            assertNotNull(navigation, entryId + " lost its navigation profile");
            assertEquals(expected.radius(), navigation.bodyRadius(), 1.0E-9,
                    entryId + " radius");
            assertEquals(expected.yawDock(), navigation.dockYaw(), 1.0E-9,
                    entryId + " dock yaw");
            assertEquals(expected.dockY(), navigation.dockPosition().localY(), 1.0E-9,
                    entryId + " dock Y");
            assertEquals(expected.bodyY(), navigation.bodyPosition().localY(), 1.0E-9,
                    entryId + " body Y");
        }

        // Star geometry is what the free-flight resolver keys off.
        for (var system : ClientUniverseCatalog.baseline().allSystems())
        {
            assertEquals(NavigableBodyExpectations.starPosition(system.systemId()),
                    system.stellarVisual().getVirtualPosition(),
                    system.systemId() + " star position differs");
        }
    }

    @Test
    void aNullRegistryAccessLeavesTheCatalogUntouched()
    {
        UniverseCatalog before = ClientUniverseCatalog.current();
        ClientUniverseCatalog.refreshFrom(null);
        assertSame(before, ClientUniverseCatalog.current(),
                "a null registry access must not replace the catalog");
    }

    @Test
    void installingACustomUniverseReplacesTheBaseline()
    {
        StarSystemDefinition original = BuiltInUniverse.systems().get(0);
        StarSystemDefinition renamed = new StarSystemDefinition("sysCustom",
                original.nameKey(), original.descriptionKey(), original.starTypeKey(),
                original.stellarVisual(), original.galaxyMapPosition(),
                original.navigationCenter(), original.influenceRadius(), original.bodies());

        ClientUniverseCatalog.setForTesting(UniverseCatalog.of(List.of(renamed)));

        assertTrue(ClientUniverseCatalog.current().system("sysCustom").isPresent(),
                "the installed universe should be active");
        assertTrue(ClientUniverseCatalog.current().system("sys1").isEmpty(),
                "replacing, not merging: a datapack that removes a system must remove it");
        assertTrue(ClientUniverseCatalog.isSynced(), "an installed universe counts as synced");
    }

    @Test
    void resetDropsBackToTheBuiltInUniverse()
    {
        ClientUniverseCatalog.setForTesting(UniverseCatalog.of(List.of(
                BuiltInUniverse.systems().get(0))));
        assertEquals(1, ClientUniverseCatalog.current().systemCount());

        ClientUniverseCatalog.reset();

        assertEquals(2, ClientUniverseCatalog.current().systemCount(),
                "reset must restore the built-in universe");
        assertFalse(ClientUniverseCatalog.isSynced(), "reset must clear the synced flag");
        assertEquals("sys1", ClientUniverseCatalog.current().allSystems().get(0).systemId());
    }

    @Test
    void everyLookupTheStarMapNeedsWorksOnTheBaseline()
    {
        UniverseCatalog universe = ClientUniverseCatalog.current();

        assertTrue(universe.system("sys1").isPresent());
        assertTrue(universe.system("sys2").isPresent());
        assertEquals("sys1", universe.systemOfBody("sys1:lush").orElseThrow().systemId());
        assertEquals(6, universe.navigableBodies().size());
        // Five: every flyable body except the gas giant, which is orbit-only.
        assertEquals(5, universe.surfaceBodies().size());
        // Six: the renderer draws exactly the bodies the legacy enum covered.
        assertEquals(6, universe.spaceRenderedBodies().size());
        assertEquals(1, universe.spatialIndex().occupiedSectorCount());

        // Queries the star map performs for parent-child moon placement.
        CelestialBodyDefinition molten = universe.body("sys1:molten").orElseThrow();
        assertEquals(Optional.of("sys1:lush"), molten.parentEntryId());
        assertTrue(universe.body("sys1:gasgiant").orElseThrow().parentEntryId().isEmpty());
    }

    @Test
    void repeatedResetsAreIdempotent()
    {
        ClientUniverseCatalog.reset();
        UniverseCatalog first = ClientUniverseCatalog.current();
        ClientUniverseCatalog.reset();
        assertSame(first, ClientUniverseCatalog.current(),
                "reset should reuse the immutable baseline rather than rebuild it");
    }
}
