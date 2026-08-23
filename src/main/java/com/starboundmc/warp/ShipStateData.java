package com.starboundmc.warp;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.Planet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Server-persistent state for the shared ship: which planet it orbits, the fuel
 * tank, the star-map entries visited so far, and the exact star-map entry the
 * ship is currently docked at. Stored in the overworld's data storage so it
 * survives restarts and is shared across dimensions.
 */
public class ShipStateData extends SavedData
{
    public static final String NAME = "starboundmc_ship";
    public static final int MAX_FUEL = 1000;
    private static final int MAX_VISITED_ENTRIES = 1024;
    private static final int MAX_ENTRY_ID_LENGTH = 128;
    private static final SavedData.Factory<ShipStateData> FACTORY =
            new SavedData.Factory<>(ShipStateData::new, ShipStateData::load);

    private Planet planet = Planet.LUSH;
    private int fuel = MAX_FUEL;
    private final Set<String> visited = new LinkedHashSet<>();
    private String currentEntryId = null;

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
        data.planet = Planet.fromId(tag.getString("Planet"));
        // Old/partially-written saves may lack the Fuel tag; start them full
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
        if (tag.contains("CurrentEntry", Tag.TAG_STRING))
        {
            String entry = tag.getString("CurrentEntry");
            data.currentEntryId = safeEntryId(entry);
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
        data.shipPosition = loadShipPosition(tag, data.planet);
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
        return data;
    }

    public static ShipStateData load(CompoundTag tag)
    {
        return load(tag, HolderLookup.Provider.create(java.util.stream.Stream.empty()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putString("Planet", planet.getId());
        tag.putInt("Fuel", fuel);
        ListTag visitedList = new ListTag();
        for (String entryId : visited)
        {
            visitedList.add(StringTag.valueOf(entryId));
        }
        tag.put("Visited", visitedList);
        tag.putString("CurrentEntry", currentEntryId == null ? "" : currentEntryId);
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

    public Planet getPlanet()
    {
        return planet;
    }

    public void setPlanet(Planet planet)
    {
        this.planet = planet;
        this.setDirty();
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

    private static UniversePosition loadShipPosition(CompoundTag tag, Planet planet)
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

            UniversePosition dock = defaultDock(planet);
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
            return defaultDock(planet);
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

    private static UniversePosition defaultDock(Planet planet)
    {
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
}
