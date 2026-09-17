package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.starmap.GalaxyMapPosition;
import com.starboundmc.world.starmap.StellarVisualProfile;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
                    CelestialBodyDefinition.CODEC.listOf().fieldOf("bodies")
                            .forGetter(StarSystemDefinition::bodies)
            ).apply(instance, StarSystemDefinition::new));

    public StarSystemDefinition
    {
        if (systemId == null || systemId.isBlank())
            throw new IllegalArgumentException("systemId must not be blank");
        if (systemId.contains(":"))
            throw new IllegalArgumentException("systemId must not be namespaced, got: " + systemId);
        if (!Double.isFinite(influenceRadius) || influenceRadius <= 0.0)
            throw new IllegalArgumentException("influenceRadius must be positive and finite");
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
