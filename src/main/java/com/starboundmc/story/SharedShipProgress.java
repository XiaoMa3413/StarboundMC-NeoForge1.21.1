package com.starboundmc.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;

/**
 * Immutable, server-authoritative story state for the currently shared ship.
 * The fields form a state vector so core, mission and both engines remain
 * independently persisted without a second world-level data source.
 */
public final class SharedShipProgress
{
    public static final int CURRENT_SCHEMA_VERSION = 3;

    private static final String VERSION_TAG = "Version";
    private static final String REVISION_TAG = "Revision";
    private static final String CORE_TAG = "Core";
    private static final String SURFACE_MISSION_TAG = "SurfaceMission";
    private static final String SUBLIGHT_ENGINE_TAG = "SublightEngine";
    private static final String HYPERDRIVE_TAG = "Hyperdrive";
    private static final String REBOOT_COMPLETE_AT_TAG = "RebootCompleteAt";
    private static final String MINERAL_SCAN_TAG = "MineralScan";
    private static final String MINERAL_SCAN_NEXT_CUE_AT_TAG = "MineralScanNextCueAt";
    private static final String SUBLIGHT_IGNITION_COMPLETE_AT_TAG = "SublightIgnitionCompleteAt";

    private final long revision;
    private final CoreState core;
    private final SurfaceMissionState surfaceMission;
    private final EngineState sublightEngine;
    private final EngineState hyperdrive;
    private final long rebootCompleteGameTime;
    private final MineralScanState mineralScan;
    private final long mineralScanNextCueGameTime;
    private final long sublightIgnitionCompleteGameTime;

    private SharedShipProgress(long revision, CoreState core,
                               SurfaceMissionState surfaceMission, EngineState sublightEngine,
                               EngineState hyperdrive, long rebootCompleteGameTime,
                               MineralScanState mineralScan, long mineralScanNextCueGameTime,
                               long sublightIgnitionCompleteGameTime)
    {
        this.revision = revision;
        this.core = Objects.requireNonNull(core, "core");
        this.surfaceMission = Objects.requireNonNull(surfaceMission, "surfaceMission");
        this.sublightEngine = Objects.requireNonNull(sublightEngine, "sublightEngine");
        this.hyperdrive = Objects.requireNonNull(hyperdrive, "hyperdrive");
        this.rebootCompleteGameTime = rebootCompleteGameTime;
        this.mineralScan = Objects.requireNonNull(mineralScan, "mineralScan");
        this.mineralScanNextCueGameTime = mineralScanNextCueGameTime;
        this.sublightIgnitionCompleteGameTime = sublightIgnitionCompleteGameTime;
    }

    /** New worlds begin in the confirmed emergency-offline prologue state. */
    public static SharedShipProgress newWorld()
    {
        return current(0L, CoreState.OFFLINE, SurfaceMissionState.LOCKED,
                EngineState.DAMAGED, EngineState.DAMAGED, 0L,
                MineralScanState.LOCKED, 0L, 0L);
    }

    public static LoadResult load(CompoundTag tag)
    {
        Objects.requireNonNull(tag, "tag");
        if (!tag.contains(VERSION_TAG, Tag.TAG_INT))
            throw new IllegalArgumentException("Missing or malformed story schema");
        int version = tag.getInt(VERSION_TAG);
        if (version != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported pre-release story schema: " + version);
        for (String key : new String[]{CORE_TAG, SURFACE_MISSION_TAG, SUBLIGHT_ENGINE_TAG, HYPERDRIVE_TAG, MINERAL_SCAN_TAG})
            if (!tag.contains(key, Tag.TAG_STRING)) throw new IllegalArgumentException("Missing story field: " + key);
        for (String key : new String[]{REVISION_TAG, REBOOT_COMPLETE_AT_TAG, MINERAL_SCAN_NEXT_CUE_AT_TAG, SUBLIGHT_IGNITION_COMPLETE_AT_TAG})
            if (!tag.contains(key, Tag.TAG_LONG) || tag.getLong(key) < 0) throw new IllegalArgumentException("Invalid story field: " + key);
        long revision = tag.getLong(REVISION_TAG);
        CoreState core = CoreState.fromId(tag.getString(CORE_TAG), null);
        SurfaceMissionState mission = SurfaceMissionState.fromId(tag.getString(SURFACE_MISSION_TAG), null);
        EngineState sublight = EngineState.fromId(tag.getString(SUBLIGHT_ENGINE_TAG), null);
        EngineState hyperdrive = EngineState.fromId(tag.getString(HYPERDRIVE_TAG), null);
        MineralScanState mineralScan = MineralScanState.fromId(tag.getString(MINERAL_SCAN_TAG), null);
        long rebootCompleteAt = tag.getLong(REBOOT_COMPLETE_AT_TAG);
        long mineralScanNextCueAt = tag.getLong(MINERAL_SCAN_NEXT_CUE_AT_TAG);
        long sublightIgnitionCompleteAt = tag.getLong(SUBLIGHT_IGNITION_COMPLETE_AT_TAG);
        if (core == null || mission == null || sublight == null || hyperdrive == null || mineralScan == null
                || hyperdrive == EngineState.IGNITING
                || (core == CoreState.REBOOTING) != (rebootCompleteAt > 0)
                || (sublight == EngineState.IGNITING) != (sublightIgnitionCompleteAt > 0)
                || mineralScan.isInProgress() != (mineralScanNextCueAt > 0)
                || core != CoreState.ONLINE && (mission != SurfaceMissionState.LOCKED
                    || sublight != EngineState.DAMAGED || hyperdrive != EngineState.DAMAGED || mineralScan != MineralScanState.LOCKED)
                || hyperdrive == EngineState.ONLINE && sublight != EngineState.ONLINE
                || mission != SurfaceMissionState.COMPLETE && (sublight != EngineState.DAMAGED
                    || hyperdrive != EngineState.DAMAGED || mineralScan != MineralScanState.LOCKED)
                || (sublight == EngineState.IGNITING || sublight == EngineState.ONLINE) && mineralScan != MineralScanState.COMPLETE)
            throw new IllegalArgumentException("Inconsistent shared story state");
        // A valid interrupted reboot retains the established recovery policy.
        boolean requiresSave = core == CoreState.REBOOTING;
        if (requiresSave) { core = CoreState.ONLINE; rebootCompleteAt = 0; revision = increment(revision); }

        return new LoadResult(current(revision, core, mission, sublight, hyperdrive,
                        rebootCompleteAt, mineralScan, mineralScanNextCueAt,
                        sublightIgnitionCompleteAt),
                requiresSave);
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_TAG, CURRENT_SCHEMA_VERSION);
        tag.putLong(REVISION_TAG, revision);
        tag.putString(CORE_TAG, core.id());
        tag.putString(SURFACE_MISSION_TAG, surfaceMission.id());
        tag.putString(SUBLIGHT_ENGINE_TAG, sublightEngine.id());
        tag.putString(HYPERDRIVE_TAG, hyperdrive.id());
        tag.putLong(REBOOT_COMPLETE_AT_TAG, rebootCompleteGameTime);
        tag.putString(MINERAL_SCAN_TAG, mineralScan.id());
        tag.putLong(MINERAL_SCAN_NEXT_CUE_AT_TAG, mineralScanNextCueGameTime);
        tag.putLong(SUBLIGHT_IGNITION_COMPLETE_AT_TAG, sublightIgnitionCompleteGameTime);
        return tag;
    }

    public SharedShipProgress beginCoreReboot(long gameTime, long durationTicks)
    {
        if (core != CoreState.OFFLINE)
            return this;
        long start = Math.max(0L, gameTime);
        long duration = Math.max(1L, durationTicks);
        long completion = start > Long.MAX_VALUE - duration ? Long.MAX_VALUE : start + duration;
        return changed(CoreState.REBOOTING, surfaceMission, sublightEngine, hyperdrive, completion);
    }

    public SharedShipProgress finishCoreRebootIfDue(long gameTime)
    {
        if (core != CoreState.REBOOTING || gameTime < rebootCompleteGameTime)
            return this;
        return changed(CoreState.ONLINE, surfaceMission, sublightEngine, hyperdrive, 0L);
    }

    public SharedShipProgress activateSurfaceMission()
    {
        if (core != CoreState.ONLINE || surfaceMission != SurfaceMissionState.LOCKED)
            return this;
        return changed(core, SurfaceMissionState.ACTIVE, sublightEngine, hyperdrive, 0L);
    }

    public SharedShipProgress completeSurfaceMission()
    {
        if (surfaceMission != SurfaceMissionState.ACTIVE)
            return this;
        return changed(core, SurfaceMissionState.COMPLETE, sublightEngine, hyperdrive, 0L);
    }

    public SharedShipProgress beginMineralScan(long gameTime, long delayTicks)
    {
        if (core != CoreState.ONLINE
                || surfaceMission != SurfaceMissionState.COMPLETE
                || mineralScan != MineralScanState.LOCKED)
            return this;
        return changedScan(MineralScanState.PENDING,
                safeAdd(Math.max(0L, gameTime), Math.max(1L, delayTicks)));
    }

    /** Admin-only replay hook; it deliberately leaves every engine untouched. */
    public SharedShipProgress replayMineralScan(long gameTime, long delayTicks)
    {
        if (core != CoreState.ONLINE
                || surfaceMission != SurfaceMissionState.COMPLETE)
            return this;
        return changedScan(MineralScanState.PENDING,
                safeAdd(Math.max(0L, gameTime), Math.max(1L, delayTicks)));
    }

    public SharedShipProgress advanceMineralScanIfDue(long gameTime,
                                                       long resultDelayTicks,
                                                       long conclusionDelayTicks)
    {
        if (!mineralScan.isInProgress()
                || gameTime < mineralScanNextCueGameTime)
            return this;
        return switch (mineralScan)
        {
            case PENDING -> changedScan(MineralScanState.SCANNING,
                    safeAdd(gameTime, Math.max(1L, resultDelayTicks)));
            case SCANNING -> changedScan(MineralScanState.RESULT_REPORTED,
                    safeAdd(gameTime, Math.max(1L, conclusionDelayTicks)));
            case RESULT_REPORTED -> changedScan(MineralScanState.COMPLETE, 0L);
            default -> this;
        };
    }

    public SharedShipProgress restoreSublightEngine()
    {
        if (core != CoreState.ONLINE
                || surfaceMission != SurfaceMissionState.COMPLETE
                || sublightEngine == EngineState.ONLINE)
            return this;
        return current(increment(revision), core, surfaceMission,
                EngineState.ONLINE, hyperdrive, 0L,
                MineralScanState.COMPLETE, 0L, 0L);
    }

    /** Starts the shared, server-timed ignition after one player's payment. */
    public SharedShipProgress beginSublightIgnition(long gameTime, long durationTicks)
    {
        if (core != CoreState.ONLINE
                || surfaceMission != SurfaceMissionState.COMPLETE
                || mineralScan != MineralScanState.COMPLETE
                || sublightEngine != EngineState.DAMAGED)
            return this;
        long start = Math.max(0L, gameTime);
        long duration = Math.max(1L, durationTicks);
        long completion = safeAdd(start, duration);
        return current(increment(revision), core, surfaceMission,
                EngineState.IGNITING, hyperdrive, 0L,
                mineralScan, mineralScanNextCueGameTime, completion);
    }

    public SharedShipProgress finishSublightIgnitionIfDue(long gameTime)
    {
        if (sublightEngine != EngineState.IGNITING
                || gameTime < sublightIgnitionCompleteGameTime)
            return this;
        return current(increment(revision), core, surfaceMission,
                EngineState.ONLINE, hyperdrive, 0L,
                MineralScanState.COMPLETE, 0L, 0L);
    }

    public SharedShipProgress restoreHyperdrive()
    {
        if (core != CoreState.ONLINE || sublightEngine != EngineState.ONLINE
                || hyperdrive == EngineState.ONLINE)
            return this;
        return changed(core, surfaceMission, sublightEngine, EngineState.ONLINE, 0L);
    }

    /**
     * Completes the shared prologue in one atomic debug transition.
     *
     * <p>This deliberately clears any in-progress reboot, survey, or ignition
     * deadline so a debug run cannot leave a later tick to undo the unlocked
     * state.</p>
     */
    public SharedShipProgress debugCompletePrologue()
    {
        if (core == CoreState.ONLINE
                && surfaceMission == SurfaceMissionState.COMPLETE
                && sublightEngine == EngineState.ONLINE
                && hyperdrive == EngineState.ONLINE
                && mineralScan == MineralScanState.COMPLETE
                && rebootCompleteGameTime == 0L
                && sublightIgnitionCompleteGameTime == 0L
                && mineralScanNextCueGameTime == 0L)
            return this;
        return current(increment(revision), CoreState.ONLINE, SurfaceMissionState.COMPLETE,
                EngineState.ONLINE, EngineState.ONLINE, 0L,
                MineralScanState.COMPLETE, 0L, 0L);
    }

    public int schemaVersion()
    {
        return CURRENT_SCHEMA_VERSION;
    }

    public long revision()
    {
        return revision;
    }

    public CoreState core()
    {
        return core;
    }

    public SurfaceMissionState surfaceMission()
    {
        return surfaceMission;
    }

    public EngineState sublightEngine()
    {
        return sublightEngine;
    }

    public EngineState hyperdrive()
    {
        return hyperdrive;
    }

    public long rebootCompleteGameTime()
    {
        return rebootCompleteGameTime;
    }

    public MineralScanState mineralScan()
    {
        return mineralScan;
    }

    public long mineralScanNextCueGameTime()
    {
        return mineralScanNextCueGameTime;
    }

    public long sublightIgnitionCompleteGameTime()
    {
        return sublightIgnitionCompleteGameTime;
    }

    public boolean isSublightIgniting()
    {
        return sublightEngine == EngineState.IGNITING;
    }

    public boolean canUseTeleporter()
    {
        return core == CoreState.ONLINE;
    }

    public boolean canBrowseCurrentSystem()
    {
        return core == CoreState.ONLINE;
    }

    public boolean canTravelWithinSystem()
    {
        return core == CoreState.ONLINE && sublightEngine == EngineState.ONLINE;
    }

    public boolean canTravelBetweenSystems()
    {
        return canTravelWithinSystem() && hyperdrive == EngineState.ONLINE;
    }

    private SharedShipProgress changed(CoreState nextCore, SurfaceMissionState nextMission,
                                       EngineState nextSublight, EngineState nextHyperdrive,
                                       long nextRebootCompleteAt)
    {
        return current(increment(revision), nextCore, nextMission, nextSublight, nextHyperdrive,
                nextRebootCompleteAt, mineralScan, mineralScanNextCueGameTime,
                nextSublight == EngineState.IGNITING ? sublightIgnitionCompleteGameTime : 0L);
    }

    private SharedShipProgress changedScan(MineralScanState nextScan, long nextCueAt)
    {
        return current(increment(revision), core, surfaceMission, sublightEngine, hyperdrive,
                rebootCompleteGameTime, nextScan, nextCueAt,
                sublightEngine == EngineState.IGNITING ? sublightIgnitionCompleteGameTime : 0L);
    }

    private static SharedShipProgress current(long revision, CoreState core,
                                              SurfaceMissionState mission, EngineState sublight,
                                              EngineState hyperdrive, long rebootCompleteAt,
                                              MineralScanState mineralScan, long mineralScanNextCueAt,
                                              long sublightIgnitionCompleteAt)
    {
        return new SharedShipProgress(revision, core, mission, sublight,
                hyperdrive, rebootCompleteAt, mineralScan, mineralScanNextCueAt,
                sublightIgnitionCompleteAt);
    }

    private static long safeAdd(long value, long delta)
    {
        return value > Long.MAX_VALUE - delta ? Long.MAX_VALUE : value + delta;
    }

    private static long increment(long value)
    {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }

    public record LoadResult(SharedShipProgress state, boolean requiresSave)
    {
        public LoadResult
        {
            Objects.requireNonNull(state, "state");
        }
    }
}
