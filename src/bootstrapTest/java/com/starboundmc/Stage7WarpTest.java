package com.starboundmc;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.ShipSpace;
import com.starboundmc.warp.ShipWarpManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.UniverseTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage7WarpTest {
    @Test
    void everyRouteAdvancesThroughTheExpectedDeterministicPhases() {
        for (String from : UniverseTestSupport.navigableEntryIds()) {
            for (String to : UniverseTestSupport.navigableEntryIds()) {
                if (from == to) {
                    continue;
                }
                ShipFlightController flight = new ShipFlightController(from, to);
                List<FlightPhase> phases = new ArrayList<>();
                phases.add(flight.getPhase());
                while (!flight.isLanded()) {
                    FlightPhase previous = flight.getPhase();
                    flight.tick();
                    if (flight.getPhase() != previous) {
                        phases.add(flight.getPhase());
                    }
                }
                FlightPhase travel = flight.isShortRoute() ? FlightPhase.CRUISE : FlightPhase.HYPERSPACE;
                assertEquals(List.of(FlightPhase.TURN, FlightPhase.ACCELERATE, travel,
                        FlightPhase.DECELERATE, FlightPhase.ARRIVE), phases, from + " -> " + to);
                assertEquals(UniverseNavigation.universeDock(to), flight.getUniversePosition());
                assertEquals(new UniverseDelta(0.0, 0.0, 0.0), flight.getUniverseVelocity());
                assertEquals(0L, flight.getRemainingTicks());
            }
        }
    }

    @Test
    void allSampledRoutesStayOutsideEveryPlanetKeepOutShell() {
        for (String from : UniverseTestSupport.navigableEntryIds()) {
            for (String to : UniverseTestSupport.navigableEntryIds()) {
                if (from == to) {
                    continue;
                }
                ShipFlightController flight = new ShipFlightController(from, to);
                for (int tick = 0; tick <= flight.getTotalTicks(); tick++) {
                    var position = ShipFlightController.sampleUniversePosition(
                            from, to, flight.getTotalTicks(), tick);
                    for (String body : UniverseTestSupport.navigableEntryIds()) {
                        UniverseDelta delta = position.deltaTo(UniverseNavigation.universeBodyPosition(body));
                        double planarDistance = Math.hypot(delta.x(), delta.z());
                        assertTrue(planarDistance + 1.0e-6 >= UniverseNavigation.radius(body) * 1.44,
                                from + " -> " + to + " entered " + body + " shell at tick " + tick);
                    }
                }
            }
        }
    }

    @Test
    void routeDurationsAndFuelCostsPreserveGameplayContract() {
        ShipFlightController local = new ShipFlightController("sys1:lush", "sys1:molten");
        ShipFlightController crossSystem = new ShipFlightController("sys1:lush", "sys2:frozen");
        assertEquals(220, local.getTotalTicks());
        assertTrue(crossSystem.getTotalTicks() >= 360 && crossSystem.getTotalTicks() <= 560);
        assertEquals(20, ShipWarpManager.warpFuelCost("sys1:lush", "sys1:molten"));
        assertEquals(100, ShipWarpManager.warpFuelCost("sys1:lush", "sys2:frozen"));
    }

    @Test
    void warpPayloadAndLifecycleEventsAreServerAuthoritative() throws IOException {
        String handler = source("network/ServerPayloadHandler.java");
        String actions = source("network/Stage7ServerPayloadActions.java");
        String manager = source("warp/ShipWarpManager.java");
        String events = source("event/ShipWarpEvents.java");
        assertTrue(handler.contains("instanceof WarpControlMenu menu"));
        assertTrue(handler.contains("menu.stillValid(player)"));
        assertTrue(actions.contains("instanceof WarpControlMenu menu && menu.stillValid(player)"));
        assertTrue(manager.contains("player.level().dimension().equals(Stage6TravelService.SHIP_LEVEL)"));
        // Migration A5 renamed the reachability check: a warp target must be
        // navigable (has flight geometry), which is deliberately not the same as
        // landable (has a surface dimension). Assert both, so a later edit cannot
        // silently swap one for the other and admit the gas giant as a destination.
        assertTrue(manager.contains("entry == null || !entry.isNavigable()"));
        assertFalse(manager.contains("!entry.isLandable()"));
        assertTrue(manager.contains("getFuel() < cost"));
        assertTrue(manager.contains("canTravelWithinSystem(server)"));
        assertTrue(manager.contains("canTravelBetweenSystems(server)"));
        assertTrue(manager.contains("SNAPSHOT_INTERVAL = 5"));
        assertTrue(events.contains("ServerTickEvent.Post"));
        assertTrue(events.contains("ServerStartedEvent"));
        assertTrue(events.contains("ServerStoppedEvent"));
        assertFalse(events.contains("net.minecraftforge"));
    }

    @Test
    void bothConsoleTypesUseTheSameWarpAuthorityBoundary() throws IOException {
        String shipConsole = source("menu/ShipConsoleMenu.java");
        String starmapTerminal = source("menu/StarmapTerminalMenu.java");
        String boundary = source("menu/WarpControlMenu.java");

        assertTrue(shipConsole.contains("implements WarpControlMenu"));
        assertTrue(starmapTerminal.contains("implements WarpControlMenu"));
        assertTrue(boundary.contains("boolean stillValid(Player player)"));
    }

    @Test
    void reconnectAndRestartCannotLeaveAStaleSnapshotRevision() throws IOException {
        String client = source("network/ClientNetworkState.java");
        String clientEvents = source("client/ClientConnectionEvents.java");
        String manager = source("warp/ShipWarpManager.java");
        assertTrue(client.contains("flight = null"));
        assertTrue(clientEvents.contains("ClientPlayerNetworkEvent.LoggingIn"));
        assertTrue(clientEvents.contains("ClientPlayerNetworkEvent.LoggingOut"));
        assertTrue(manager.contains("server.overworld().getGameTime()"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }
}
