package com.starboundmc.network;

import java.util.Objects;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge 1.21.1 play-payload protocol. */
public final class ModNetwork {
    public static final String PROTOCOL_VERSION = "15";
    private static volatile ServerPayloadActions serverActions = ServerPayloadActions.NONE;

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(JumpBoostPacket.TYPE, JumpBoostPacket.STREAM_CODEC, JumpBoostPacket::handle);
        registrar.playToClient(MobilityStatePacket.TYPE, MobilityStatePacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToServer(EppModuleClickPacket.TYPE, EppModuleClickPacket.STREAM_CODEC, EppModuleClickPacket::handle);
        registrar.playToServer(RelayActionPacket.TYPE, RelayActionPacket.STREAM_CODEC, RelayActionPacket::handle);
        registrar.playToClient(RelaySnapshotPacket.TYPE, RelaySnapshotPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToServer(EvaInputPacket.TYPE, EvaInputPacket.STREAM_CODEC, EvaInputPacket::handle);
        registrar.playToClient(EvaStatePacket.TYPE, EvaStatePacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToServer(OpenEppPacket.TYPE, OpenEppPacket.STREAM_CODEC, OpenEppPacket::handle);
        registrar.playToClient(EppSnapshotPacket.TYPE, EppSnapshotPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(EppVisualPacket.TYPE, EppVisualPacket.STREAM_CODEC, ClientPayloadHandler::handle);

        registrar.playToServer(UpgradeMatterManipulatorPacket.TYPE,
                UpgradeMatterManipulatorPacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(StartWarpPacket.TYPE,
                StartWarpPacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(TeleporterUsePacket.TYPE,
                TeleporterUsePacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(TeleporterRenamePacket.TYPE,
                TeleporterRenamePacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(TeleportToShipPacket.TYPE,
                TeleportToShipPacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(AddFuelPacket.TYPE,
                AddFuelPacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(StartRefinementPacket.TYPE,
                StartRefinementPacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(StopRefinementPacket.TYPE,
                StopRefinementPacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(ClaimRefinedVoxelsPacket.TYPE,
                ClaimRefinedVoxelsPacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(StartPrintPacket.TYPE,
                StartPrintPacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(CancelPrintQueuePacket.TYPE,
                CancelPrintQueuePacket.STREAM_CODEC, ServerPayloadHandler::handle);
        registrar.playToServer(ShipAiActionPacket.TYPE,
                ShipAiActionPacket.STREAM_CODEC, ServerPayloadHandler::handle);

        registrar.playToClient(SyncStarStatePacket.TYPE,
                SyncStarStatePacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(WarpStartPacket.TYPE,
                WarpStartPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(SyncFuelPacket.TYPE,
                SyncFuelPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(TeleporterListPacket.TYPE,
                TeleporterListPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(SyncFlightPacket.TYPE,
                SyncFlightPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(SyncVoxelWalletPacket.TYPE,
                SyncVoxelWalletPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(SyncVoxelMachinePacket.TYPE,
                SyncVoxelMachinePacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(SyncPrintQueuePacket.TYPE,
                SyncPrintQueuePacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(PrintSubmissionResultPacket.TYPE,
                PrintSubmissionResultPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(ShipStorySnapshotPacket.TYPE,
                ShipStorySnapshotPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(ShipEnvironmentSnapshotPacket.TYPE,
                ShipEnvironmentSnapshotPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(NovaBroadcastPacket.TYPE,
                NovaBroadcastPacket.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToClient(HudBootstrapStatePacket.TYPE,
                HudBootstrapStatePacket.STREAM_CODEC, ClientPayloadHandler::handle);
    }

    public static void installServerActions(ServerPayloadActions actions) {
        serverActions = Objects.requireNonNull(actions, "actions");
    }

    static ServerPayloadActions serverActions() {
        return serverActions;
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersInDimension(level, payload);
    }

    public static void sendToPlayersTrackingChunk(
            ServerLevel level, ChunkPos chunk, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingChunk(level, chunk, payload);
    }
}
