package com.starboundmc.world.starmap;

import com.starboundmc.space.SectorCoordinate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable sector hash for static star-system definitions. */
public final class GalaxySpatialIndex
{
    private final Map<SectorCoordinate, StarSystem[]> sectors;
    private final int systemCount;

    private GalaxySpatialIndex(Map<SectorCoordinate, StarSystem[]> sectors, int systemCount)
    {
        this.sectors = sectors;
        this.systemCount = systemCount;
    }

    public static GalaxySpatialIndex build(List<StarSystem> systems)
    {
        Objects.requireNonNull(systems, "systems");
        Map<SectorCoordinate, List<StarSystem>> mutable = new HashMap<>();
        for (StarSystem system : systems)
        {
            Objects.requireNonNull(system, "system");
            SectorCoordinate sector = system.getUniverseNavigationCenter().sector();
            mutable.computeIfAbsent(sector, ignored -> new ArrayList<>()).add(system);
        }

        Map<SectorCoordinate, StarSystem[]> frozen = new HashMap<>(mutable.size());
        for (Map.Entry<SectorCoordinate, List<StarSystem>> entry : mutable.entrySet())
            frozen.put(entry.getKey(), entry.getValue().toArray(StarSystem[]::new));
        return new GalaxySpatialIndex(Map.copyOf(frozen), systems.size());
    }

    /**
     * Writes systems in the centre sector first, then outward shell by shell.
     * Returns at most {@code output.length}; no per-result objects are created.
     */
    public int queryNearby(SectorCoordinate center, int sectorRadius, StarSystem[] output)
    {
        return queryNearby(center, sectorRadius, output, output.length);
    }

    public int queryNearby(SectorCoordinate center, int sectorRadius, StarSystem[] output, int limit)
    {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(output, "output");
        if (sectorRadius < 0)
            throw new IllegalArgumentException("sectorRadius must be non-negative");
        int capacity = Math.max(0, Math.min(limit, output.length));
        if (capacity == 0)
            return 0;

        int count = copySector(center, output, 0, capacity);
        for (int shell = 1; shell <= sectorRadius && count < capacity; shell++)
        {
            for (int dx = -shell; dx <= shell && count < capacity; dx++)
            {
                for (int dy = -shell; dy <= shell && count < capacity; dy++)
                {
                    for (int dz = -shell; dz <= shell && count < capacity; dz++)
                    {
                        if (Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)) != shell)
                            continue;
                        count = copySector(center.offset(dx, dy, dz), output, count, capacity);
                    }
                }
            }
        }
        return count;
    }

    private int copySector(SectorCoordinate sector, StarSystem[] output, int offset, int capacity)
    {
        StarSystem[] bucket = sectors.get(sector);
        if (bucket == null)
            return offset;
        int amount = Math.min(bucket.length, capacity - offset);
        System.arraycopy(bucket, 0, output, offset, amount);
        return offset + amount;
    }

    public int systemCount()
    {
        return systemCount;
    }

    public int occupiedSectorCount()
    {
        return sectors.size();
    }
}
