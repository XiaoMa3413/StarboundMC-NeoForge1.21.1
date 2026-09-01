package com.starboundmc.network;

import com.starboundmc.client.shipai.ClientShipStoryState;
import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.MineralScanState;
import com.starboundmc.story.SurfaceMissionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientShipStoryStateTest
{
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
                7, 0L, 1, 5L, CoreState.REBOOTING, SurfaceMissionState.LOCKED,
                EngineState.DAMAGED, EngineState.DAMAGED, MineralScanState.LOCKED, 20,
                1, 1L, false, 0, 0, 0));
        ClientShipStoryState.apply(7, new ShipStorySnapshotPacket(
                7, 0L, 1, 5L, CoreState.OFFLINE, SurfaceMissionState.COMPLETE,
                EngineState.ONLINE, EngineState.ONLINE, MineralScanState.COMPLETE, 12,
                1, 1L, true, 0, 0, 0));

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
        return new ShipStorySnapshotPacket(containerId, 0L, 1, sharedRevision, core,
                SurfaceMissionState.LOCKED, EngineState.DAMAGED, EngineState.DAMAGED,
                MineralScanState.LOCKED,
                core == CoreState.REBOOTING ? 20 : 0,
                1, playerRevision, identityConfirmed, 0, 0, 0);
    }

    private static ShipStorySnapshotPacket snapshotWithAck(int containerId, long ack,
                                                           long sharedRevision,
                                                           CoreState core,
                                                           long playerRevision,
                                                           boolean identityConfirmed)
    {
        return new ShipStorySnapshotPacket(containerId, ack, 1, sharedRevision, core,
                SurfaceMissionState.LOCKED, EngineState.DAMAGED, EngineState.DAMAGED,
                MineralScanState.LOCKED,
                core == CoreState.REBOOTING ? 20 : 0,
                1, playerRevision, identityConfirmed, 0, 0, 0);
    }
}
