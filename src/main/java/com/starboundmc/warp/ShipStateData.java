package com.starboundmc.warp;

import com.mojang.logging.LogUtils;
import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.story.SharedShipProgress;
import com.starboundmc.world.universe.LegacyUniverseCompatibility;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Server-persistent state for the shared ship: the fuel tank, the star-map
 * entries visited so far, and the exact body the ship is currently at.
 *
 * <p>The ship's location is stored as a universe <b>entry id</b>
 * ({@code CurrentEntry}), and that id is the single authority. Older saves only
 * named one of the four legacy planets, so they are migrated on load by an
 * explicit planet-to-entry mapping.</p>
 *
 * <p>An id this build does not recognise is preserved verbatim rather than
 * rewritten. The legacy enum silently resolved anything unknown to the starter
 * planet, which would quietly teleport a player who had a datapack body into
 * their save; keeping the original id means removing and re-adding the datapack
 * restores their position, and the WARN log says what happened.</p>
 */
public class ShipStateData extends SavedData
{
    public static final String NAME = "starboundmc_ship";
    public static final int MAX_FUEL = 1000;
    /**
     * Save layout version. 1 (or absent) is the legacy era where only a
     * {@code Planet} name was stored; 2 stores {@code CurrentEntry} as the
     * authority and keeps {@code Planet} only as a derived compatibility field.
     */
    public static final int SCHEMA_VERSION = 2;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_VISITED_ENTRIES = 1024;
    private static final int MAX_ENTRY_ID_LENGTH = 128;
    private static final SavedData.Factory<ShipStateData> FACTORY =
            new SavedData.Factory<>(ShipStateData::new, ShipStateData::load);

    /** Layout version this instance was loaded from; SCHEMA_VERSION for a new one. */
    private int loadedSchemaVersion = SCHEMA_VERSION;
    private int fuel = MAX_FUEL;
    private final Set<String> visited = new LinkedHashSet<>();
    /** The authoritative location. Null means "never set", not "unknown body". */
    private String currentEntryId = null;
    private SharedShipProgress storyProgress = SharedShipProgress.newWorld();

    // Virtual-flight state. Old saves have none and therefore load docked.
    private boolean flightActive = false;
    private String flightTargetEntryId = null;
    private int flightElapsedTicks = 0;
    private int flightTotalTicks = 0;
    private FlightPhase flightPhase = FlightPhase.DOCKED;
    private UniversePosition shipPosition = UniversePosition.of(0.0, 102.0, 0.0);
    private UniverseDelta shipVelocity = new UniverseDelta(0.0, 0.0, 0.0);
    private double shipYaw = 0.0;
    private double shipPitch = 0.0;
    private double shipRoll = 0.0;

    public static ShipStateData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage()
                .computeIfAbsent(FACTORY, NAME);
    }

    public static ShipStateData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        ShipStateData data = new ShipStateData();
        int schemaVersion = tag.contains("SchemaVersion", Tag.TAG_INT)
                ? tag.getInt("SchemaVersion") : 1;
        data.loadedSchemaVersion = schemaVersion;

        // Fuel: old/partially-written saves may lack the tag; start them full
        // instead of reading the missing int as 0.
        data.fuel = tag.contains("Fuel", Tag.TAG_INT)
                ? clamp(tag.getInt("Fuel"), 0, MAX_FUEL) : MAX_FUEL;
        if (tag.contains("Visited", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("Visited", Tag.TAG_STRING);
            for (int i = 0; i < list.size() && data.visited.size() < MAX_VISITED_ENTRIES; i++)
            {
                String entry = safeEntryId(list.getString(i));
                if (entry != null)
                    data.visited.add(entry);
            }
        }

        String storedEntry = tag.contains("CurrentEntry", Tag.TAG_STRING)
                ? safeEntryId(tag.getString("CurrentEntry")) : null;
        if (storedEntry != null)
        {
            // A shape check only. Whether the universe still contains the body is
            // decided by the caller, which is the only place that has a catalog.
            data.currentEntryId = storedEntry;
        }
        else
        {
            data.currentEntryId = migratedEntryId(tag, schemaVersion);
        }

        if (!tag.contains("Story"))
        {
            // Existing worlds predate the prologue and must retain all travel abilities.
            data.storyProgress = SharedShipProgress.legacyUnlocked();
            data.setDirty();
        }
        else if (tag.contains("Story", Tag.TAG_COMPOUND))
        {
            SharedShipProgress.LoadResult loadedStory =
                    SharedShipProgress.load(tag.getCompound("Story"));
            data.storyProgress = loadedStory.state();
            if (loadedStory.requiresSave())
                data.setDirty();
        }
        else
        {
            // A present but malformed Story tag is corruption, not a legacy save.
            data.storyProgress = SharedShipProgress.newWorld();
            data.setDirty();
        }
        data.flightActive = tag.getBoolean("FlightActive");
        data.flightTargetEntryId = tag.contains("FlightTarget", Tag.TAG_STRING)
                ? safeEntryId(tag.getString("FlightTarget")) : null;
        data.flightTotalTicks = Math.max(0, tag.getInt("FlightTotal"));
        data.flightElapsedTicks = clamp(tag.getInt("FlightElapsed"), 0, data.flightTotalTicks);
        if (tag.contains("FlightPhaseName", Tag.TAG_STRING))
        {
            try { data.flightPhase = FlightPhase.valueOf(tag.getString("FlightPhaseName")); }
            catch (IllegalArgumentException ignored) { data.flightPhase = FlightPhase.DOCKED; }
        }
        else
        {
            // Migration from protocol-2 saves, before TURN was inserted into the enum.
            data.flightPhase = switch (tag.getInt("FlightPhase"))
            {
                case 1 -> FlightPhase.ACCELERATE;
                case 2 -> FlightPhase.HYPERSPACE;
                case 3 -> FlightPhase.DECELERATE;
                case 4 -> FlightPhase.ARRIVE;
                default -> FlightPhase.DOCKED;
            };
        }
        data.shipPosition = loadShipPosition(tag, data.currentEntryId);
        data.shipVelocity = new UniverseDelta(
                finiteDoubleOrDefault(tag, "ShipVelocityX", 0.0),
                finiteDoubleOrDefault(tag, "ShipVelocityY", 0.0),
                finiteDoubleOrDefault(tag, "ShipVelocityZ", 0.0));
        data.shipYaw = finiteDoubleOrDefault(tag, "ShipYaw", 0.0);
        data.shipPitch = finiteDoubleOrDefault(tag, "ShipPitch", 0.0);
        data.shipRoll = finiteDoubleOrDefault(tag, "ShipRoll", 0.0);
        if (data.flightActive && (data.flightTargetEntryId == null || data.flightTotalTicks == 0))
        {
            data.flightActive = false;
            data.flightTargetEntryId = null;
            data.flightElapsedTicks = 0;
            data.flightTotalTicks = 0;
            data.flightPhase = FlightPhase.DOCKED;
        }
        // A save written before this class stored an entry id needs rewriting in
        // the new layout, so the next write is not still version 1.
        if (schemaVersion < SCHEMA_VERSION)
            data.setDirty();
        return data;
    }

    /**
     * Derives the location of a save written before {@code CurrentEntry} existed.
     *
     * <p>The legacy planet name is mapped explicitly instead of through
     * {@code Planet.fromId}, which answers the starter planet for any unrecognised
     * string. A save naming a planet that no longer exists must not be silently
     * relocated to the starting world; it falls through to null and the caller
     * reports the loss.</p>
     */
    private static String migratedEntryId(CompoundTag tag, int schemaVersion)
    {
        if (!tag.contains("Planet", Tag.TAG_STRING))
        {
            if (schemaVersion >= SCHEMA_VERSION)
            {
                LOGGER.warn("Ship save is schema {} but has no location; it will be reset "
                        + "to the starting body.", schemaVersion);
            }
            return null;
        }

        String legacyName = tag.getString("Planet");
        String entryId = LegacyUniverseCompatibility.parsePlanetId(legacyName).orElse(null);
        if (entryId == null)
        {
            // Do NOT fall back to the starter body here: that would look like a
            // successful migration and hide the fact that the location is gone.
            LOGGER.warn("Ship save names an unknown legacy planet '{}'; the location will be "
                    + "reset to the starting body.", legacyName);
            return null;
        }
        LOGGER.info("Migrated legacy ship save from planet '{}' to entry '{}'.",
                legacyName, entryId);
        return entryId;
    }

    public static ShipStateData load(CompoundTag tag)
    {
        return load(tag, HolderLookup.Provider.create(java.util.stream.Stream.empty()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putInt("SchemaVersion", SCHEMA_VERSION);
        // The legacy field is written as a derived view of the authoritative id, so
        // an older build can still read the save. A body with no legacy planet
        // writes an empty string rather than a wrong name.
        // The compatibility field is a derived view of the authoritative id. A
        // body with no legacy name writes empty rather than a wrong name.
        tag.putString("Planet", legacyPlanetName() == null ? "" : legacyPlanetName());
        tag.putInt("Fuel", fuel);
        ListTag visitedList = new ListTag();
        for (String entryId : visited)
        {
            visitedList.add(StringTag.valueOf(entryId));
        }
        tag.put("Visited", visitedList);
        tag.putString("CurrentEntry", currentEntryId == null ? "" : currentEntryId);
        tag.put("Story", storyProgress.save());
        tag.putBoolean("FlightActive", flightActive);
        tag.putString("FlightTarget", flightTargetEntryId == null ? "" : flightTargetEntryId);
        tag.putInt("FlightElapsed", flightElapsedTicks);
        tag.putInt("FlightTotal", flightTotalTicks);
        tag.putInt("FlightPhase", flightPhase.ordinal());
        tag.putString("FlightPhaseName", flightPhase.name());
        SectorCoordinate sector = shipPosition.sector();
        tag.putLong("ShipSectorX", sector.x());
        tag.putLong("ShipSectorY", sector.y());
        tag.putLong("ShipSectorZ", sector.z());
        tag.putDouble("ShipLocalX", shipPosition.localX());
        tag.putDouble("ShipLocalY", shipPosition.localY());
        tag.putDouble("ShipLocalZ", shipPosition.localZ());
        tag.putDouble("ShipVelocityX", shipVelocity.x());
        tag.putDouble("ShipVelocityY", shipVelocity.y());
        tag.putDouble("ShipVelocityZ", shipVelocity.z());
        // Keep the old absolute fields while older builds may still read this save.
        tag.putDouble("ShipX", legacyCoordinate(sector.x(), shipPosition.localX()));
        tag.putDouble("ShipY", legacyCoordinate(sector.y(), shipPosition.localY()));
        tag.putDouble("ShipZ", legacyCoordinate(sector.z(), shipPosition.localZ()));
        tag.putDouble("ShipYaw", shipYaw);
        tag.putDouble("ShipPitch", shipPitch);
        tag.putDouble("ShipRoll", shipRoll);
        return tag;
    }

    /**
     * The legacy planet name for the current location, or null.
     *
     * <p>Derived, never stored: a body added by a datapack has no legacy name, and
     * a second stored copy could disagree with the authoritative id. Callers must
     * handle null rather than assume the starter body.</p>
     */
    public String legacyPlanetName()
    {
        return LegacyUniverseCompatibility.legacyPlanetName(currentEntryId).orElse(null);
    }

    /**
     * The layout version this instance was loaded from.
     *
     * <p>Equal to {@link #SCHEMA_VERSION} for a new or already-migrated instance,
     * lower for a save still in the legacy layout. Exposed so a migration decision
     * can be inspected and tested rather than inferred from which tags exist.</p>
     */
    public int getSchemaVersion()
    {
        return loadedSchemaVersion;
    }

    public int getFuel()
    {
        return fuel;
    }

    public void setFuel(int fuel)
    {
        this.fuel = clamp(fuel, 0, MAX_FUEL);
        this.setDirty();
    }

    public Set<String> getVisited()
    {
        return java.util.Collections.unmodifiableSet(visited);
    }

    public boolean isVisited(String entryId)
    {
        return entryId != null && visited.contains(entryId);
    }

    public void markVisited(String entryId)
    {
        String safeEntry = safeEntryId(entryId);
        if (safeEntry != null && visited.size() < MAX_VISITED_ENTRIES && visited.add(safeEntry))
        {
            this.setDirty();
        }
    }

    public String getCurrentEntryId()
    {
        return currentEntryId;
    }

    public void setCurrentEntryId(String entryId)
    {
        this.currentEntryId = safeEntryId(entryId);
        this.setDirty();
    }

    public SharedShipProgress getStoryProgress()
    {
        return storyProgress;
    }

    public boolean beginCoreReboot(long gameTime, long durationTicks)
    {
        return applyStoryProgress(storyProgress.beginCoreReboot(gameTime, durationTicks));
    }

    public boolean finishCoreRebootIfDue(long gameTime)
    {
        return applyStoryProgress(storyProgress.finishCoreRebootIfDue(gameTime));
    }

    public boolean activateSurfaceMission()
    {
        return applyStoryProgress(storyProgress.activateSurfaceMission());
    }

    public boolean completeSurfaceMission()
    {
        return applyStoryProgress(storyProgress.completeSurfaceMission());
    }

    public boolean beginMineralScan(long gameTime, long delayTicks)
    {
        return applyStoryProgress(storyProgress.beginMineralScan(gameTime, delayTicks));
    }

    public boolean advanceMineralScanIfDue(long gameTime,
                                           long resultDelayTicks,
                                           long conclusionDelayTicks)
    {
        return applyStoryProgress(storyProgress.advanceMineralScanIfDue(
                gameTime, resultDelayTicks, conclusionDelayTicks));
    }

    public boolean replayMineralScan(long gameTime, long delayTicks)
    {
        return applyStoryProgress(storyProgress.replayMineralScan(gameTime, delayTicks));
    }

    public boolean restoreSublightEngine()
    {
        return applyStoryProgress(storyProgress.restoreSublightEngine());
    }

    public boolean beginSublightIgnition(long gameTime, long durationTicks)
    {
        return applyStoryProgress(storyProgress.beginSublightIgnition(gameTime, durationTicks));
    }

    public boolean finishSublightIgnitionIfDue(long gameTime)
    {
        return applyStoryProgress(storyProgress.finishSublightIgnitionIfDue(gameTime));
    }

    public boolean restoreHyperdrive()
    {
        return applyStoryProgress(storyProgress.restoreHyperdrive());
    }

    /** Completes the shared prologue for an in-game debug run. */
    public boolean debugCompletePrologue()
    {
        return applyStoryProgress(storyProgress.debugCompletePrologue());
    }

    public boolean isFlightActive()
    {
        return flightActive;
    }

    public String getFlightTargetEntryId()
    {
        return flightTargetEntryId;
    }

    public int getFlightElapsedTicks()
    {
        return flightElapsedTicks;
    }

    public int getFlightTotalTicks()
    {
        return flightTotalTicks;
    }

    public FlightPhase getFlightPhase()
    {
        return flightPhase;
    }

    /** Local-coordinate compatibility accessors used by the current Vec3 flight controller. */
    public double getShipX() { return shipPosition.localX(); }
    public double getShipY() { return shipPosition.localY(); }
    public double getShipZ() { return shipPosition.localZ(); }
    public UniversePosition getShipUniversePosition() { return shipPosition; }
    public UniverseDelta getShipVelocity() { return shipVelocity; }
    public double getShipYaw() { return shipYaw; }
    public double getShipPitch() { return shipPitch; }
    public double getShipRoll() { return shipRoll; }

    /** Atomically persists a complete virtual-flight snapshot. */
    public void setFlight(boolean active, String targetEntryId, int elapsedTicks, int totalTicks,
                          FlightPhase phase, net.minecraft.world.phys.Vec3 position,
                          double yaw, double pitch, double roll)
    {
        setFlight(active, targetEntryId, elapsedTicks, totalTicks, phase,
                UniversePosition.fromLegacy(position), new UniverseDelta(0.0, 0.0, 0.0),
                yaw, pitch, roll);
    }

    /** Atomically persists a sector-aware virtual-flight snapshot. */
    public void setFlight(boolean active, String targetEntryId, int elapsedTicks, int totalTicks,
                          FlightPhase phase, UniversePosition position, UniverseDelta velocity,
                          double yaw, double pitch, double roll)
    {
        this.flightActive = active;
        this.flightTargetEntryId = safeEntryId(targetEntryId);
        this.flightTotalTicks = Math.max(0, totalTicks);
        this.flightElapsedTicks = clamp(elapsedTicks, 0, this.flightTotalTicks);
        this.flightPhase = phase == null ? FlightPhase.DOCKED : phase;
        this.shipPosition = java.util.Objects.requireNonNull(position, "position");
        this.shipVelocity = java.util.Objects.requireNonNull(velocity, "velocity");
        this.shipYaw = finiteOrDefault(yaw, 0.0);
        this.shipPitch = finiteOrDefault(pitch, 0.0);
        this.shipRoll = finiteOrDefault(roll, 0.0);
        this.setDirty();
    }

    private static UniversePosition loadShipPosition(CompoundTag tag, String entryId)
    {
        boolean hasSectorPosition = tag.contains("ShipSectorX", Tag.TAG_LONG)
                && tag.contains("ShipSectorY", Tag.TAG_LONG)
                && tag.contains("ShipSectorZ", Tag.TAG_LONG)
                && tag.contains("ShipLocalX", Tag.TAG_DOUBLE)
                && tag.contains("ShipLocalY", Tag.TAG_DOUBLE)
                && tag.contains("ShipLocalZ", Tag.TAG_DOUBLE);
        try
        {
            if (hasSectorPosition)
            {
                return UniversePosition.of(
                        new SectorCoordinate(tag.getLong("ShipSectorX"), tag.getLong("ShipSectorY"), tag.getLong("ShipSectorZ")),
                        tag.getDouble("ShipLocalX"), tag.getDouble("ShipLocalY"), tag.getDouble("ShipLocalZ"));
            }

            UniversePosition dock = defaultDock(entryId);
            boolean hasLegacyPosition = tag.contains("ShipX", Tag.TAG_DOUBLE)
                    || tag.contains("ShipY", Tag.TAG_DOUBLE)
                    || tag.contains("ShipZ", Tag.TAG_DOUBLE);
            if (!hasLegacyPosition)
                return dock;
            return UniversePosition.of(
                    finiteDoubleOrDefault(tag, "ShipX", legacyCoordinate(dock.sector().x(), dock.localX())),
                    finiteDoubleOrDefault(tag, "ShipY", legacyCoordinate(dock.sector().y(), dock.localY())),
                    finiteDoubleOrDefault(tag, "ShipZ", legacyCoordinate(dock.sector().z(), dock.localZ())));
        }
        catch (IllegalArgumentException | ArithmeticException ignored)
        {
            return defaultDock(entryId);
        }
    }

    private static double finiteDoubleOrDefault(CompoundTag tag, String key, double fallback)
    {
        if (!tag.contains(key, Tag.TAG_DOUBLE))
            return fallback;
        double value = tag.getDouble(key);
        return Double.isFinite(value) ? value : fallback;
    }

    private static double legacyCoordinate(long sector, double local)
    {
        return (double) sector * UniversePosition.SECTOR_SIZE + local;
    }

    /**
     * Where the ship sits when the save has no usable position.
     *
     * <p>Uses the body's own dock when the universe knows it, so a ship without a
     * stored position still appears at the right place. Falls back to the starter
     * body's dock only for an unknown or missing id, which is the case
     * {@code initialize} then reports.</p>
     */
    private static UniversePosition defaultDock(String entryId)
    {
        try
        {
            if (UniverseNavigation.isNavigable(entryId))
                return UniverseNavigation.universeDock(entryId);
        }
        catch (RuntimeException ignored)
        {
            // A catalog that is not ready yet must not break save loading.
        }
        return UniversePosition.of(0.0, 102.0, 0.0);
    }

    private static String safeEntryId(String value)
    {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.length() > MAX_ENTRY_ID_LENGTH ? null : trimmed;
    }

    private static int clamp(int value, int minimum, int maximum)
    {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double finiteOrDefault(double value, double fallback)
    {
        return Double.isFinite(value) ? value : fallback;
    }

    private boolean applyStoryProgress(SharedShipProgress updated)
    {
        if (updated == storyProgress)
            return false;
        storyProgress = updated;
        this.setDirty();
        return true;
    }
}
