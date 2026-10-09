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
    void internalSchemasAndMissingAuthoritativeFieldsAreRejected() {
        var old = new CompoundTag(); old.putInt("Version", 2);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SharedShipProgress.load(old));
        var incomplete = new CompoundTag(); incomplete.putInt("Version", SharedShipProgress.CURRENT_SCHEMA_VERSION);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SharedShipProgress.load(incomplete));
    }

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
    void debugSkipCompletesEverySharedPrologueGateAtomically()
    {
        SharedShipProgress skipped = SharedShipProgress.newWorld().debugCompletePrologue();

        assertEquals(CoreState.ONLINE, skipped.core());
        assertEquals(SurfaceMissionState.COMPLETE, skipped.surfaceMission());
        assertEquals(EngineState.ONLINE, skipped.sublightEngine());
        assertEquals(EngineState.ONLINE, skipped.hyperdrive());
        assertEquals(MineralScanState.COMPLETE, skipped.mineralScan());
        assertEquals(0L, skipped.rebootCompleteGameTime());
        assertEquals(0L, skipped.mineralScanNextCueGameTime());
        assertEquals(0L, skipped.sublightIgnitionCompleteGameTime());
        assertTrue(skipped.canTravelBetweenSystems());
        assertSame(skipped, skipped.debugCompletePrologue());
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
    void sublightIgnitionIsServerTimedAndIdempotent()
    {
        SharedShipProgress ready = SharedShipProgress.newWorld()
                .beginCoreReboot(0L, 1L)
                .finishCoreRebootIfDue(1L)
                .activateSurfaceMission()
                .completeSurfaceMission()
                .beginMineralScan(1L, 1L)
                .advanceMineralScanIfDue(2L, 1L, 1L)
                .advanceMineralScanIfDue(3L, 1L, 1L)
                .advanceMineralScanIfDue(4L, 1L, 1L);

        SharedShipProgress igniting = ready.beginSublightIgnition(100L, 60L);
        assertEquals(EngineState.IGNITING, igniting.sublightEngine());
        assertEquals(160L, igniting.sublightIgnitionCompleteGameTime());
        assertFalse(igniting.canTravelWithinSystem());
        assertSame(igniting, igniting.beginSublightIgnition(110L, 60L));
        assertSame(igniting, igniting.finishSublightIgnitionIfDue(159L));

        SharedShipProgress online = igniting.finishSublightIgnitionIfDue(160L);
        assertEquals(EngineState.ONLINE, online.sublightEngine());
        assertEquals(0L, online.sublightIgnitionCompleteGameTime());
        assertTrue(online.canTravelWithinSystem());
        assertSame(online, online.finishSublightIgnitionIfDue(161L));
    }

    @Test
    void sublightIgnitionDeadlineSurvivesSaveAndLoad()
    {
        SharedShipProgress ready = SharedShipProgress.newWorld()
                .beginCoreReboot(0L, 1L)
                .finishCoreRebootIfDue(1L)
                .activateSurfaceMission()
                .completeSurfaceMission()
                .beginMineralScan(1L, 1L)
                .advanceMineralScanIfDue(2L, 1L, 1L)
                .advanceMineralScanIfDue(3L, 1L, 1L)
                .advanceMineralScanIfDue(4L, 1L, 1L);

        SharedShipProgress.LoadResult loaded = SharedShipProgress.load(
                ready.beginSublightIgnition(500L, 60L).save());

        assertEquals(EngineState.IGNITING, loaded.state().sublightEngine());
        assertEquals(560L, loaded.state().sublightIgnitionCompleteGameTime());
        assertFalse(loaded.requiresSave());
    }

    @Test
    void ignitionWithIncompleteScanFailsClosedOnLoad()
    {
        SharedShipProgress ready = SharedShipProgress.newWorld()
                .beginCoreReboot(0L, 1L)
                .finishCoreRebootIfDue(1L)
                .activateSurfaceMission()
                .completeSurfaceMission()
                .beginMineralScan(1L, 1L)
                .advanceMineralScanIfDue(2L, 1L, 1L)
                .advanceMineralScanIfDue(3L, 1L, 1L)
                .advanceMineralScanIfDue(4L, 1L, 1L);
        CompoundTag malformed = ready.beginSublightIgnition(100L, 60L).save();
        malformed.putString("MineralScan", MineralScanState.LOCKED.id());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SharedShipProgress.load(malformed));
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
        CompoundTag tag = SharedShipProgress.newWorld().debugCompletePrologue().save();
        tag.putString("SublightEngine", EngineState.DAMAGED.id());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SharedShipProgress.load(tag));
    }

    @Test
    void enginesLoadedBeforeFirstLandingFailClosed()
    {
        CompoundTag tag = SharedShipProgress.newWorld().debugCompletePrologue().save();
        tag.putString("SurfaceMission", SurfaceMissionState.LOCKED.id());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SharedShipProgress.load(tag));
    }

    @Test
    void onlyTheCurrentExplicitVersionCanBeLoaded()
    {
        CompoundTag saved = SharedShipProgress.newWorld().save();
        assertEquals(saved, SharedShipProgress.load(saved).state().save());
        CompoundTag missing = saved.copy();
        missing.remove("Version");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SharedShipProgress.load(missing));
        CompoundTag wrongType = saved.copy();
        wrongType.putString("Version", Integer.toString(SharedShipProgress.CURRENT_SCHEMA_VERSION));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SharedShipProgress.load(wrongType));
        for (int version : new int[]{SharedShipProgress.CURRENT_SCHEMA_VERSION - 1, SharedShipProgress.CURRENT_SCHEMA_VERSION + 1}) {
            CompoundTag unsupported = saved.copy();
            unsupported.putInt("Version", version);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SharedShipProgress.load(unsupported));
        }
    }
}
