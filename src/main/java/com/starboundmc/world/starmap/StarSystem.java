package com.starboundmc.world.starmap;

import com.starboundmc.space.UniversePosition;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/**
 * A star system on the star map: a star (color/size for the UI, optionally a
 * radiation ring) plus a list of celestial bodies. Systems are static
 * definitions; adding one is just another entry in {@link StarSystems}.
 */
public class StarSystem
{
    private final String systemId;
    private final String nameKey;
    private final String descriptionKey;
    private final String starTypeKey;
    private final StellarVisualProfile stellarVisual;
    /** Art-directed normalized position on the ship console's galaxy map. */
    private final GalaxyMapPosition galaxyMapPosition;
    /** Navigation-space centre and soft influence radius used during free flight. */
    private final Vec3 navigationCenter;
    private final UniversePosition universeNavigationCenter;
    private final double influenceRadius;
    private final List<PlanetEntry> entries;

    public StarSystem(String systemId, String nameKey, String descriptionKey, String starTypeKey,
                      StellarVisualProfile stellarVisual, GalaxyMapPosition galaxyMapPosition,
                      Vec3 navigationCenter,
                      double influenceRadius, List<PlanetEntry> entries)
    {
        this.systemId = systemId;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.starTypeKey = starTypeKey;
        this.stellarVisual = stellarVisual;
        this.galaxyMapPosition = Objects.requireNonNull(galaxyMapPosition, "galaxyMapPosition");
        this.navigationCenter = navigationCenter;
        this.universeNavigationCenter = UniversePosition.fromLegacy(navigationCenter);
        this.influenceRadius = influenceRadius;
        this.entries = entries;
    }

    /**
     * Compatibility constructor for temporary and test systems. Production
     * systems should provide an authored galaxy-map position explicitly.
     */
    public StarSystem(String systemId, String nameKey, String descriptionKey, String starTypeKey,
                      StellarVisualProfile stellarVisual, Vec3 navigationCenter,
                      double influenceRadius, List<PlanetEntry> entries)
    {
        this(systemId, nameKey, descriptionKey, starTypeKey, stellarVisual,
                GalaxyMapPosition.fallback(systemId), navigationCenter, influenceRadius, entries);
    }

    public String getSystemId()
    {
        return systemId;
    }

    public String getNameKey()
    {
        return nameKey;
    }

    public String getDescriptionKey()
    {
        return descriptionKey;
    }

    public String getStarTypeKey()
    {
        return starTypeKey;
    }

    public int getStarColor()
    {
        return stellarVisual.getSurfaceColor();
    }

    public int getStarGlowSize()
    {
        return stellarVisual.getStarMapGlowSize();
    }

    public int getRadiationRadius()
    {
        return stellarVisual.getStarMapRadiationRadius();
    }

    public StellarVisualProfile getStellarVisual()
    {
        return stellarVisual;
    }

    public GalaxyMapPosition getGalaxyMapPosition()
    {
        return galaxyMapPosition;
    }

    public Vec3 getNavigationCenter()
    {
        return navigationCenter;
    }

    /** Continuous-universe adapter; current systems remain in sector zero. */
    public UniversePosition getUniverseNavigationCenter()
    {
        return universeNavigationCenter;
    }

    public double getInfluenceRadius()
    {
        return influenceRadius;
    }

    public List<PlanetEntry> getEntries()
    {
        return entries;
    }

    /** Number of direct satellites orbiting the supplied planet in this system. */
    public int getMoonCount(PlanetEntry parent)
    {
        if (parent == null || parent.isMoon())
            return 0;
        String parentId = parent.getEntryId();
        return (int) entries.stream()
                .filter(PlanetEntry::isMoon)
                .filter(entry -> Objects.equals(parentId, entry.getParentEntryId()))
                .count();
    }
}
