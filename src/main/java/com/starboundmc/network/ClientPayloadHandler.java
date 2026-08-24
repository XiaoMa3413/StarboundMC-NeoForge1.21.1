package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.client.ClientTeleporterState;
import com.starboundmc.client.WarpSounds;
import com.starboundmc.world.Planet;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client payload effects in a dedicated class boundary. NeoForge invokes these
 * handlers only for clientbound play payloads; server authority remains in
 * {@link ServerPayloadHandler}.
 */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handle(SyncStarStatePacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
        ClientPlanetState.setStarState(payload.visited(), payload.currentEntryId());
    }

    public static void handle(SyncPlanetPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
        ClientPlanetState.setCurrent(Planet.fromId(payload.planetId()));
        if (ClientPlanetState.consumeArrivalCue()) {
            WarpSounds.onWarpFinished();
        }
    }

    public static void handle(WarpStartPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
        ClientPlanetState.startWarp(Planet.fromId(payload.planetId()),
                payload.durationTicks(), emptyToNull(payload.entryId()));
        WarpSounds.onWarpStarted();
    }

    public static void handle(SyncFuelPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
        ClientPlanetState.setFuel(payload.fuel(), payload.maxFuel());
    }

    public static void handle(TeleporterListPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
        ClientTeleporterState.receive(payload.entries(), payload.currentName());
    }

    public static void handle(SyncFlightPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
        ClientPlanetState.applyFlightSnapshot(payload.revision(), payload.serverTick(),
                payload.phase(), payload.position(), payload.velocity(),
                payload.yaw(), payload.pitch(), payload.roll(),
                payload.elapsedTicks(), payload.totalTicks(),
                emptyToNull(payload.targetEntryId()));
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
