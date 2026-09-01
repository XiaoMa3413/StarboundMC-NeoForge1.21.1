package com.starboundmc.story;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedShipProgressTest
{
    @Test
    void newWorldStartsOfflineAndLocked()
    {
        SharedShipProgress state = SharedShipProgress.newWorld();

        assertEquals(CoreState.OFFLINE, state.core());
        assertEquals(SurfaceMissionState.LOCKED, state.surfaceMission());
        assertEquals(EngineState.DAMAGED, state.sublightEngine());
        assertEquals(EngineState.DAMAGED, state.hyperdrive());
        assertEquals(MineralScanState.LOCKED, state.mineralScan());
        assertFalse(state.canUseTeleporter());
        assertFalse(state.canBrowseCurrentSystem());
        assertFalse(state.canTravelWithinSystem());
        assertFalse(state.canTravelBetweenSystems());
    }

    @Test
    void repeatedCoreRebootRequestIsIdempotent()
    {
        SharedShipProgress offline = SharedShipProgress.newWorld();
        SharedShipProgress rebooting = offline.beginCoreReboot(100L, 50L);

        assertEquals(CoreState.REBOOTING, rebooting.core());
        assertEquals(150L, rebooting.rebootCompleteGameTime());
        assertEquals(1L, rebooting.revision());
        assertSame(rebooting, rebooting.beginCoreReboot(110L, 50L));
    }

    @Test
    void dueRebootCompletesOnlyOnce()
    {
        SharedShipProgress rebooting = SharedShipProgress.newWorld().beginCoreReboot(100L, 50L);

        assertSame(rebooting, rebooting.finishCoreRebootIfDue(149L));
        SharedShipProgress online = rebooting.finishCoreRebootIfDue(150L);
        assertEquals(CoreState.ONLINE, online.core());
        assertEquals(0L, online.rebootCompleteGameTime());
        assertSame(online, online.finishCoreRebootIfDue(151L));
    }

    @Test
    void hyperdriveCannotSkipSublightRepair()
    {
        SharedShipProgress online = SharedShipProgress.newWorld()
                .beginCoreReboot(0L, 1L)
                .finishCoreRebootIfDue(1L);

        assertSame(online, online.restoreHyperdrive());
        assertSame(online, online.restoreSublightEngine());
        SharedShipProgress missionComplete = online.activateSurfaceMission().completeSurfaceMission();
        SharedShipProgress sublight = missionComplete.restoreSublightEngine();
        SharedShipProgress hyperdrive = sublight.restoreHyperdrive();

        assertEquals(EngineState.ONLINE, sublight.sublightEngine());
        assertEquals(EngineState.DAMAGED, sublight.hyperdrive());
        assertTrue(hyperdrive.canTravelWithinSystem());
        assertTrue(hyperdrive.canTravelBetweenSystems());
    }

    @Test
    void mineralScanUsesPersistedFifteenFiveSixSecondPhases()
    {
        SharedShipProgress missionComplete = SharedShipProgress.newWorld()
                .beginCoreReboot(0L, 1L)
                .finishCoreRebootIfDue(1L)
                .activateSurfaceMission()
                .completeSurfaceMission();

        SharedShipProgress pending = missionComplete.beginMineralScan(100L, 300L);
        assertEquals(MineralScanState.PENDING, pending.mineralScan());
        assertEquals(400L, pending.mineralScanNextCueGameTime());
        assertSame(pending, pending.advanceMineralScanIfDue(399L, 100L, 120L));

        SharedShipProgress scanning = pending.advanceMineralScanIfDue(400L, 100L, 120L);
        assertEquals(MineralScanState.SCANNING, scanning.mineralScan());
        assertEquals(500L, scanning.mineralScanNextCueGameTime());

        SharedShipProgress reported = scanning.advanceMineralScanIfDue(500L, 100L, 120L);
        assertEquals(MineralScanState.RESULT_REPORTED, reported.mineralScan());
        assertEquals(620L, reported.mineralScanNextCueGameTime());

        SharedShipProgress complete = reported.advanceMineralScanIfDue(620L, 100L, 120L);
        assertEquals(MineralScanState.COMPLETE, complete.mineralScan());
        assertEquals(0L, complete.mineralScanNextCueGameTime());
        assertSame(complete, complete.advanceMineralScanIfDue(1_000L, 100L, 120L));
    }

    @Test
    void inProgressMineralScanRoundTripsWithoutLosingItsDeadline()
    {
        SharedShipProgress pending = SharedShipProgress.newWorld()
                .beginCoreReboot(0L, 1L)
                .finishCoreRebootIfDue(1L)
                .activateSurfaceMission()
                .completeSurfaceMission()
                .beginMineralScan(1_000L, 300L);

        SharedShipProgress.LoadResult loaded = SharedShipProgress.load(pending.save());

        assertEquals(MineralScanState.PENDING, loaded.state().mineralScan());
        assertEquals(1_300L, loaded.state().mineralScanNextCueGameTime());
        assertFalse(loaded.requiresSave());
    }

    @Test
    void versionOneDamagedShipCanEnterTheNewMineralScanScene()
    {
        CompoundTag old = SharedShipProgress.newWorld()
                .beginCoreReboot(0L, 1L)
                .finishCoreRebootIfDue(1L)
                .activateSurfaceMission()
                .completeSurfaceMission()
                .save();
        old.putInt("Version", 1);
        old.remove("MineralScan");
        old.remove("MineralScanNextCueAt");

        SharedShipProgress.LoadResult loaded = SharedShipProgress.load(old);

        assertEquals(MineralScanState.LOCKED, loaded.state().mineralScan());
        assertTrue(loaded.requiresSave());
    }

    @Test
    void interruptedRebootRecoversOnlineWhenLoaded()
    {
        SharedShipProgress rebooting = SharedShipProgress.newWorld().beginCoreReboot(500L, 60L);

        SharedShipProgress.LoadResult loaded = SharedShipProgress.load(rebooting.save());

        assertEquals(CoreState.ONLINE, loaded.state().core());
        assertEquals(0L, loaded.state().rebootCompleteGameTime());
        assertTrue(loaded.requiresSave());
    }

    @Test
    void invalidEngineCombinationFailsClosed()
    {
        CompoundTag tag = SharedShipProgress.legacyUnlocked().save();
        tag.putString("SublightEngine", EngineState.DAMAGED.id());

        SharedShipProgress.LoadResult loaded = SharedShipProgress.load(tag);

        assertEquals(EngineState.DAMAGED, loaded.state().sublightEngine());
        assertEquals(EngineState.DAMAGED, loaded.state().hyperdrive());
        assertTrue(loaded.requiresSave());
    }

    @Test
    void enginesLoadedBeforeFirstLandingFailClosed()
    {
        CompoundTag tag = SharedShipProgress.legacyUnlocked().save();
        tag.putString("SurfaceMission", SurfaceMissionState.LOCKED.id());

        SharedShipProgress.LoadResult loaded = SharedShipProgress.load(tag);

        assertEquals(EngineState.DAMAGED, loaded.state().sublightEngine());
        assertEquals(EngineState.DAMAGED, loaded.state().hyperdrive());
        assertTrue(loaded.requiresSave());
    }

    @Test
    void futureSchemaIsReadOnlyAndRoundTripsUntouched()
    {
        CompoundTag future = new CompoundTag();
        future.putInt("Version", SharedShipProgress.CURRENT_SCHEMA_VERSION + 1);
        future.putString("FutureField", "keep-me");

        SharedShipProgress.LoadResult loaded = SharedShipProgress.load(future);

        assertFalse(loaded.state().isWritable());
        assertSame(loaded.state(), loaded.state().beginCoreReboot(0L, 1L));
        assertEquals(future, loaded.state().save());
        assertFalse(loaded.requiresSave());
    }
}
