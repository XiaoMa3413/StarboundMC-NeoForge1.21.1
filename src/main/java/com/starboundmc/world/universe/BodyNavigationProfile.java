package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.space.UniversePosition;

/** Flight geometry: dock and body positions, radius and dock heading.
 * Positions retain sector and local coordinates. A body without navigation cannot be flown to. */
public record BodyNavigationProfile(UniversePosition dockPosition,
                                    UniversePosition bodyPosition,
                                    double bodyRadius,
                                    double dockYaw)
{
    public static final Codec<BodyNavigationProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    UniversePosition.CODEC.fieldOf("dock_position")
                            .forGetter(BodyNavigationProfile::dockPosition),
                    UniversePosition.CODEC.fieldOf("body_position")
                            .forGetter(BodyNavigationProfile::bodyPosition),
                    Codec.DOUBLE.fieldOf("body_radius").forGetter(BodyNavigationProfile::bodyRadius),
                    Codec.DOUBLE.fieldOf("dock_yaw").forGetter(BodyNavigationProfile::dockYaw)
            ).apply(instance, BodyNavigationProfile::new));

    public BodyNavigationProfile
    {
        if (!Double.isFinite(bodyRadius) || bodyRadius <= 0.0)
            throw new IllegalArgumentException("bodyRadius must be positive and finite");
        if (!Double.isFinite(dockYaw))
            throw new IllegalArgumentException("dockYaw must be finite");
    }
}
