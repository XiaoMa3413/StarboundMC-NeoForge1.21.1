package com.starboundmc.client;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.BuiltInUniverse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Current body identity, packet ordering and unknown datapack pose behavior. */
class ClientPlanetStateEntryIdTest {
    @BeforeEach void reset() { ClientPlanetState.resetConnectionState(); }

    @Test void aNewConnectionStartsAtTheAuthoredDockAndClearsVisitedAndTarget() {
        ClientPlanetState.setStarState(List.of("sys2:frozen"), "sys2:frozen");
        ClientPlanetState.startWarp("sys1:molten", 220);
        ClientPlanetState.resetConnectionState();
        assertEquals(BuiltInUniverse.STARTER_BODY_ID, ClientPlanetState.getCurrent());
        assertEquals(UniverseNavigation.universeDock(BuiltInUniverse.STARTER_BODY_ID),
                ClientPlanetState.getShipUniversePosition());
        assertFalse(ClientPlanetState.isVisited("sys2:frozen"));
        assertNull(ClientPlanetState.getWarpTarget());
        assertFalse(ClientPlanetState.isWarping());
    }

    @Test void currentAndVisitedUpdatesShareOneBodyIdentity() {
        ClientPlanetState.setCurrent("sys1:molten");
        ClientPlanetState.setStarState(List.of("sys2:frozen"), "sys2:frozen");
        var snapshot = ClientPlanetState.captureVisualSnapshot();
        assertEquals("sys2:frozen", ClientPlanetState.getCurrent());
        assertEquals(ClientPlanetState.getCurrent(), snapshot.currentBody());
        assertEquals(UniverseNavigation.universeDock("sys2:frozen"), snapshot.universePosition());
        assertTrue(ClientPlanetState.isVisited("sys2:frozen"));
    }

    @Test void aSnapshotSupersedesTheEarlyWarpNotificationTarget() {
        ClientPlanetState.startWarp("sys1:molten", 220);
        snapshot(1, FlightPhase.ACCELERATE, "sys2:frozen", 40, 400);
        assertEquals("sys2:frozen", ClientPlanetState.getWarpTarget());
        assertEquals("sys2:frozen", ClientPlanetState.captureVisualSnapshot().targetBody());
        snapshot(2, FlightPhase.DOCKED, null, 0, 0);
        assertNull(ClientPlanetState.getWarpTarget());
        assertFalse(ClientPlanetState.isWarping());
    }

    @Test void aMatchingNotificationAfterTheSnapshotDoesNotRestartTheFlight() {
        snapshot(1, FlightPhase.ACCELERATE, "sys1:molten", 40, 220);
        ClientPlanetState.startWarp("sys1:molten", 220);
        assertEquals(FlightPhase.ACCELERATE, ClientPlanetState.getFlightPhase());
        assertTrue(ClientPlanetState.warpProgress() >= 40F / 220);
        snapshot(0, FlightPhase.TURN, "sys2:frozen", 0, 400);
        assertEquals("sys1:molten", ClientPlanetState.getWarpTarget());
    }

    @Test void anUnknownDatapackBodyPreservesTheAuthoritativePose() {
        var position = UniversePosition.of(new com.starboundmc.space.SectorCoordinate(7, -9, 2), 123, 456, 789);
        ClientPlanetState.setCurrent("removed:home");
        ClientPlanetState.applyFlightSnapshot(3, 100, FlightPhase.DOCKED, position,
                new UniverseDelta(0, 0, 0), 12, 3, 1, 0, 0, null);
        ClientPlanetState.setStarState(List.of("removed:home"), "removed:home");
        assertEquals("removed:home", ClientPlanetState.getCurrent());
        assertEquals(position, ClientPlanetState.captureVisualSnapshot().universePosition());
    }

    @Test void missingRequiredIdentityIsRejected() {
        assertThrows(NullPointerException.class, () -> ClientPlanetState.setCurrent(null));
        assertThrows(NullPointerException.class, () -> ClientPlanetState.startWarp(null, 400));
    }

    private static void snapshot(long revision, FlightPhase phase, String target, int elapsed, int total) {
        ClientPlanetState.applyFlightSnapshot(revision, 100, phase, UniversePosition.of(123, 102, 456),
                new UniverseDelta(0, 0, 0), 0, 0, 0, elapsed, total, target);
    }
}
