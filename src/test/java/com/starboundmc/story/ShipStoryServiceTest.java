package com.starboundmc.story;

import com.starboundmc.network.ShipStorySnapshotPacket;
import com.starboundmc.network.ShipEnvironmentSnapshotPacket;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipStoryServiceTest
{
    @Test
    void snapshotFactoryMapsOwnerStateAndClampsRebootTime()
    {
        SharedShipProgress rebooting = SharedShipProgress.newWorld()
                .beginCoreReboot(100L, 50L);
        PlayerStoryState personal = PlayerStoryState.DEFAULT
                .confirmIdentity()
                .withReadTopic(SituationTopic.INCIDENT);

        ShipStorySnapshotPacket during = ShipStoryService.snapshotFor(
                9, 0L, rebooting, personal, 120L);
        ShipStorySnapshotPacket after = ShipStoryService.snapshotFor(
                9, 0L, rebooting, personal, 200L);

        assertEquals(9, during.containerId());
        assertEquals(rebooting.revision(), during.sharedRevision());
        assertEquals(personal.revision(), during.playerRevision());
        assertEquals(personal.readSituationMask(), during.readSituationMask());
        assertEquals(MineralScanState.LOCKED, during.mineralScan());
        assertEquals(30, during.rebootTicksRemaining());
        assertEquals(0, after.rebootTicksRemaining());
    }

    @Test
    void nonRebootingSnapshotsNeverExposeARebootCountdown()
    {
        ShipStorySnapshotPacket snapshot = ShipStoryService.snapshotFor(
                2, 0L, SharedShipProgress.newWorld(), PlayerStoryState.DEFAULT,
                Long.MIN_VALUE);

        assertEquals(0, snapshot.rebootTicksRemaining());
    }

    @Test
    void futureSharedOrPersonalSchemasDisableEveryTerminalMutation()
    {
        SharedShipProgress current = SharedShipProgress.newWorld();
        PlayerStoryState futurePersonal = new PlayerStoryState(
                PlayerStoryState.CURRENT_SCHEMA_VERSION + 1,
                0L, false, 0, 0, 0);

        CompoundTag futureTag = new CompoundTag();
        futureTag.putInt("Version", SharedShipProgress.CURRENT_SCHEMA_VERSION + 1);
        SharedShipProgress futureShared = SharedShipProgress.load(futureTag).state();

        assertTrue(ShipStoryService.terminalActionsSupported(
                current, PlayerStoryState.DEFAULT));
        assertFalse(ShipStoryService.terminalActionsSupported(
                current, futurePersonal));
        assertFalse(ShipStoryService.terminalActionsSupported(
                futureShared, PlayerStoryState.DEFAULT));
    }

    @Test
    void environmentSnapshotUsesTheSharedRevisionAndClampsRemainingTicks()
    {
        SharedShipProgress rebooting = SharedShipProgress.newWorld()
                .beginCoreReboot(100L, 50L);

        ShipEnvironmentSnapshotPacket during = ShipEnvironmentService.snapshotFor(
                12, rebooting, 120L);
        ShipEnvironmentSnapshotPacket after = ShipEnvironmentService.snapshotFor(
                12, rebooting, 200L);

        assertEquals(12, during.containerId());
        assertEquals(rebooting.schemaVersion(), during.schemaVersion());
        assertEquals(rebooting.revision(), during.revision());
        assertEquals(CoreState.REBOOTING, during.core());
        assertEquals(30, during.rebootTicksRemaining());
        assertEquals(0, after.rebootTicksRemaining());
    }
}
