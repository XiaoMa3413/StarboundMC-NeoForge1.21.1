package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Where a body sits in its system, matching the legacy {@code PlanetEntry}
 * orbit fields exactly.
 *
 * <p>A body with a {@code parentEntryId} orbits that body instead of the star,
 * and its radius/angle are then relative to the parent. This is how the molten
 * moon and the rocky moon are placed.</p>
 */
public record BodyOrbitDefinition(int orbitRadius, float orbitAngle, Optional<String> parentEntryId)
{
    public static final Codec<BodyOrbitDefinition> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("orbit_radius").forGetter(BodyOrbitDefinition::orbitRadius),
                    Codec.FLOAT.fieldOf("orbit_angle").forGetter(BodyOrbitDefinition::orbitAngle),
                    Codec.STRING.optionalFieldOf("parent").forGetter(BodyOrbitDefinition::parentEntryId)
            ).apply(instance, BodyOrbitDefinition::new));

    public BodyOrbitDefinition
    {
        if (orbitRadius < 0)
            throw new IllegalArgumentException("orbitRadius must be non-negative");
        if (!Float.isFinite(orbitAngle))
            throw new IllegalArgumentException("orbitAngle must be finite");
        parentEntryId = parentEntryId == null ? Optional.empty() : parentEntryId;
    }

    /** A body orbiting its system's star directly. */
    public static BodyOrbitDefinition aroundStar(int orbitRadius, float orbitAngle)
    {
        return new BodyOrbitDefinition(orbitRadius, orbitAngle, Optional.empty());
    }

    /** A moon: radius and angle are measured from the parent body. */
    public static BodyOrbitDefinition aroundBody(String parentEntryId, int orbitRadius, float orbitAngle)
    {
        return new BodyOrbitDefinition(orbitRadius, orbitAngle, Optional.of(parentEntryId));
    }

    public boolean isMoon()
    {
        return parentEntryId.isPresent();
    }
}
