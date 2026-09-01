package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.client.ClientTeleporterState;
import com.starboundmc.client.ClientShipEnvironmentState;
import com.starboundmc.client.WarpSounds;
import com.starboundmc.client.shipai.ClientShipStoryState;
import com.starboundmc.client.shipai.ClientNovaBroadcastState;
import com.starboundmc.menu.ShipAiTerminalMenu;
import com.starboundmc.menu.StarmapTerminalMenu;
import com.starboundmc.menu.TeleporterMenu;
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

    public static void handle(SyncVoxelWalletPacket payload, IPayloadContext context) {
        com.starboundmc.client.ClientVoxelWalletState.set(payload.balance());
    }

    public static void handle(SyncVoxelMachinePacket payload, IPayloadContext context) {
        com.starboundmc.client.ClientVoxelMachineState.apply(
                payload, context.player().level().getGameTime());
    }

    public static void handle(SyncPrintQueuePacket payload, IPayloadContext context) {
        com.starboundmc.client.ClientPrintQueueState.apply(payload);
    }

    public static void handle(ShipStorySnapshotPacket payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof ShipAiTerminalMenu menu) {
            ClientShipStoryState.apply(menu.containerId, payload);
        }
    }

    public static void handle(ShipEnvironmentSnapshotPacket payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof StarmapTerminalMenu menu) {
            ClientShipEnvironmentState.apply(menu.containerId, payload);
        } else if (context.player().containerMenu instanceof TeleporterMenu menu) {
            ClientShipEnvironmentState.apply(menu.containerId, payload);
        }
    }

    public static void handle(NovaBroadcastPacket payload, IPayloadContext context) {
        ClientNovaBroadcastState.enqueue(payload.translationKey());
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
