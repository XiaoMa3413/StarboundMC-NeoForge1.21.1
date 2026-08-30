package com.starboundmc.client;

import com.starboundmc.network.ShipEnvironmentSnapshotPacket;
import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.SharedShipProgress;

/**
 * Connection-scoped client mirror for the shared ship environment.
 *
 * <p>The mirror is deliberately bound to the current menu id. A packet for a
 * previous menu must never unlock a newly opened screen that happens to reuse
 * the same client-side UI tree.</p>
 */
public final class ClientShipEnvironmentState
{
    private static int containerId = -1;
    private static long updateSequence;
    private static View view;
    /**
     * Once a server has sent a newer schema, this client cannot safely infer
     * the meaning of its state vector. Keep the current menu fail-closed until
     * it is replaced or the connection is reset.
     */
    private static boolean unsupportedSchema;

    private ClientShipEnvironmentState()
    {
    }

    public static void resetConnectionState()
    {
        containerId = -1;
        updateSequence = 0L;
        view = null;
        unsupportedSchema = false;
    }

    /** Starts (or reuses) a menu session without discarding an already-arrived packet. */
    public static void beginContainer(int newContainerId)
    {
        if (containerId == newContainerId)
            return;
        containerId = newContainerId;
        updateSequence = 0L;
        view = null;
        unsupportedSchema = false;
    }

    /** Clears the session when the corresponding screen is removed. */
    public static void endContainer(int expectedContainerId)
    {
        if (containerId == expectedContainerId)
            resetConnectionState();
    }

    public static boolean apply(int activeContainerId, ShipEnvironmentSnapshotPacket snapshot)
    {
        if (snapshot.containerId() != activeContainerId)
            return false;
        if (containerId != activeContainerId)
            beginContainer(activeContainerId);

        if (snapshot.schemaVersion() > SharedShipProgress.CURRENT_SCHEMA_VERSION)
            unsupportedSchema = true;

        if (view == null || snapshot.revision() > view.revision()
                || snapshot.revision() == view.revision()
                && snapshot.schemaVersion() > view.schemaVersion())
        {
            view = new View(snapshot.schemaVersion(), snapshot.revision(), snapshot.core(),
                    snapshot.sublightEngine(), snapshot.hyperdrive(),
                    snapshot.rebootTicksRemaining());
        }
        else if (snapshot.revision() == view.revision()
                && snapshot.schemaVersion() == view.schemaVersion())
        {
            // The reboot countdown changes every tick without changing the
            // persisted state revision.
            view = view.withRebootTicksRemaining(snapshot.rebootTicksRemaining());
        }
        updateSequence = increment(updateSequence);
        return true;
    }

    public static boolean hasSnapshot(int expectedContainerId)
    {
        return containerId == expectedContainerId && view != null;
    }

    public static boolean hasSupportedSnapshot(int expectedContainerId)
    {
        return hasSnapshot(expectedContainerId) && !unsupportedSchema
                && view.schemaSupported();
    }

    /** Fail closed until the server has supplied a snapshot for this menu. */
    public static boolean isLocked(int expectedContainerId)
    {
        return !hasSupportedSnapshot(expectedContainerId)
                || view.core() != CoreState.ONLINE;
    }

    public static boolean isCoreOnline(int expectedContainerId)
    {
        return hasSupportedSnapshot(expectedContainerId)
                && view.core() == CoreState.ONLINE;
    }

    public static EngineState sublightEngine(int expectedContainerId)
    {
        return hasSupportedSnapshot(expectedContainerId)
                ? view.sublightEngine() : EngineState.DAMAGED;
    }

    public static EngineState hyperdrive(int expectedContainerId)
    {
        return hasSupportedSnapshot(expectedContainerId)
                ? view.hyperdrive() : EngineState.DAMAGED;
    }

    public static boolean canTravelWithinSystem(int expectedContainerId)
    {
        return isCoreOnline(expectedContainerId)
                && view.sublightEngine() == EngineState.ONLINE;
    }

    public static boolean canTravelBetweenSystems(int expectedContainerId)
    {
        return canTravelWithinSystem(expectedContainerId)
                && view.hyperdrive() == EngineState.ONLINE;
    }

    public static Snapshot snapshot(int expectedContainerId)
    {
        if (!hasSnapshot(expectedContainerId))
            throw new IllegalStateException(
                    "No ship environment snapshot for container " + expectedContainerId);
        return new Snapshot(containerId, updateSequence, view);
    }

    private static long increment(long value)
    {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }

    public record Snapshot(int containerId, long updateSequence, View view)
    {
        public CoreState core()
        {
            return view.core();
        }

        public EngineState sublightEngine()
        {
            return view.sublightEngine();
        }

        public EngineState hyperdrive()
        {
            return view.hyperdrive();
        }

        public int rebootTicksRemaining()
        {
            return view.rebootTicksRemaining();
        }
    }

    public record View(int schemaVersion, long revision, CoreState core,
                       EngineState sublightEngine, EngineState hyperdrive,
                       int rebootTicksRemaining)
    {
        public boolean schemaSupported()
        {
            return schemaVersion <= SharedShipProgress.CURRENT_SCHEMA_VERSION;
        }

        private View withRebootTicksRemaining(int remainingTicks)
        {
            return new View(schemaVersion, revision, core, sublightEngine,
                    hyperdrive, remainingTicks);
        }
    }
}
