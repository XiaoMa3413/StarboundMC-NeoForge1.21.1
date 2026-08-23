package com.starboundmc.network;

import com.starboundmc.client.ClientTeleporterState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client payload effects in a dedicated class boundary. This class deliberately
 * imports no client classes, so registering its handlers is safe on a dedicated
 * server; NeoForge invokes them only for clientbound play payloads.
 */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handle(SyncStarStatePacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
    }

    public static void handle(SyncPlanetPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
    }

    public static void handle(WarpStartPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
    }

    public static void handle(SyncFuelPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
    }

    public static void handle(TeleporterListPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
        ClientTeleporterState.receive(payload.entries(), payload.currentName());
    }

    public static void handle(SyncFlightPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
    }
}
