package com.starboundmc.client.shipai;

import com.starboundmc.network.ShipStorySnapshotPacket;
import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.MineralScanState;
import com.starboundmc.story.PlayerStoryState;
import com.starboundmc.story.SharedShipProgress;
import com.starboundmc.story.SurfaceMissionState;

import java.util.ArrayDeque;
import java.util.Deque;

/** Connection-scoped client mirror populated only by owner-targeted S2C snapshots. */
public final class ClientShipStoryState
{
    private static int containerId = -1;
    private static long updateSequence;
    private static long acknowledgedRequestId;
    /**
     * A shared-state broadcast may arrive between the acknowledgement packet
     * and the next UI tick. Keep positive acknowledgements until the root has
     * consumed the matching request instead of treating the latest packet's
     * zero value as a destructive reset.
     */
    private static final Deque<Long> acknowledgedRequestIds = new ArrayDeque<>();
    private static SharedView shared;
    private static PlayerView player;

    private ClientShipStoryState()
    {
    }

    public static void resetConnectionState()
    {
        beginContainer(-1);
    }

    /** Clears any snapshot left by a previous menu, including a reused id. */
    public static void beginContainer(int newContainerId)
    {
        containerId = newContainerId;
        updateSequence = 0L;
        acknowledgedRequestId = 0L;
        acknowledgedRequestIds.clear();
        shared = null;
        player = null;
    }

    public static boolean apply(int activeContainerId, ShipStorySnapshotPacket snapshot)
    {
        if (snapshot.containerId() != activeContainerId)
            return false;
        if (snapshot.containerId() != containerId)
            beginContainer(snapshot.containerId());
        if (shared == null || snapshot.sharedRevision() > shared.revision()
                || snapshot.sharedRevision() == shared.revision()
                && snapshot.sharedSchemaVersion() > shared.schemaVersion())
        {
            shared = new SharedView(snapshot.sharedSchemaVersion(), snapshot.sharedRevision(),
                    snapshot.core(), snapshot.surfaceMission(), snapshot.sublightEngine(),
                    snapshot.hyperdrive(), snapshot.mineralScan(),
                    snapshot.rebootTicksRemaining());
        }
        else if (snapshot.sharedRevision() == shared.revision()
                && snapshot.sharedSchemaVersion() == shared.schemaVersion())
        {
            shared = shared.withRebootTicksRemaining(snapshot.rebootTicksRemaining());
        }
        if (player == null || snapshot.playerRevision() > player.revision()
                || snapshot.playerRevision() == player.revision()
                && snapshot.playerSchemaVersion() > player.schemaVersion())
        {
            player = new PlayerView(snapshot.playerSchemaVersion(), snapshot.playerRevision(),
                    snapshot.identityConfirmed(), snapshot.readSituationMask(),
                    snapshot.tutorialMask(), snapshot.dismissedHintMask());
        }
        // An acknowledgement belongs to this packet only. Unrelated shared
        // broadcasts must never be replayed as an ack for a later request.
        acknowledgedRequestId = snapshot.acknowledgedRequestId();
        if (acknowledgedRequestId > 0L && !acknowledgedRequestIds.contains(acknowledgedRequestId))
            acknowledgedRequestIds.addLast(acknowledgedRequestId);
        updateSequence = increment(updateSequence);
        return true;
    }

    /** Removes one acknowledgement for the active container, if it is queued. */
    public static boolean consumeAcknowledgement(int expectedContainerId, long requestId)
    {
        if (requestId <= 0L || containerId != expectedContainerId)
            return false;
        return acknowledgedRequestIds.removeFirstOccurrence(requestId);
    }

    public static boolean hasSnapshot(int expectedContainerId)
    {
        return containerId == expectedContainerId && shared != null && player != null;
    }

    public static Snapshot snapshot(int expectedContainerId)
    {
        if (!hasSnapshot(expectedContainerId))
            throw new IllegalStateException("No ship story snapshot for container " + expectedContainerId);
        return new Snapshot(containerId, updateSequence, acknowledgedRequestId,
                shared, player);
    }

    private static long increment(long value)
    {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }

    /**
     * updateSequence advances for every accepted server reply, even when both
     * persisted revisions are unchanged. The UI uses it to release a pending
     * request after an idempotent or rejected action receives its authoritative
     * no-op snapshot.
     */
    public record Snapshot(int containerId, long updateSequence,
                           long acknowledgedRequestId,
                           SharedView shared, PlayerView player)
    {
    }

    public record SharedView(int schemaVersion, long revision, CoreState core,
                             SurfaceMissionState surfaceMission, EngineState sublightEngine,
                             EngineState hyperdrive, MineralScanState mineralScan,
                             int rebootTicksRemaining)
    {
        public boolean schemaSupported()
        {
            return schemaVersion <= SharedShipProgress.CURRENT_SCHEMA_VERSION;
        }

        SharedView withRebootTicksRemaining(int remainingTicks)
        {
            return new SharedView(schemaVersion, revision, core, surfaceMission,
                    sublightEngine, hyperdrive, mineralScan, remainingTicks);
        }
    }

    public record PlayerView(int schemaVersion, long revision, boolean identityConfirmed,
                             int readSituationMask, int tutorialMask, int dismissedHintMask)
    {
        public boolean schemaSupported()
        {
            return schemaVersion <= PlayerStoryState.CURRENT_SCHEMA_VERSION;
        }
    }
}
