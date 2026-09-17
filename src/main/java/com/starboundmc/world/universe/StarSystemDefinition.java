package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.starmap.GalaxyMapPosition;
import com.starboundmc.world.starmap.StellarVisualProfile;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * One star system: its star, its physical place in the universe, its
 * art-directed node on the galaxy map, and the bodies it owns.
 *
 * <p>The galaxy-map position and the navigation centre are deliberately separate
 * concepts. The map position is UI composition; the navigation centre is where
 * the system actually is in flight space. Conflating them would let a cosmetic
 * map edit move a planet.</p>
 */
public record StarSystemDefinition(String systemId,
                                   String nameKey,
                                   String descriptionKey,
                                   String starTypeKey,
                                   StellarVisualProfile stellarVisual,
                                   GalaxyMapPosition galaxyMapPosition,
                                   UniversePosition navigationCenter,
                                   double influenceRadius,
                                   double planetFieldRadius,
                                   List<CelestialBodyDefinition> bodies)
{
    public static final Codec<StarSystemDefinition> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("system_id").forGetter(StarSystemDefinition::systemId),
                    Codec.STRING.fieldOf("name_key").forGetter(StarSystemDefinition::nameKey),
                    Codec.STRING.fieldOf("description_key").forGetter(StarSystemDefinition::descriptionKey),
                    Codec.STRING.fieldOf("star_type_key").forGetter(StarSystemDefinition::starTypeKey),
                    StellarVisualProfile.CODEC.fieldOf("stellar_visual")
                            .forGetter(StarSystemDefinition::stellarVisual),
                    GalaxyMapPosition.CODEC.fieldOf("galaxy_map_position")
                            .forGetter(StarSystemDefinition::galaxyMapPosition),
                    UniversePosition.CODEC.fieldOf("navigation_center")
                            .forGetter(StarSystemDefinition::navigationCenter),
                    Codec.DOUBLE.fieldOf("influence_radius")
                            .forGetter(StarSystemDefinition::influenceRadius),
                    // Optional so a hand-written system keeps working: leaving it out
                    // means the field matches the influence radius, which is what the
                    // one-argument legacy constructor did.
                    Codec.DOUBLE.optionalFieldOf("planet_field_radius")
                            .forGetter(system -> Optional.of(system.planetFieldRadius())),
                    CelestialBodyDefinition.CODEC.listOf().fieldOf("bodies")
                            .forGetter(StarSystemDefinition::bodies)
            ).apply(instance, (systemId, nameKey, descriptionKey, starTypeKey, stellarVisual,
                               galaxyMapPosition, navigationCenter, influenceRadius,
                               planetFieldRadius, bodies) ->
                    new StarSystemDefinition(systemId, nameKey, descriptionKey, starTypeKey,
                            stellarVisual, galaxyMapPosition, navigationCenter, influenceRadius,
                            planetFieldRadius.orElse(null), bodies)));

    /**
     * A system whose body-rendering field matches its star influence: the shape
     * the legacy one-argument constructor produced.
     */
    public StarSystemDefinition(String systemId, String nameKey, String descriptionKey,
                                String starTypeKey, StellarVisualProfile stellarVisual,
                                GalaxyMapPosition galaxyMapPosition,
                                UniversePosition navigationCenter,
                                double influenceRadius,
                                List<CelestialBodyDefinition> bodies)
    {
        this(systemId, nameKey, descriptionKey, starTypeKey, stellarVisual, galaxyMapPosition,
                navigationCenter, influenceRadius, Double.NaN, bodies);
    }

    public StarSystemDefinition
    {
        if (systemId == null || systemId.isBlank())
            throw new IllegalArgumentException("systemId must not be blank");
        if (systemId.contains(":"))
            throw new IllegalArgumentException("systemId must not be namespaced, got: " + systemId);
        if (!Double.isFinite(influenceRadius) || influenceRadius <= 0.0)
            throw new IllegalArgumentException("influenceRadius must be positive and finite");
        // A system always renders its own bodies at least as far out as the star
        // itself reaches, so an omitted (NaN) or smaller field collapses upward.
        // A star's visual fade is not a reason to stop drawing a berth that is
        // still inside the system: the outer berths live well beyond the fade.
        planetFieldRadius = Double.isFinite(planetFieldRadius)
                ? Math.max(influenceRadius, planetFieldRadius)
                : influenceRadius;
        bodies = List.copyOf(bodies);
    }

    /**
     * The shared direction used for planetary lighting in this system. Every
     * body uses this one vector so a moon cannot acquire a visible light
     * mismatch against its primary.
     */
    public Vec3 lightingDirection()
    {
        Vec3 delta = stellarVisual.getVirtualPosition().subtract(navigationCenter.toLocalVec3());
        return delta.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : delta.normalize();
    }

    public int starColor()
    {
        return stellarVisual.getSurfaceColor();
    }

    /** Number of direct satellites orbiting the supplied body in this system. */
    public int moonCount(String parentEntryId)
    {
        if (parentEntryId == null)
            return 0;
        int count = 0;
        for (CelestialBodyDefinition body : bodies)
        {
            if (parentEntryId.equals(body.parentEntryId().orElse(null)))
                count++;
        }
        return count;
    }
}
