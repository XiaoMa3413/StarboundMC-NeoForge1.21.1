package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.world.starmap.StarmapBodyVisual;

import java.util.List;
import java.util.Optional;

/** A body with independent star-map, navigation, space-visual and surface capabilities.
 * Catalogs select the required capabilities for each consumer; a visible orbit-only body need not have a surface. */
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

    /** Whether flight geometry exists, independently of a landable surface. */
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
