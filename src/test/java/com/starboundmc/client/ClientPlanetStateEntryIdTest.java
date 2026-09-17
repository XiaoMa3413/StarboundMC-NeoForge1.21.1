package com.starboundmc.client;

import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.BuiltInUniverse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A6: the client identifies the ship's location by entry id.
 *
 * <p>The point of the migration is that an entry id can name a body the legacy
 * enum could not, so these tests push non-legacy ids through the client state and
 * check it still snaps, samples and falls back sensibly.</p>
 */
class ClientPlanetStateEntryIdTest
{
    private static final String LUSH = "sys1:lush";
    private static final String MOLTEN = "sys1:molten";
    private static final String FROZEN = "sys2:frozen";

    @AfterEach
    void resetState()
    {
        ClientPlanetState.resetConnectionState();
        ClientPlanetState.setCurrent(LUSH);
    }

    @Test
    void startsDockedAtTheStarterBodyUsingItsEntryId()
    {
        ClientPlanetState.resetConnectionState();
        assertEquals(BuiltInUniverse.STARTER_BODY_ID, ClientPlanetState.getCurrent(),
                "the client should start at the authored starter body");
        assertEquals(BuiltInUniverse.STARTER_BODY_ID, ClientPlanetState.getCurrentEntryId(),
                "the current entry id must be available before any packet arrives");
        assertFalse(ClientPlanetState.isWarping());
        assertNull(ClientPlanetState.getWarpTarget(), "a docked ship has no warp target");
    }

    /**
     * The starter body must actually exist in the universe, or every fallback path
     * in this class would hand the renderer an id nothing can resolve.
     */
    @Test
    void theStarterBodyIsANavigableBodyInTheUniverse()
    {
        assertTrue(UniverseNavigation.isNavigable(BuiltInUniverse.STARTER_BODY_ID),
                "the starter body id must name a navigable body");
        assertNotNull(UniverseNavigation.body(BuiltInUniverse.STARTER_BODY_ID));
    }

    @Test
    void settingTheCurrentBodySnapsTheDockFromTheCatalog()
    {
        ClientPlanetState.setCurrent(FROZEN);
        assertEquals(FROZEN, ClientPlanetState.getCurrent());

        var snapshot = ClientPlanetState.captureVisualSnapshot();
        assertEquals(UniverseNavigation.universeDock(FROZEN), snapshot.universePosition(),
                "a docked ship must sit exactly at the catalog's dock position");
        assertEquals((float) UniverseNavigation.yawDock(FROZEN), snapshot.yaw(), 1.0E-6F,
                "the dock heading must come from the catalog");
        assertEquals(FROZEN, snapshot.currentBody());
        assertNull(snapshot.targetBody(), "no target while docked");
    }

    @Test
    void startingAWarpUsesTheEntryIdDirectly()
    {
        ClientPlanetState.setCurrent(LUSH);
        ClientPlanetState.startWarp(MOLTEN, 220, MOLTEN);

        assertTrue(ClientPlanetState.isWarping());
        assertEquals(MOLTEN, ClientPlanetState.getWarpTarget());
        assertEquals(MOLTEN, ClientPlanetState.getWarpEntryId());

        var snapshot = ClientPlanetState.captureVisualSnapshot();
        assertTrue(snapshot.warping());
        assertEquals(LUSH, snapshot.currentBody(), "the departure body is unchanged");
        assertEquals(MOLTEN, snapshot.targetBody(), "the target is the entry id itself");

        // The ship must still be at the departure dock on the first frame. The
        // visual clock extrapolates a few microseconds past tick 0, so this is a
        // proximity check rather than an exact one; the curve itself is pinned by
        // ShipFlightControllerUniverseTest.
        var departureDock = UniverseNavigation.universeDock(LUSH);
        double offset = Math.sqrt(snapshot.universePosition().distanceToSqr(departureDock));
        assertTrue(offset < 1.0,
                "a warp must start at the departure dock, but the ship was " + offset + " away");

        // And the route it samples is the one addressed by these two entry ids.
        var routeStart = com.starboundmc.warp.ShipFlightController
                .sampleUniversePosition(LUSH, MOLTEN, 220, 0);
        assertEquals(departureDock, routeStart,
                "tick 0 of the id-addressed curve must be the departure dock");
    }

    @Test
    void aFlightSnapshotAdoptsTheTargetEntryIdFromTheWire()
    {
        ClientPlanetState.setCurrent(LUSH);
        ClientPlanetState.applyFlightSnapshot(1L, 0L, FlightPhase.HYPERSPACE,
                UniverseNavigation.universeDock(FROZEN), new com.starboundmc.space.UniverseDelta(0, 0, 0),
                0.0F, 0.0F, 0.0F, 40, 400, FROZEN);

        assertEquals(FROZEN, ClientPlanetState.getWarpTarget(),
                "the target identity comes from the packet, not from a re-derived enum");
        assertEquals(FROZEN, ClientPlanetState.getWarpEntryId());
    }

    /**
     * A warp target the current universe does not know about must not produce an
     * undefined route. The curve falls back to the current body instead.
     */
    @Test
    void anUnknownWarpTargetDoesNotProduceAnUndefinedRoute()
    {
        ClientPlanetState.setCurrent(LUSH);
        ClientPlanetState.startWarp("othermod:planet_x", 400, "othermod:planet_x");

        // The target is recorded verbatim (the server said so), but sampling stays
        // defined because the fallback resolves to the current body.
        assertEquals("othermod:planet_x", ClientPlanetState.getWarpTarget());
        var snapshot = ClientPlanetState.captureVisualSnapshot();
        assertNotNull(snapshot.universePosition(), "sampling must stay defined");
        assertEquals(UniverseNavigation.universeDock(LUSH), snapshot.universePosition(),
                "an unresolvable target must fall back to the current body's dock");
    }

    @Test
    void settingANullCurrentBodyIsIgnored()
    {
        ClientPlanetState.setCurrent(LUSH);
        ClientPlanetState.setCurrent(null);
        assertEquals(LUSH, ClientPlanetState.getCurrent(),
                "a null update must not wipe the last known location");
    }

    @Test
    void startingANullWarpIsIgnored()
    {
        ClientPlanetState.setCurrent(LUSH);
        ClientPlanetState.startWarp(null, 400, null);
        assertFalse(ClientPlanetState.isWarping(), "a null target must not start a flight");
    }

    /**
     * The star-state packet is authoritative for the star map, and must win over
     * the locally tracked body.
     */
    @Test
    void theSyncedEntryIdIsAuthoritativeForTheStarMap()
    {
        ClientPlanetState.setCurrent(LUSH);
        ClientPlanetState.setStarState(List.of(LUSH, FROZEN), FROZEN);
        assertEquals(FROZEN, ClientPlanetState.getCurrentEntryId(),
                "the synced id must be reported");
        assertTrue(ClientPlanetState.isVisited(FROZEN));
    }

    /**
     * Before the star-state packet lands, the star map still needs an answer.
     */
    @Test
    void theReportedEntryIdFallsBackToTheTrackedBody()
    {
        ClientPlanetState.resetConnectionState();
        ClientPlanetState.setCurrent(MOLTEN);
        // resetConnectionState clears the synced id; setCurrent restores it, so
        // clear it explicitly to exercise the fallback.
        ClientPlanetState.setStarState(List.of(), null);
        assertEquals(MOLTEN, ClientPlanetState.getCurrentEntryId(),
                "with no synced id the tracked body must be reported");
    }
}
