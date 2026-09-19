package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.client.ClientTeleporterState;
import com.starboundmc.client.ClientShipEnvironmentState;
import com.starboundmc.client.WarpSounds;
import com.starboundmc.client.shipai.ClientShipStoryState;
import com.starboundmc.client.shipai.ClientNovaBroadcastState;
import com.starboundmc.client.hud.HudBootController;
import com.starboundmc.menu.ShipAiTerminalMenu;
import com.starboundmc.menu.StarmapTerminalMenu;
import com.starboundmc.menu.TeleporterMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client payload effects in a dedicated class boundary. NeoForge invokes these
 * handlers only for clientbound play payloads; server authority remains in
 * {@link ServerPayloadHandler}.
 */
public final class ClientPayloadHandler {
    public static void handle(MobilityStatePacket payload, IPayloadContext context) {
        var state = context.player().getData(com.starboundmc.story.ModAttachments.MOBILITY_STATE);
        state.equipped = payload.equipped();
        state.charged = payload.charged();
    }
    public static void handle(RelaySnapshotPacket payload, IPayloadContext context) {
        com.starboundmc.client.space.RelayClientState.snapshot = payload;
        com.starboundmc.client.space.RelayClientState.receivedTick = context.player().level().getGameTime();
    }
    public static void handle(EvaStatePacket payload, IPayloadContext context) {
        var player = context.player();
        if (!player.level().dimension().location().equals(payload.dimension())) return;
        var state = player.getData(com.starboundmc.story.ModAttachments.EVA);
        if (state.mode != payload.mode() || !payload.dimension().equals(state.dimension)) state.clearInput();
        state.mode = Math.clamp(payload.mode(), 0, 2);
        state.dimension = payload.dimension();
    }
    public static void handle(EppSnapshotPacket payload, IPayloadContext context) {
        com.starboundmc.client.epp.EppClientState.snapshot = payload;
    }
    public static void handle(EppVisualPacket payload, IPayloadContext context) {
        var entity = context.player().level().getEntity(payload.entityId());
        if (entity != null) entity.setData(com.starboundmc.story.ModAttachments.EPP_VISUAL, Math.clamp(payload.generation(), 0, 2));
    }
    private ClientPayloadHandler() {
    }

    public static void handle(SyncStarStatePacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
        // This packet carries the ship's location, which is both the star map's
        // "current" marker and the departure end of the next flight route. It
        // replaced the legacy planet sync that used to be the only thing telling the
        // client where the ship was (§22), so both have to be applied here.
        ClientPlanetState.setStarState(payload.visited(), payload.currentEntryId());
        // The arrival cue is consumed here now. It used to be consumed by the
        // legacy planet sync, which was deleted (§22); this packet is sent on
        // arrival as well, so the sound still fires exactly once.
        if (ClientPlanetState.consumeArrivalCue()) {
            WarpSounds.onWarpFinished();
        }
    }

    public static void handle(WarpStartPacket payload, IPayloadContext context) {
        ClientNetworkState.apply(payload);
        String targetEntryId = emptyToNull(payload.entryId());
        ClientPlanetState.startWarp(targetEntryId, payload.durationTicks(), targetEntryId);
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
                emptyToNull(payload.targetEntryId()), payload.crewHold());
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

    public static void handle(PrintSubmissionResultPacket payload, IPayloadContext context) {
        if (net.minecraft.client.Minecraft.getInstance().screen
                instanceof com.starboundmc.client.VoxelPrintingStationScreen screen
                && screen.getMenu().containerId == payload.containerId()
                && screen.getMenu().blockPos().equals(payload.pos())) {
            screen.acceptSubmission(payload.accepted());
        }
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
        HudBootController.INSTANCE.onNovaBroadcast(payload.translationKey());
        ClientNovaBroadcastState.enqueue(payload.translationKey());
    }

    public static void handle(HudBootstrapStatePacket payload, IPayloadContext context) {
        if (payload.core() == com.starboundmc.story.CoreState.ONLINE)
            ClientNovaBroadcastState.clearDeferredBootMessages();
        HudBootController.INSTANCE.applyServerState(
                payload.core(), payload.wakePresented(), payload.terminalContacted());
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
