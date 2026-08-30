package com.starboundmc.client;

import com.starboundmc.network.ShipEnvironmentSnapshotPacket;
import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.SharedShipProgress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientShipEnvironmentStateTest
{
    @AfterEach
    void clearState()
    {
        ClientShipEnvironmentState.resetConnectionState();
    }

    @Test
    void failsClosedUntilMatchingContainerSnapshotArrives()
    {
        ClientShipEnvironmentState.beginContainer(4);

        assertTrue(ClientShipEnvironmentState.isLocked(4));
        assertFalse(ClientShipEnvironmentState.apply(5, online(4, 1L, 0)));
        assertTrue(ClientShipEnvironmentState.isLocked(4));
        assertTrue(ClientShipEnvironmentState.apply(4, online(4, 1L, 0)));
        assertFalse(ClientShipEnvironmentState.isLocked(4));
    }

    @Test
    void sameRevisionUpdatesRebootCountdownAndOlderRevisionCannotUnlock()
    {
        ClientShipEnvironmentState.beginContainer(7);
        ClientShipEnvironmentState.apply(7, new ShipEnvironmentSnapshotPacket(
                7, 1, 8L, CoreState.REBOOTING,
                EngineState.DAMAGED, EngineState.DAMAGED, 40));
        ClientShipEnvironmentState.apply(7, new ShipEnvironmentSnapshotPacket(
                7, 1, 8L, CoreState.REBOOTING,
                EngineState.DAMAGED, EngineState.DAMAGED, 12));

        assertTrue(ClientShipEnvironmentState.isLocked(7));
        assertEquals(12, ClientShipEnvironmentState.snapshot(7).rebootTicksRemaining());

        ClientShipEnvironmentState.apply(7, online(7, 7L, 0));
        assertTrue(ClientShipEnvironmentState.isLocked(7));
        assertEquals(CoreState.REBOOTING, ClientShipEnvironmentState.snapshot(7).core());

        ClientShipEnvironmentState.apply(7, online(7, 9L, 0));
        assertFalse(ClientShipEnvironmentState.isLocked(7));
    }

    @Test
    void aNewContainerClearsPreviousMenuStateButSameContainerKeepsEarlyPacket()
    {
        ClientShipEnvironmentState.apply(11, online(11, 3L, 0));
        ClientShipEnvironmentState.beginContainer(11);
        assertFalse(ClientShipEnvironmentState.isLocked(11));

        ClientShipEnvironmentState.beginContainer(12);
        assertTrue(ClientShipEnvironmentState.isLocked(12));
        assertTrue(ClientShipEnvironmentState.isLocked(11));
    }

    @Test
    void aFutureSchemaRemainsLockedEvenIfAnOlderSnapshotArrivesLater()
    {
        ClientShipEnvironmentState.beginContainer(15);
        ClientShipEnvironmentState.apply(15, new ShipEnvironmentSnapshotPacket(
                15, SharedShipProgress.CURRENT_SCHEMA_VERSION + 1, 20L,
                CoreState.ONLINE, EngineState.ONLINE, EngineState.ONLINE, 0));

        assertTrue(ClientShipEnvironmentState.isLocked(15));
        assertFalse(ClientShipEnvironmentState.hasSupportedSnapshot(15));

        ClientShipEnvironmentState.apply(15, online(15, 21L, 0));
        assertTrue(ClientShipEnvironmentState.isLocked(15));
    }

    @Test
    void engineCapabilitiesAreMirroredSeparatelyFromCoreLock()
    {
        ClientShipEnvironmentState.beginContainer(18);
        ClientShipEnvironmentState.apply(18, new ShipEnvironmentSnapshotPacket(
                18, 1, 3L, CoreState.ONLINE,
                EngineState.ONLINE, EngineState.DAMAGED, 0));

        assertFalse(ClientShipEnvironmentState.isLocked(18));
        assertTrue(ClientShipEnvironmentState.canTravelWithinSystem(18));
        assertFalse(ClientShipEnvironmentState.canTravelBetweenSystems(18));
        assertEquals(EngineState.DAMAGED,
                ClientShipEnvironmentState.hyperdrive(18));
    }

    private static ShipEnvironmentSnapshotPacket online(int containerId, long revision,
                                                         int rebootTicks)
    {
        return new ShipEnvironmentSnapshotPacket(containerId, 1, revision, CoreState.ONLINE,
                EngineState.ONLINE, EngineState.ONLINE, rebootTicks);
    }
}
