package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Environmental profile of a landable body: what the player is exposed to once
 * standing on the surface.
 *
 * <p>This is data only. The hazard service that consumes it belongs to a later
 * batch; the values exist now so a body's environment travels with its
 * definition instead of being implied by which dimension it points at.</p>
 *
 * <p>{@code breathable} is a level, not a boolean hazard: a non-breathable body
 * needs an EPP to survive at all, whereas the tiered values are resistances the
 * player builds up over time. Tiers start at 0, meaning "no exposure".</p>
 */
public record PlanetEnvironmentProfile(boolean breathable,
                                       int coldTier,
                                       int heatTier,
                                       int radiationTier,
                                       float gravityScale)
{
    /** Tiers are 0-3 so an EPP can be authored per tier without a giant scale. */
    public static final int MAX_TIER = 3;

    public static final Codec<PlanetEnvironmentProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("breathable", true)
                            .forGetter(PlanetEnvironmentProfile::breathable),
                    Codec.intRange(0, MAX_TIER).optionalFieldOf("cold_tier", 0)
                            .forGetter(PlanetEnvironmentProfile::coldTier),
                    Codec.intRange(0, MAX_TIER).optionalFieldOf("heat_tier", 0)
                            .forGetter(PlanetEnvironmentProfile::heatTier),
                    Codec.intRange(0, MAX_TIER).optionalFieldOf("radiation_tier", 0)
                            .forGetter(PlanetEnvironmentProfile::radiationTier),
                    Codec.FLOAT.optionalFieldOf("gravity_scale", 1.0F)
                            .forGetter(PlanetEnvironmentProfile::gravityScale)
            ).apply(instance, PlanetEnvironmentProfile::new));

    public PlanetEnvironmentProfile
    {
        requireTier("coldTier", coldTier);
        requireTier("heatTier", heatTier);
        requireTier("radiationTier", radiationTier);
        if (!Float.isFinite(gravityScale) || gravityScale <= 0.0F)
            throw new IllegalArgumentException("gravityScale must be positive and finite");
    }

    /** A temperate, breathable world needing no protection: the Lush baseline. */
    public static final PlanetEnvironmentProfile TEMPERATE =
            new PlanetEnvironmentProfile(true, 0, 0, 0, 1.0F);

    public boolean hasAnyHazard()
    {
        return !breathable || coldTier > 0 || heatTier > 0 || radiationTier > 0;
    }

    private static void requireTier(String name, int tier)
    {
        if (tier < 0 || tier > MAX_TIER)
            throw new IllegalArgumentException(name + " must be within 0.." + MAX_TIER);
    }
}
