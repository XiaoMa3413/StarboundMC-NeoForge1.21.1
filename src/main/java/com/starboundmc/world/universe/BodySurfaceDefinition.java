package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** Surface destination, landing policy and environment.
 * Absence means orbit-only, as for the gas giant. Rocky Moon has its own landable surface. */
public record BodySurfaceDefinition(ResourceLocation dimension,
                                    LandingPolicy landingPolicy,
                                    PlanetEnvironmentProfile environment)
{
    public static final Codec<BodySurfaceDefinition> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("dimension")
                            .forGetter(BodySurfaceDefinition::dimension),
                    LandingPolicy.CODEC.optionalFieldOf("landing_policy", LandingPolicy.SURFACE_SCAN)
                            .forGetter(BodySurfaceDefinition::landingPolicy),
                    PlanetEnvironmentProfile.CODEC.optionalFieldOf("environment",
                                    PlanetEnvironmentProfile.TEMPERATE)
                            .forGetter(BodySurfaceDefinition::environment)
            ).apply(instance, BodySurfaceDefinition::new));

    /**
     * How the arrival position is chosen. The first version deliberately has
     * only these two: {@code OVERWORLD_RESPAWN} is what the lush overworld does
     * (returning the player to their bed), and {@code SURFACE_SCAN} covers the
     * authored barren/frozen/molten worlds that scan downward for safe footing.
     */
    public enum LandingPolicy
    {
        /** Use the dimension's respawn anchor, as the lush overworld does. */
        OVERWORLD_RESPAWN,
        /** Scan down from the build limit for the first safe standing position. */
        SURFACE_SCAN;

        /**
         * Unknown names are reported as a parse error rather than an exception,
         * so a hand-written datapack with a typo names the bad field instead of
         * failing the whole registry load with a stack trace.
         */
        public static final Codec<LandingPolicy> CODEC = Codec.STRING.comapFlatMap(
                LandingPolicy::parse, LandingPolicy::name);

        static com.mojang.serialization.DataResult<LandingPolicy> parse(String name)
        {
            for (LandingPolicy policy : values())
            {
                if (policy.name().equals(name))
                    return com.mojang.serialization.DataResult.success(policy);
            }
            return com.mojang.serialization.DataResult.error(
                    () -> "Unknown landing policy: " + name);
        }
    }

    public BodySurfaceDefinition
    {
        // ResourceLocation already guarantees a namespace: a bare name such as
        // "overworld" is normalized to "minecraft:overworld" at parse time, which
        // matches vanilla datapack convention. There is nothing left to validate.
    }
}
