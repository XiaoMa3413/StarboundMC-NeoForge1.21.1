package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.UniverseNavigation;
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

    /**
     * The star-state packet is what tells the client where the ship is, and that
     * location is the departure end of the next flight route.
     *
     * <p>Applying it only to the star-map view left the departure body frozen at the
     * starter world, so a ship docked at any other body began every flight at Lush.
     * The berths are thousands of units apart, so the two candidate docks cannot be
     * confused.</p>
     */
    @Test
    void theRouteDepartsFromTheBodyTheServerReports() {
        ClientPayloadHandler.handle(new SyncStarStatePacket(List.of(), "sys1:barren"), null);

        ClientPayloadHandler.handle(new WarpStartPacket("sys1:lush", 400), null);

        var snapshot = ClientPlanetState.captureVisualSnapshot();
        assertTrue(snapshot.warping(), "the warp should have started");

        double fromOwnBerth = Math.sqrt(snapshot.universePosition()
                .distanceToSqr(UniverseNavigation.universeDock("sys1:barren")));
        double fromStarterBerth = Math.sqrt(snapshot.universePosition()
                .distanceToSqr(UniverseNavigation.universeDock("sys1:lush")));

        assertTrue(fromOwnBerth < 1.0,
                "the flight must begin at the ship's own berth, but started "
                        + fromOwnBerth + " units away from it");
        assertTrue(fromStarterBerth > 100.0,
                "the flight must not begin at the starter body's berth");
    }

    /**
     * The same property for every body the server can report, including the two the
     * legacy planet enum could not name: the ids are opaque here, so nothing but the
     * packet decides where the route starts.
     *
     * <p>Compared against the pure curve at the progress the snapshot itself
     * reports, because the client interpolates against a wall clock and the two
     * reads cannot happen at the same instant.</p>
     */
    @Test
    void anyBodyTheServerReportsCanBeTheDepartureEnd() {
        for (String departure : List.of("sys1:barren", "sys1:molten", "sys1:gasgiant",
                "sys1:rockymoon", "sys2:frozen")) {
            resetState();
            ClientPayloadHandler.handle(new SyncStarStatePacket(List.of(), departure), null);
            ClientPayloadHandler.handle(new WarpStartPacket("sys1:lush", 400), null);

            var snapshot = ClientPlanetState.captureVisualSnapshot();
            double ticks = snapshot.warpProgress() * snapshot.warpDurationTicks();
            var expected = ShipFlightController.sampleUniversePosition(
                    departure, "sys1:lush", 400, ticks);

            assertEquals(0.0, Math.sqrt(expected.distanceToSqr(snapshot.universePosition())),
                    1.0E-6, "a ship at " + departure + " flew a route from the wrong body");
        }
    }
}
