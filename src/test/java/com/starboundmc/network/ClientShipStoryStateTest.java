package com.starboundmc.network;

import com.starboundmc.client.shipai.ClientShipStoryState;
import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.MineralScanState;
import com.starboundmc.story.SurfaceMissionState;
import com.starboundmc.story.SharedShipProgress;
import com.starboundmc.story.PlayerStoryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientShipStoryStateTest
{
    @Test void taskRevisionsAndWireRoundTripDoNotReplayOlderClaims() {
        var progress = com.starboundmc.story.NovaTaskProgress.DEFAULT.observe(true, true, true, false, true)
                .claim(com.starboundmc.story.NovaTask.SURFACE);
        var packet = snapshot(7, 1, CoreState.ONLINE, 1, true).withTasks(progress);
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            ShipStorySnapshotPacket.STREAM_CODEC.encode(buffer, packet);
            assertEquals(packet, ShipStorySnapshotPacket.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
        ClientShipStoryState.apply(7, packet);
        ClientShipStoryState.apply(7, snapshot(7, 2, CoreState.ONLINE, 2, true));
        assertEquals(progress, ClientShipStoryState.tasks(7));
        ClientShipStoryState.beginContainer(8);
        assertFalse(ClientShipStoryState.apply(8, packet));
        assertEquals(com.starboundmc.story.NovaTaskProgress.DEFAULT, ClientShipStoryState.tasks(8));
    }
    @BeforeEach
    void resetConnection()
    {
        ClientShipStoryState.resetConnectionState();
    }

    @Test
    void sharedAndPersonalRevisionsMergeIndependently()
    {
        ClientShipStoryState.apply(7, snapshot(7, 5L, CoreState.OFFLINE, 9L, true));
        ClientShipStoryState.apply(7, snapshot(7, 6L, CoreState.ONLINE, 8L, false));

        ClientShipStoryState.Snapshot merged = ClientShipStoryState.snapshot(7);
        assertEquals(6L, merged.shared().revision());
        assertEquals(CoreState.ONLINE, merged.shared().core());
        assertEquals(9L, merged.player().revision());
        assertTrue(merged.player().identityConfirmed());
    }

    @Test
    void aNewContainerDropsLateStateFromThePreviousMenu()
    {
        ClientShipStoryState.apply(7, snapshot(7, 5L, CoreState.ONLINE, 9L, true));
        ClientShipStoryState.apply(8, snapshot(8, 0L, CoreState.OFFLINE, 0L, false));
        assertFalse(ClientShipStoryState.apply(
                8, snapshot(7, 6L, CoreState.ONLINE, 10L, true)));

        assertFalse(ClientShipStoryState.hasSnapshot(7));
        assertTrue(ClientShipStoryState.hasSnapshot(8));
        assertEquals(CoreState.OFFLINE, ClientShipStoryState.snapshot(8).shared().core());
    }

    @Test
    void everyAcceptedReplyAdvancesTheUpdateSequenceEvenWhenRevisionsMatch()
    {
        ClientShipStoryState.apply(7, snapshot(7, 5L, CoreState.ONLINE, 9L, true));
        long firstSequence = ClientShipStoryState.snapshot(7).updateSequence();

        ClientShipStoryState.apply(7, snapshot(7, 5L, CoreState.ONLINE, 9L, true));

        assertEquals(firstSequence + 1L,
                ClientShipStoryState.snapshot(7).updateSequence());
    }

    @Test
    void aRejectedContainerReplyDoesNotAdvanceTheCurrentSequence()
    {
        ClientShipStoryState.apply(8, snapshot(8, 2L, CoreState.ONLINE, 3L, true));
        long sequence = ClientShipStoryState.snapshot(8).updateSequence();

        assertFalse(ClientShipStoryState.apply(
                8, snapshot(7, 3L, CoreState.ONLINE, 4L, true)));

        assertEquals(sequence, ClientShipStoryState.snapshot(8).updateSequence());
    }

    @Test
    void beginningAReusedContainerIdClearsItsPreviousSnapshot()
    {
        ClientShipStoryState.apply(7, snapshot(7, 5L, CoreState.ONLINE, 9L, true));

        ClientShipStoryState.beginContainer(7);

        assertFalse(ClientShipStoryState.hasSnapshot(7));
    }

    @Test
    void onlyThePacketAckIsExposedToTheUi()
    {
        ClientShipStoryState.apply(7, snapshotWithAck(7, 11L, 5L,
                CoreState.ONLINE, 9L, true));
        assertEquals(11L, ClientShipStoryState.snapshot(7).acknowledgedRequestId());

        ClientShipStoryState.apply(7, snapshotWithAck(7, 0L, 5L,
                CoreState.ONLINE, 9L, true));
        assertEquals(0L, ClientShipStoryState.snapshot(7).acknowledgedRequestId());
    }

    @Test
    void aZeroAckBroadcastCannotEraseAQueuedPositiveAck()
    {
        ClientShipStoryState.apply(7, snapshotWithAck(7, 11L, 5L,
                CoreState.ONLINE, 9L, true));
        ClientShipStoryState.apply(7, snapshotWithAck(7, 0L, 5L,
                CoreState.ONLINE, 9L, true));

        assertTrue(ClientShipStoryState.consumeAcknowledgement(7, 11L));
        assertFalse(ClientShipStoryState.consumeAcknowledgement(7, 11L));
    }

    @Test
    void acknowledgementFromAnOldContainerCannotReleaseCurrentContainer()
    {
        ClientShipStoryState.apply(7, snapshotWithAck(7, 11L, 5L,
                CoreState.ONLINE, 9L, true));
        ClientShipStoryState.beginContainer(8);

        assertFalse(ClientShipStoryState.consumeAcknowledgement(8, 11L));
    }

    @Test
    void equalSharedRevisionCannotReplaceSemanticStateButCanRefreshCountdown()
    {
        ClientShipStoryState.apply(7, new ShipStorySnapshotPacket(
                7, 0L, SharedShipProgress.CURRENT_SCHEMA_VERSION, 5L, CoreState.REBOOTING, SurfaceMissionState.LOCKED,
                EngineState.DAMAGED, EngineState.DAMAGED, MineralScanState.LOCKED, 20,
                PlayerStoryState.CURRENT_SCHEMA_VERSION, 1L, false, 0, 0, 0));
        ClientShipStoryState.apply(7, new ShipStorySnapshotPacket(
                7, 0L, SharedShipProgress.CURRENT_SCHEMA_VERSION, 5L, CoreState.OFFLINE, SurfaceMissionState.COMPLETE,
                EngineState.ONLINE, EngineState.ONLINE, MineralScanState.COMPLETE, 12,
                PlayerStoryState.CURRENT_SCHEMA_VERSION, 1L, true, 0, 0, 0));

        ClientShipStoryState.SharedView shared = ClientShipStoryState.snapshot(7).shared();
        ClientShipStoryState.PlayerView player = ClientShipStoryState.snapshot(7).player();
        assertEquals(CoreState.REBOOTING, shared.core());
        assertEquals(SurfaceMissionState.LOCKED, shared.surfaceMission());
        assertEquals(12, shared.rebootTicksRemaining());
        assertFalse(player.identityConfirmed());
    }

    private static ShipStorySnapshotPacket snapshot(int containerId, long sharedRevision,
                                                    CoreState core, long playerRevision,
                                                    boolean identityConfirmed)
    {
        return new ShipStorySnapshotPacket(containerId, 0L, SharedShipProgress.CURRENT_SCHEMA_VERSION, sharedRevision, core,
                SurfaceMissionState.LOCKED, EngineState.DAMAGED, EngineState.DAMAGED,
                MineralScanState.LOCKED,
                core == CoreState.REBOOTING ? 20 : 0,
                PlayerStoryState.CURRENT_SCHEMA_VERSION, playerRevision, identityConfirmed, 0, 0, 0);
    }

    private static ShipStorySnapshotPacket snapshotWithAck(int containerId, long ack,
                                                           long sharedRevision,
                                                           CoreState core,
                                                           long playerRevision,
                                                           boolean identityConfirmed)
    {
        return new ShipStorySnapshotPacket(containerId, ack, SharedShipProgress.CURRENT_SCHEMA_VERSION, sharedRevision, core,
                SurfaceMissionState.LOCKED, EngineState.DAMAGED, EngineState.DAMAGED,
                MineralScanState.LOCKED,
                core == CoreState.REBOOTING ? 20 : 0,
                PlayerStoryState.CURRENT_SCHEMA_VERSION, playerRevision, identityConfirmed, 0, 0, 0);
    }

    @Test
    void decoderRejectsUnsupportedSharedAndPersonalSchemas() {
        for (boolean shared : new boolean[]{true, false}) {
            int current = shared ? SharedShipProgress.CURRENT_SCHEMA_VERSION : PlayerStoryState.CURRENT_SCHEMA_VERSION;
            for (int unsupported : new int[]{current - 1, current + 1}) {
                var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    ShipStorySnapshotPacket.STREAM_CODEC.encode(buffer, snapshot(7, 0, CoreState.OFFLINE, 0, false));
                    buffer.readVarInt(); // container
                    buffer.readVarLong(); // acknowledgement
                    int schemaOffset = buffer.readerIndex();
                    if (!shared) {
                        buffer.readVarInt(); // shared schema
                        buffer.readVarLong(); // shared revision
                        for (int state = 0; state < 5; state++) buffer.readUtf();
                        buffer.readVarInt(); // reboot countdown
                        schemaOffset = buffer.readerIndex();
                    }
                    buffer.setByte(schemaOffset, unsupported);
                    buffer.readerIndex(0);
                    assertThrows(IllegalArgumentException.class, () -> ShipStorySnapshotPacket.STREAM_CODEC.decode(buffer));
                } finally { buffer.release(); }
            }
        }
    }
}
