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
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final String VERSION_TAG = "Version";
    private static final String REVISION_TAG = "Revision";
    private static final String CORE_TAG = "Core";
    private static final String SURFACE_MISSION_TAG = "SurfaceMission";
    private static final String SUBLIGHT_ENGINE_TAG = "SublightEngine";
    private static final String HYPERDRIVE_TAG = "Hyperdrive";
    private static final String REBOOT_COMPLETE_AT_TAG = "RebootCompleteAt";

    private final int schemaVersion;
    private final long revision;
    private final CoreState core;
    private final SurfaceMissionState surfaceMission;
    private final EngineState sublightEngine;
    private final EngineState hyperdrive;
    private final long rebootCompleteGameTime;
    private final CompoundTag preservedFutureData;

    private SharedShipProgress(int schemaVersion, long revision, CoreState core,
                               SurfaceMissionState surfaceMission, EngineState sublightEngine,
                               EngineState hyperdrive, long rebootCompleteGameTime,
                               CompoundTag preservedFutureData)
    {
        this.schemaVersion = schemaVersion;
        this.revision = Math.max(0L, revision);
        this.core = Objects.requireNonNull(core, "core");
        this.surfaceMission = Objects.requireNonNull(surfaceMission, "surfaceMission");
        this.sublightEngine = Objects.requireNonNull(sublightEngine, "sublightEngine");
        this.hyperdrive = Objects.requireNonNull(hyperdrive, "hyperdrive");
        this.rebootCompleteGameTime = Math.max(0L, rebootCompleteGameTime);
        this.preservedFutureData = preservedFutureData == null ? null : preservedFutureData.copy();
    }

    /** New worlds begin in the confirmed emergency-offline prologue state. */
    public static SharedShipProgress newWorld()
    {
        return current(0L, CoreState.OFFLINE, SurfaceMissionState.LOCKED,
                EngineState.DAMAGED, EngineState.DAMAGED, 0L);
    }

    /** Existing worlds without a Story tag retain every previously available travel feature. */
    public static SharedShipProgress legacyUnlocked()
    {
        return current(1L, CoreState.ONLINE, SurfaceMissionState.COMPLETE,
                EngineState.ONLINE, EngineState.ONLINE, 0L);
    }

    public static LoadResult load(CompoundTag tag)
    {
        Objects.requireNonNull(tag, "tag");
        int version = tag.contains(VERSION_TAG, Tag.TAG_INT) ? tag.getInt(VERSION_TAG) : 0;
        if (version > CURRENT_SCHEMA_VERSION)
        {
            SharedShipProgress protectedState = new SharedShipProgress(
                    version, 0L, CoreState.OFFLINE, SurfaceMissionState.LOCKED,
                    EngineState.DAMAGED, EngineState.DAMAGED, 0L, tag);
            return new LoadResult(protectedState, false);
        }

        boolean requiresSave = version != CURRENT_SCHEMA_VERSION;
        long revision = tag.contains(REVISION_TAG, Tag.TAG_LONG) ? tag.getLong(REVISION_TAG) : 0L;
        if (revision < 0L)
        {
            revision = 0L;
            requiresSave = true;
        }

        CoreState core = CoreState.fromId(tag.getString(CORE_TAG), null);
        if (core == null)
        {
            core = CoreState.OFFLINE;
            requiresSave = true;
        }
        SurfaceMissionState mission = SurfaceMissionState.fromId(tag.getString(SURFACE_MISSION_TAG), null);
        if (mission == null)
        {
            mission = SurfaceMissionState.LOCKED;
            requiresSave = true;
        }
        EngineState sublight = EngineState.fromId(tag.getString(SUBLIGHT_ENGINE_TAG), null);
        if (sublight == null)
        {
            sublight = EngineState.DAMAGED;
            requiresSave = true;
        }
        EngineState hyperdrive = EngineState.fromId(tag.getString(HYPERDRIVE_TAG), null);
        if (hyperdrive == null)
        {
            hyperdrive = EngineState.DAMAGED;
            requiresSave = true;
        }
        long rebootCompleteAt = tag.contains(REBOOT_COMPLETE_AT_TAG, Tag.TAG_LONG)
                ? tag.getLong(REBOOT_COMPLETE_AT_TAG) : 0L;
        if (rebootCompleteAt < 0L)
        {
            rebootCompleteAt = 0L;
            requiresSave = true;
        }

        // A server interruption must not leave the shared ship permanently rebooting.
        if (core == CoreState.REBOOTING)
        {
            core = CoreState.ONLINE;
            rebootCompleteAt = 0L;
            revision = increment(revision);
            requiresSave = true;
        }
        else if (rebootCompleteAt != 0L)
        {
            rebootCompleteAt = 0L;
            requiresSave = true;
        }

        // Invalid combinations fail closed instead of granting skipped progression.
        if (core == CoreState.OFFLINE)
        {
            if (mission != SurfaceMissionState.LOCKED
                    || sublight != EngineState.DAMAGED || hyperdrive != EngineState.DAMAGED)
            {
                mission = SurfaceMissionState.LOCKED;
                sublight = EngineState.DAMAGED;
                hyperdrive = EngineState.DAMAGED;
                requiresSave = true;
            }
        }
        if (hyperdrive == EngineState.ONLINE && sublight != EngineState.ONLINE)
        {
            hyperdrive = EngineState.DAMAGED;
            requiresSave = true;
        }
        if (mission != SurfaceMissionState.COMPLETE
                && (sublight == EngineState.ONLINE || hyperdrive == EngineState.ONLINE))
        {
            sublight = EngineState.DAMAGED;
            hyperdrive = EngineState.DAMAGED;
            requiresSave = true;
        }

        return new LoadResult(current(revision, core, mission, sublight, hyperdrive, rebootCompleteAt),
                requiresSave);
    }

    public CompoundTag save()
    {
        if (preservedFutureData != null)
            return preservedFutureData.copy();

        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_TAG, CURRENT_SCHEMA_VERSION);
        tag.putLong(REVISION_TAG, revision);
        tag.putString(CORE_TAG, core.id());
        tag.putString(SURFACE_MISSION_TAG, surfaceMission.id());
        tag.putString(SUBLIGHT_ENGINE_TAG, sublightEngine.id());
        tag.putString(HYPERDRIVE_TAG, hyperdrive.id());
        tag.putLong(REBOOT_COMPLETE_AT_TAG, rebootCompleteGameTime);
        return tag;
    }

    public SharedShipProgress beginCoreReboot(long gameTime, long durationTicks)
    {
        if (!isWritable() || core != CoreState.OFFLINE)
            return this;
        long start = Math.max(0L, gameTime);
        long duration = Math.max(1L, durationTicks);
        long completion = start > Long.MAX_VALUE - duration ? Long.MAX_VALUE : start + duration;
        return changed(CoreState.REBOOTING, surfaceMission, sublightEngine, hyperdrive, completion);
    }

    public SharedShipProgress finishCoreRebootIfDue(long gameTime)
    {
        if (!isWritable() || core != CoreState.REBOOTING || gameTime < rebootCompleteGameTime)
            return this;
        return changed(CoreState.ONLINE, surfaceMission, sublightEngine, hyperdrive, 0L);
    }

    public SharedShipProgress activateSurfaceMission()
    {
        if (!isWritable() || core != CoreState.ONLINE || surfaceMission != SurfaceMissionState.LOCKED)
            return this;
        return changed(core, SurfaceMissionState.ACTIVE, sublightEngine, hyperdrive, 0L);
    }

    public SharedShipProgress completeSurfaceMission()
    {
        if (!isWritable() || surfaceMission != SurfaceMissionState.ACTIVE)
            return this;
        return changed(core, SurfaceMissionState.COMPLETE, sublightEngine, hyperdrive, 0L);
    }

    public SharedShipProgress restoreSublightEngine()
    {
        if (!isWritable() || core != CoreState.ONLINE
                || surfaceMission != SurfaceMissionState.COMPLETE
                || sublightEngine == EngineState.ONLINE)
            return this;
        return changed(core, surfaceMission, EngineState.ONLINE, hyperdrive, 0L);
    }

    public SharedShipProgress restoreHyperdrive()
    {
        if (!isWritable() || core != CoreState.ONLINE || sublightEngine != EngineState.ONLINE
                || hyperdrive == EngineState.ONLINE)
            return this;
        return changed(core, surfaceMission, sublightEngine, EngineState.ONLINE, 0L);
    }

    public int schemaVersion()
    {
        return schemaVersion;
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

    public boolean isWritable()
    {
        return preservedFutureData == null;
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
                nextRebootCompleteAt);
    }

    private static SharedShipProgress current(long revision, CoreState core,
                                              SurfaceMissionState mission, EngineState sublight,
                                              EngineState hyperdrive, long rebootCompleteAt)
    {
        return new SharedShipProgress(CURRENT_SCHEMA_VERSION, revision, core, mission, sublight,
                hyperdrive, rebootCompleteAt, null);
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
