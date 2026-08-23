package com.starboundmc.world.starmap;

import com.starboundmc.world.Planet;

import java.util.Objects;

/**
 * A single celestial body shown on the star map.
 *
 * <p>Entries are deliberately DECOUPLED from the {@link Planet} enum (which is
 * only about dimension destinations): {@code destination == null} marks a body
 * that exists in the star map but is not reachable yet (locked, e.g. the gas
 * giant). Adding a new planet later is just one more {@code PlanetEntry}
 * definition — with a destination if it has a real dimension, without one if
 * it is only a placeholder.</p>
 *
 * <p>Layout: {@code orbitRadius}/{@code orbitAngle} position the body around
 * the system's star. A moon (non-null {@code parentEntryId}) is positioned
 * around its parent body instead, using the same radius/angle relative to the
 * parent.</p>
 */
public class PlanetEntry
{
    private final String entryId;
    private final String nameKey;
    private final String typeKey;
    private final String descriptionKey;
    private final Planet destination;
    private final int threatLevel;
    private final int orbitRadius;
    private final float orbitAngle;
    private final String parentEntryId;
    private final StarmapBodyVisual visual;

    public PlanetEntry(String entryId, String nameKey, String typeKey, String descriptionKey,
                       Planet destination, int threatLevel,
                       int orbitRadius, float orbitAngle, String parentEntryId,
                       StarmapBodyVisual visual)
    {
        this.entryId = entryId;
        this.nameKey = nameKey;
        this.typeKey = typeKey;
        this.descriptionKey = descriptionKey;
        this.destination = destination;
        this.threatLevel = threatLevel;
        this.orbitRadius = orbitRadius;
        this.orbitAngle = orbitAngle;
        this.parentEntryId = parentEntryId;
        this.visual = Objects.requireNonNull(visual, "visual");
    }

    /** Compatibility constructor for colour/size-only definitions. */
    public PlanetEntry(String entryId, String nameKey, String typeKey, String descriptionKey,
                       Planet destination, int threatLevel,
                       int orbitRadius, float orbitAngle, String parentEntryId, int color, int markerSize)
    {
        this(entryId, nameKey, typeKey, descriptionKey, destination, threatLevel,
                orbitRadius, orbitAngle, parentEntryId, StarmapBodyVisual.basic(color, markerSize));
    }

    public String getEntryId()
    {
        return entryId;
    }

    public String getNameKey()
    {
        return nameKey;
    }

    public String getTypeKey()
    {
        return typeKey;
    }

    public String getDescriptionKey()
    {
        return descriptionKey;
    }

    /** The dimension destination; null means the body is not reachable yet. */
    public Planet getDestination()
    {
        return destination;
    }

    public boolean isReachable()
    {
        return destination != null;
    }

    public int getThreatLevel()
    {
        return threatLevel;
    }

    public int getOrbitRadius()
    {
        return orbitRadius;
    }

    public float getOrbitAngle()
    {
        return orbitAngle;
    }

    /** Entry id of the body this one orbits; null = orbits the star. */
    public String getParentEntryId()
    {
        return parentEntryId;
    }

    public boolean isMoon()
    {
        return parentEntryId != null;
    }

    public StarmapBodyVisual getVisual()
    {
        return visual;
    }

    /** Marker color used by the star map UI. */
    public int getColor()
    {
        return visual.getPrimaryColor();
    }

    /** Marker dot diameter in px used by the star map UI. */
    public int getMarkerSize()
    {
        return visual.getMarkerSize();
    }
}
