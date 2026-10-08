package com.starboundmc.warp;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.story.SharedShipProgress;
import com.starboundmc.world.universe.BuiltInUniverse;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

/** Persistent shared ship authority. Unknown authored body ids survive datapack removal unchanged. */
public class ShipStateData extends SavedData
{
    public static final String NAME = "starboundmc_ship";
    public static final int MAX_FUEL = 1000;
    /** Internal pre-release layouts before schema 3 are unsupported. */
    public static final int SCHEMA_VERSION = 3;
    private static final int MAX_VISITED_ENTRIES = 1024;
    private static final int MAX_ENTRY_ID_LENGTH = 128;
    private static final SavedData.Factory<ShipStateData> FACTORY =
            new SavedData.Factory<>(ShipStateData::new, ShipStateData::load);

    private int fuel = MAX_FUEL;
    private final Set<String> visited = new LinkedHashSet<>();
    private String currentEntryId = BuiltInUniverse.STARTER_BODY_ID;
    private SharedShipProgress storyProgress = SharedShipProgress.newWorld();

    // Complete authoritative virtual-flight snapshot.
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
        requireTag(tag, "SchemaVersion", Tag.TAG_INT);
        if (tag.getInt("SchemaVersion") != SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported pre-release ship schema: " + tag.getInt("SchemaVersion"));
        for (String key : new String[]{"Fuel", "FlightElapsed", "FlightTotal"}) requireTag(tag, key, Tag.TAG_INT);
        for (String key : new String[]{"CurrentEntry", "FlightTarget", "FlightPhase"}) requireTag(tag, key, Tag.TAG_STRING);
        for (String key : new String[]{"ShipSectorX", "ShipSectorY", "ShipSectorZ"}) requireTag(tag, key, Tag.TAG_LONG);
        for (String key : new String[]{"ShipLocalX", "ShipLocalY", "ShipLocalZ", "ShipVelocityX", "ShipVelocityY", "ShipVelocityZ", "ShipYaw", "ShipPitch", "ShipRoll"}) requireTag(tag, key, Tag.TAG_DOUBLE);
        requireTag(tag, "FlightActive", Tag.TAG_BYTE);
        requireTag(tag, "Visited", Tag.TAG_LIST);
        requireTag(tag, "Story", Tag.TAG_COMPOUND);
        ShipStateData data = new ShipStateData();
        data.fuel = tag.getInt("Fuel");
        if (data.fuel < 0 || data.fuel > MAX_FUEL) throw new IllegalArgumentException("Invalid ship fuel");
        ListTag list = (ListTag) tag.get("Visited");
        if (list.size() > MAX_VISITED_ENTRIES || (!list.isEmpty() && list.getElementType() != Tag.TAG_STRING))
            throw new IllegalArgumentException("Invalid visited body list");
        for (int i = 0; i < list.size(); i++) data.visited.add(requireEntryId(list.getString(i)));
        data.currentEntryId = requireEntryId(tag.getString("CurrentEntry"));
        SharedShipProgress.LoadResult story = SharedShipProgress.load(tag.getCompound("Story"));
        data.storyProgress = story.state();
        data.setFlight(tag.getBoolean("FlightActive"), tag.getString("FlightTarget").isEmpty() ? null : tag.getString("FlightTarget"),
                tag.getInt("FlightElapsed"), tag.getInt("FlightTotal"), FlightPhase.valueOf(tag.getString("FlightPhase")),
                UniversePosition.of(new SectorCoordinate(tag.getLong("ShipSectorX"), tag.getLong("ShipSectorY"), tag.getLong("ShipSectorZ")),
                        tag.getDouble("ShipLocalX"), tag.getDouble("ShipLocalY"), tag.getDouble("ShipLocalZ")),
                new UniverseDelta(tag.getDouble("ShipVelocityX"), tag.getDouble("ShipVelocityY"), tag.getDouble("ShipVelocityZ")),
                tag.getDouble("ShipYaw"), tag.getDouble("ShipPitch"), tag.getDouble("ShipRoll"));
        data.setDirty(story.requiresSave());
        return data;
    }

    private static void requireTag(CompoundTag tag, String key, int type)
    {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing or malformed ship field: " + key);
    }

    public static ShipStateData load(CompoundTag tag)
    {
        return load(tag, HolderLookup.Provider.create(java.util.stream.Stream.empty()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putInt("SchemaVersion", SCHEMA_VERSION);
        tag.putInt("Fuel", fuel);
        ListTag visitedList = new ListTag();
        for (String entryId : visited)
        {
            visitedList.add(StringTag.valueOf(entryId));
        }
        tag.put("Visited", visitedList);
        tag.putString("CurrentEntry", currentEntryId);
        tag.put("Story", storyProgress.save());
        tag.putBoolean("FlightActive", flightActive);
        tag.putString("FlightTarget", flightTargetEntryId == null ? "" : flightTargetEntryId);
        tag.putInt("FlightElapsed", flightElapsedTicks);
        tag.putInt("FlightTotal", flightTotalTicks);
        tag.putString("FlightPhase", flightPhase.name());
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
        tag.putDouble("ShipYaw", shipYaw);
        tag.putDouble("ShipPitch", shipPitch);
        tag.putDouble("ShipRoll", shipRoll);
        return tag;
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
        String id = requireEntryId(entryId);
        if (visited.contains(id)) return;
        if (visited.size() >= MAX_VISITED_ENTRIES) throw new IllegalStateException("Visited body limit exceeded");
        visited.add(id);
        setDirty();
    }

    public String getCurrentEntryId()
    {
        return currentEntryId;
    }

    public void setCurrentEntryId(String entryId)
    {
        this.currentEntryId = requireEntryId(entryId);
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

    public UniversePosition getShipUniversePosition() { return shipPosition; }
    public UniverseDelta getShipVelocity() { return shipVelocity; }
    public double getShipYaw() { return shipYaw; }
    public double getShipPitch() { return shipPitch; }
    public double getShipRoll() { return shipRoll; }

    /** Atomically persists a sector-aware virtual-flight snapshot. */
    public void setFlight(boolean active, String targetEntryId, int elapsedTicks, int totalTicks,
                          FlightPhase phase, UniversePosition position, UniverseDelta velocity,
                          double yaw, double pitch, double roll)
    {
        java.util.Objects.requireNonNull(phase, "phase");
        if (elapsedTicks < 0 || totalTicks < 0 || elapsedTicks > totalTicks
                || active && (phase == FlightPhase.DOCKED || totalTicks == 0 || targetEntryId == null)
                || !active && (phase != FlightPhase.DOCKED || targetEntryId != null || elapsedTicks != 0 || totalTicks != 0))
            throw new IllegalArgumentException("Inconsistent ship flight state");
        if (!Double.isFinite(yaw) || !Double.isFinite(pitch) || !Double.isFinite(roll))
            throw new IllegalArgumentException("Ship attitude must be finite");
        String target = active ? requireEntryId(targetEntryId) : null;
        java.util.Objects.requireNonNull(position, "position");
        java.util.Objects.requireNonNull(velocity, "velocity");
        this.flightActive = active;
        this.flightTargetEntryId = target;
        this.flightTotalTicks = totalTicks;
        this.flightElapsedTicks = elapsedTicks;
        this.flightPhase = phase;
        this.shipPosition = position;
        this.shipVelocity = velocity;
        this.shipYaw = yaw;
        this.shipPitch = pitch;
        this.shipRoll = roll;
        this.setDirty();
    }

    private static String requireEntryId(String value)
    {
        String id = safeEntryId(value);
        if (id == null || !id.equals(value)) throw new IllegalArgumentException("Invalid body entry id: " + value);
        return id;
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

    private boolean applyStoryProgress(SharedShipProgress updated)
    {
        if (updated == storyProgress)
            return false;
        storyProgress = updated;
        this.setDirty();
        return true;
    }
}
