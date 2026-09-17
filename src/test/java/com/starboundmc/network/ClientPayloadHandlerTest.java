package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientPayloadHandlerTest {
    @BeforeEach
    void resetClientState() {
        resetState();
    }

    @AfterEach
    void cleanUpClientState() {
        resetState();
    }

    private static void resetState() {
        ClientNetworkState.resetConnectionState();
        ClientPlanetState.resetConnectionState();
        ClientPlanetState.setCurrent("sys1:lush");
        ClientPlanetState.setFuel(100, 100);
        ClientPlanetState.setStarState(List.of(), null);
    }

    @Test
    void appliesPlanetFuelAndStarPayloadsToRenderingState() {
        ClientPayloadHandler.handle(new SyncFuelPacket(640, 1000), null);
        ClientPayloadHandler.handle(new SyncStarStatePacket(
                List.of("sys1:lush", "sys1:frozen"), "sys1:frozen"), null);

        // The star-state packet is now the only carrier of the body identity: the
        // legacy planet sync packet was removed once the arrival cue moved here.
        assertEquals("sys1:frozen", ClientPlanetState.getCurrentEntryId());
        assertEquals(640, ClientPlanetState.getFuel());
        assertEquals(1000, ClientPlanetState.getMaxFuel());
        assertTrue(ClientPlanetState.isVisited("sys1:frozen"));
    }

    @Test
    void appliesAuthoritativeFlightSnapshotToRenderingState() {
        UniversePosition position = UniversePosition.of(120.5, 102.0, -45.25);
        UniverseDelta velocity = new UniverseDelta(2.0, 0.0, -3.0);
        SyncFlightPacket snapshot = new SyncFlightPacket(
                7L, 200L, FlightPhase.ACCELERATE,
                position, velocity, 30.0, -2.0, 1.0,
                12, 240, null);

        ClientPayloadHandler.handle(snapshot, null);

        assertTrue(ClientPlanetState.isWarping());
        assertEquals(FlightPhase.ACCELERATE, ClientPlanetState.getFlightPhase());
        assertEquals(position, ClientPlanetState.getShipUniversePosition());
        assertEquals(velocity.toVec3(), ClientPlanetState.getShipVelocity());
        assertEquals(240, ClientPlanetState.getWarpDurationTicks());
    }
}
