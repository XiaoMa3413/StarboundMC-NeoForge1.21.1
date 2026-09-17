package com.starboundmc.world.universe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A3, server half: the server owns a universe catalog that is
 * built for the running world and cleared when it stops.
 *
 * <p>The server is the authority for warps, fuel and story gates, so this cannot
 * be a client-only facility. These tests cover the parts reachable without a
 * live {@code MinecraftServer}: the baseline, direct installation, and the
 * clearing contract. The registry-backed build is covered by
 * {@code UniverseRegistryGameTests}, which has a real server to read from.</p>
 */
class ServerUniverseCatalogTest
{
    @AfterEach
    void clearAfterEach()
    {
        ServerUniverseCatalog.clear();
    }

    @Test
    void serverHasAUsableUniverseBeforeAnyWorldLoads()
    {
        ServerUniverseCatalog.clear();
        UniverseCatalog universe = ServerUniverseCatalog.current();
        assertFalse(universe.isEmpty(), "the server baseline must not be empty");
        assertEquals(2, universe.systemCount());
        assertEquals(6, universe.bodyCount());
        assertFalse(ServerUniverseCatalog.isSynced(),
                "no registry has been read yet, so this is the built-in baseline");
    }

    @Test
    void serverBaselineMatchesTheAuthoredNavigationGeometry()
    {
        ServerUniverseCatalog.clear();
        UniverseCatalog universe = ServerUniverseCatalog.current();

        for (String entryId : NavigableBodyExpectations.entryIds())
        {
            NavigableBodyExpectations.Dock expected = NavigableBodyExpectations.dock(entryId);
            BodyNavigationProfile navigation = universe.body(entryId).orElseThrow()
                    .navigation().orElseThrow();
            assertEquals(expected.dockPosition(), navigation.dockPosition(),
                    entryId + " dock differs from the authored dock");
            assertEquals(expected.bodyPosition(), navigation.bodyPosition(),
                    entryId + " body position differs from the authored model");
            assertEquals(expected.radius(), navigation.bodyRadius(), 1.0E-9, entryId);
            assertEquals(expected.yawDock(), navigation.dockYaw(), 1.0E-9, entryId);
        }
    }

    @Test
    void installingAServerUniverseReplacesTheBaselineAndClearingRestoresIt()
    {
        StarSystemDefinition original = BuiltInUniverse.systems().get(0);
        StarSystemDefinition renamed = new StarSystemDefinition("sysServer",
                original.nameKey(), original.descriptionKey(), original.starTypeKey(),
                original.stellarVisual(), original.galaxyMapPosition(),
                original.navigationCenter(), original.influenceRadius(), original.bodies());

        ServerUniverseCatalog.setForTesting(UniverseCatalog.of(List.of(renamed)));
        assertTrue(ServerUniverseCatalog.current().system("sysServer").isPresent(),
                "the installed universe should be active");
        assertTrue(ServerUniverseCatalog.current().system("sys1").isEmpty(),
                "replacing, not merging: a datapack that removes a system must remove it");
        assertTrue(ServerUniverseCatalog.isSynced());

        ServerUniverseCatalog.clear();
        assertEquals(2, ServerUniverseCatalog.current().systemCount(),
                "clearing must restore the built-in universe");
        assertFalse(ServerUniverseCatalog.isSynced(),
                "clearing must drop the synced flag");
    }

    @Test
    void clearingIsIdempotentAndDoesNotRebuildTheBaseline()
    {
        ServerUniverseCatalog.clear();
        UniverseCatalog first = ServerUniverseCatalog.current();
        ServerUniverseCatalog.clear();
        assertSame(first, ServerUniverseCatalog.current(),
                "clearing should reuse the immutable baseline rather than rebuild it");
    }

    @Test
    void aNullServerClearsRatherThanLeavingAStaleUniverse()
    {
        StarSystemDefinition original = BuiltInUniverse.systems().get(0);
        StarSystemDefinition renamed = new StarSystemDefinition("sysStale",
                original.nameKey(), original.descriptionKey(), original.starTypeKey(),
                original.stellarVisual(), original.galaxyMapPosition(),
                original.navigationCenter(), original.influenceRadius(), original.bodies());
        ServerUniverseCatalog.setForTesting(UniverseCatalog.of(List.of(renamed)));

        ServerUniverseCatalog.initialize(null);

        assertTrue(ServerUniverseCatalog.current().system("sysStale").isEmpty(),
                "a null server must not leave the previous universe installed");
        assertEquals(2, ServerUniverseCatalog.current().systemCount());
    }

    /**
     * The two sides are separate authorities. A client adopting a server's
     * registry must not silently redefine what the server thinks the universe is.
     */
    @Test
    void clientAndServerCatalogsAreIndependent()
    {
        StarSystemDefinition original = BuiltInUniverse.systems().get(0);
        StarSystemDefinition clientOnly = new StarSystemDefinition("sysClientOnly",
                original.nameKey(), original.descriptionKey(), original.starTypeKey(),
                original.stellarVisual(), original.galaxyMapPosition(),
                original.navigationCenter(), original.influenceRadius(), original.bodies());

        ServerUniverseCatalog.clear();
        ClientUniverseCatalog.setForTesting(UniverseCatalog.of(List.of(clientOnly)));
        try
        {
            assertTrue(ClientUniverseCatalog.current().system("sysClientOnly").isPresent(),
                    "the client should have adopted its own universe");
            assertTrue(ServerUniverseCatalog.current().system("sysClientOnly").isEmpty(),
                    "the server must not be affected by the client's universe");
            assertEquals(2, ServerUniverseCatalog.current().systemCount());

            // And the reverse: resetting the client leaves the server alone. Both
            // sides then point back at the one shared immutable baseline, which is
            // the intended resting state (see bothSidesShareTheSameImmutableBaseline).
            ClientUniverseCatalog.reset();
            assertEquals(2, ClientUniverseCatalog.current().systemCount());
            assertEquals(2, ServerUniverseCatalog.current().systemCount());
            assertSame(ClientUniverseCatalog.baseline(), ServerUniverseCatalog.baseline());
        }
        finally
        {
            ClientUniverseCatalog.reset();
        }
    }

    @Test
    void bothSidesShareTheSameImmutableBaseline()
    {
        ServerUniverseCatalog.clear();
        ClientUniverseCatalog.reset();
        assertSame(ClientUniverseCatalog.baseline(), ServerUniverseCatalog.baseline(),
                "the shipped universe is one immutable object shared by both sides");
        assertSame(ServerUniverseCatalog.baseline(), ServerUniverseCatalog.current());
        assertSame(ClientUniverseCatalog.baseline(), ClientUniverseCatalog.current());
    }
}
