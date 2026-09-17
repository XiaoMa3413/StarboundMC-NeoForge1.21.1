package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.world.starmap.StarmapBodyVisual;

import java.util.List;
import java.util.Optional;

/**
 * One celestial body in the universe: everything the game needs to draw it on
 * the star map, fly to it, look at it from the ship, and land on it.
 *
 * <p>The three capability profiles are independent, which is the whole point of
 * this model. A body may be flyable but not landable (a future station), visible
 * but not flyable (the gas giant), or landable but not rendered (nothing yet).
 * The legacy model expressed all of this with a single nullable
 * {@code PlanetEntry.destination}, which conflated "has a dimension" with
 * "can be reached".</p>
 *
 * <p>{@code entryId} keeps the existing {@code system:name} format because the
 * value is already stored in saves, packets, the star map and tests.</p>
 */
public record CelestialBodyDefinition(String entryId,
                                      String nameKey,
                                      String typeKey,
                                      String descriptionKey,
                                      int threatLevel,
                                      BodyOrbitDefinition orbit,
                                      StarmapBodyVisual starmapVisual,
                                      Optional<BodyNavigationProfile> navigation,
                                      Optional<BodySpaceVisualProfile> spaceVisual,
                                      Optional<BodySurfaceDefinition> surface)
{
    public static final Codec<CelestialBodyDefinition> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("entry_id").forGetter(CelestialBodyDefinition::entryId),
                    Codec.STRING.fieldOf("name_key").forGetter(CelestialBodyDefinition::nameKey),
                    Codec.STRING.fieldOf("type_key").forGetter(CelestialBodyDefinition::typeKey),
                    Codec.STRING.fieldOf("description_key").forGetter(CelestialBodyDefinition::descriptionKey),
                    Codec.intRange(0, 10).optionalFieldOf("threat_level", 0)
                            .forGetter(CelestialBodyDefinition::threatLevel),
                    BodyOrbitDefinition.CODEC.fieldOf("orbit").forGetter(CelestialBodyDefinition::orbit),
                    StarmapBodyVisual.CODEC.fieldOf("starmap_visual")
                            .forGetter(CelestialBodyDefinition::starmapVisual),
                    BodyNavigationProfile.CODEC.optionalFieldOf("navigation")
                            .forGetter(CelestialBodyDefinition::navigation),
                    BodySpaceVisualProfile.CODEC.optionalFieldOf("space_visual")
                            .forGetter(CelestialBodyDefinition::spaceVisual),
                    BodySurfaceDefinition.CODEC.optionalFieldOf("surface")
                            .forGetter(CelestialBodyDefinition::surface)
            ).apply(instance, CelestialBodyDefinition::new));

    public CelestialBodyDefinition
    {
        if (entryId == null || entryId.isBlank() || !entryId.contains(":"))
            throw new IllegalArgumentException("entryId must be a namespaced id, got: " + entryId);
        if (threatLevel < 0 || threatLevel > 10)
            throw new IllegalArgumentException("threatLevel must be within 0..10");
        navigation = navigation == null ? Optional.empty() : navigation;
        spaceVisual = spaceVisual == null ? Optional.empty() : spaceVisual;
        surface = surface == null ? Optional.empty() : surface;
    }

    /**
     * Whether the ship can plot a course here. Unlike the legacy
     * {@code PlanetEntry.isReachable()}, this asks about flight geometry and
     * says nothing about whether the body has a landable surface.
     */
    public boolean isNavigable()
    {
        return navigation.isPresent();
    }

    /** Whether a player can be sent down to a surface world. */
    public boolean isLandable()
    {
        return surface.isPresent();
    }

    /** Whether the body is actually drawn outside the cockpit window. */
    public boolean isSpaceRendered()
    {
        return spaceVisual.isPresent();
    }

    public Optional<String> parentEntryId()
    {
        return orbit.parentEntryId();
    }

    /** Convenience for the star-map UI, which keys off the marker geometry. */
    public int markerSize()
    {
        return starmapVisual.getMarkerSize();
    }

    /** Every body id referenced by this definition, for catalog validation. */
    public List<String> referencedEntryIds()
    {
        return orbit.parentEntryId().map(List::of).orElse(List.of());
    }
}
