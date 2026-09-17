package com.starboundmc.world.universe;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable runtime index over the universe registry.
 *
 * <p>Game code should not walk the registry directly: the registry is a load
 * order concern, while every gameplay question is a lookup ("which system owns
 * this body", "what is the dimension of this body", "which bodies can I fly
 * to"). Building those lookups once per catalog keeps that question out of hot
 * paths such as per-frame rendering and per-tick travel checks.</p>
 *
 * <p>A catalog is a snapshot. The server builds one when its registry loads and
 * drops it on stop; the client builds one when it receives synced data and drops
 * it on logout. Nothing here is a global singleton, so a stale universe cannot
 * outlive the connection that produced it.</p>
 */
public final class UniverseCatalog
{
    private final List<StarSystemDefinition> systems;
    private final Map<String, StarSystemDefinition> systemsById;
    private final Map<String, CelestialBodyDefinition> bodiesById;
    private final Map<String, StarSystemDefinition> systemByBodyId;
    private final Map<ResourceLocation, CelestialBodyDefinition> bodyByDimension;
    private final List<CelestialBodyDefinition> navigableBodies;
    private final List<CelestialBodyDefinition> surfaceBodies;
    private final List<CelestialBodyDefinition> spaceRenderedBodies;
    private final GalaxySpatialIndexView spatialIndex;

    private UniverseCatalog(List<StarSystemDefinition> systems,
                            Map<String, StarSystemDefinition> systemsById,
                            Map<String, CelestialBodyDefinition> bodiesById,
                            Map<String, StarSystemDefinition> systemByBodyId,
                            Map<ResourceLocation, CelestialBodyDefinition> bodyByDimension,
                            List<CelestialBodyDefinition> navigableBodies,
                            List<CelestialBodyDefinition> surfaceBodies,
                            List<CelestialBodyDefinition> spaceRenderedBodies,
                            GalaxySpatialIndexView spatialIndex)
    {
        this.systems = systems;
        this.systemsById = systemsById;
        this.bodiesById = bodiesById;
        this.systemByBodyId = systemByBodyId;
        this.bodyByDimension = bodyByDimension;
        this.navigableBodies = navigableBodies;
        this.surfaceBodies = surfaceBodies;
        this.spaceRenderedBodies = spaceRenderedBodies;
        this.spatialIndex = spatialIndex;
    }

    /**
     * Indexes the supplied systems.
     *
     * <p>Duplicate ids are rejected rather than silently overwritten: two
     * datapacks declaring the same body is an authoring error, and the failure
     * should name the id instead of leaving one definition unreachable.</p>
     */
    public static UniverseCatalog of(List<StarSystemDefinition> systems)
    {
        Objects.requireNonNull(systems, "systems");
        List<StarSystemDefinition> frozen = List.copyOf(systems);

        Map<String, StarSystemDefinition> systemsById = new LinkedHashMap<>();
        Map<String, CelestialBodyDefinition> bodiesById = new LinkedHashMap<>();
        Map<String, StarSystemDefinition> systemByBodyId = new HashMap<>();
        Map<ResourceLocation, CelestialBodyDefinition> bodyByDimension = new HashMap<>();
        List<CelestialBodyDefinition> navigable = new ArrayList<>();
        List<CelestialBodyDefinition> surface = new ArrayList<>();
        List<CelestialBodyDefinition> spaceRendered = new ArrayList<>();

        for (StarSystemDefinition system : frozen)
        {
            StarSystemDefinition clash = systemsById.put(system.systemId(), system);
            if (clash != null)
                throw new IllegalArgumentException("Duplicate star system id: " + system.systemId());

            for (CelestialBodyDefinition body : system.bodies())
            {
                CelestialBodyDefinition bodyClash = bodiesById.put(body.entryId(), body);
                if (bodyClash != null)
                    throw new IllegalArgumentException("Duplicate body entry id: " + body.entryId());
                systemByBodyId.put(body.entryId(), system);

                if (body.isNavigable())
                    navigable.add(body);
                // Drawn in space needs BOTH a visual and flight geometry: the
                // renderer asks for the body's position and radius, which come from
                // the navigation profile. A body with a visual but no navigation
                // would have no position, so including it here would crash the
                // renderer with "Unknown navigable body" exactly as an unknown saved
                // id crashed the warp manager.
                if (body.isSpaceRendered() && body.isNavigable())
                    spaceRendered.add(body);
                body.surface().ifPresent(definition -> {
                    surface.add(body);
                    CelestialBodyDefinition dimensionClash =
                            bodyByDimension.put(definition.dimension(), body);
                    if (dimensionClash != null)
                    {
                        throw new IllegalArgumentException("Dimension " + definition.dimension()
                                + " is claimed by both " + dimensionClash.entryId()
                                + " and " + body.entryId());
                    }
                });
            }
        }

        return new UniverseCatalog(frozen,
                Collections.unmodifiableMap(systemsById),
                Collections.unmodifiableMap(bodiesById),
                Collections.unmodifiableMap(systemByBodyId),
                Collections.unmodifiableMap(bodyByDimension),
                List.copyOf(navigable),
                List.copyOf(surface),
                List.copyOf(spaceRendered),
                new GalaxySpatialIndexView(frozen));
    }

    /** An empty catalog, for a client that has not received universe data yet. */
    public static UniverseCatalog empty()
    {
        return of(List.of());
    }

    // ---------------------------------------------------------------- queries

    public Optional<StarSystemDefinition> system(String systemId)
    {
        return Optional.ofNullable(systemId == null ? null : systemsById.get(systemId));
    }

    public Optional<CelestialBodyDefinition> body(String entryId)
    {
        return Optional.ofNullable(entryId == null ? null : bodiesById.get(entryId));
    }

    /**
     * The system that owns the supplied body.
     *
     * <p>This replaces the legacy habit of splitting the entry id on {@code ':'}
     * and comparing the prefix. Ownership is a property of the data, not of the
     * string's shape, so a body whose id does not begin with its system id still
     * resolves correctly.</p>
     */
    public Optional<StarSystemDefinition> systemOfBody(String entryId)
    {
        return Optional.ofNullable(entryId == null ? null : systemByBodyId.get(entryId));
    }

    public Optional<CelestialBodyDefinition> bodyByDimension(ResourceLocation dimension)
    {
        return Optional.ofNullable(dimension == null ? null : bodyByDimension.get(dimension));
    }

    public List<StarSystemDefinition> allSystems()
    {
        return systems;
    }

    public List<CelestialBodyDefinition> allBodies()
    {
        return List.copyOf(bodiesById.values());
    }

    /** Bodies the ship can plot a course to. */
    public List<CelestialBodyDefinition> navigableBodies()
    {
        return navigableBodies;
    }

    /** Bodies a player can be sent down to. */
    public List<CelestialBodyDefinition> surfaceBodies()
    {
        return surfaceBodies;
    }

    /**
     * Bodies drawn outside the cockpit window.
     *
     * <p>Requires a space visual <em>and</em> a navigation profile: the renderer
     * reads the body's position and radius from the navigation profile, so a body
     * with a visual but no geometry could not be placed. Exposed as a list so the
     * renderer iterates a stable collection instead of filtering per frame.</p>
     */
    public List<CelestialBodyDefinition> spaceRenderedBodies()
    {
        return spaceRenderedBodies;
    }

    public GalaxySpatialIndexView spatialIndex()
    {
        return spatialIndex;
    }

    public int systemCount()
    {
        return systems.size();
    }

    public int bodyCount()
    {
        return bodiesById.size();
    }

    public boolean isEmpty()
    {
        return systems.isEmpty();
    }

    /**
     * Sector-hash view over the catalog's systems.
     *
     * <p>Deliberately a plain bucket lookup rather than the legacy
     * {@code GalaxySpatialIndex}, which is typed to the old {@code StarSystem}.
     * Keeping the shape (sector to systems) lets the free-flight resolver swap
     * over without changing its query pattern.</p>
     */
    public static final class GalaxySpatialIndexView
    {
        private final Map<com.starboundmc.space.SectorCoordinate, List<StarSystemDefinition>> sectors;
        private final int systemCount;

        private GalaxySpatialIndexView(List<StarSystemDefinition> systems)
        {
            Map<com.starboundmc.space.SectorCoordinate, List<StarSystemDefinition>> mutable = new HashMap<>();
            for (StarSystemDefinition system : systems)
            {
                mutable.computeIfAbsent(system.navigationCenter().sector(), ignored -> new ArrayList<>())
                        .add(system);
            }
            Map<com.starboundmc.space.SectorCoordinate, List<StarSystemDefinition>> frozen = new HashMap<>();
            for (Map.Entry<com.starboundmc.space.SectorCoordinate, List<StarSystemDefinition>> entry
                    : mutable.entrySet())
            {
                frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            this.sectors = Collections.unmodifiableMap(frozen);
            this.systemCount = systems.size();
        }

        /** Systems in the centre sector first, then outward shell by shell. */
        public int queryNearby(com.starboundmc.space.SectorCoordinate center, int sectorRadius,
                               StarSystemDefinition[] output)
        {
            return queryNearby(center, sectorRadius, output, output.length);
        }

        public int queryNearby(com.starboundmc.space.SectorCoordinate center, int sectorRadius,
                               StarSystemDefinition[] output, int limit)
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

        private int copySector(com.starboundmc.space.SectorCoordinate sector,
                               StarSystemDefinition[] output, int offset, int capacity)
        {
            List<StarSystemDefinition> bucket = sectors.get(sector);
            if (bucket == null)
                return offset;
            int amount = Math.min(bucket.size(), capacity - offset);
            for (int i = 0; i < amount; i++)
                output[offset + i] = bucket.get(i);
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

    /** Convenience for callers that hold a registry key rather than an id. */
    public Optional<StarSystemDefinition> system(ResourceKey<StarSystemDefinition> key)
    {
        return system(key.location().getPath());
    }
}
