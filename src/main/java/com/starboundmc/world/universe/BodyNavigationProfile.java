package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.space.UniversePosition;

/**
 * The flight geometry of a navigable body: where the ship parks, where the body
 * itself sits, how big it is, and which way the ship faces when docked.
 *
 * <p>These four values are the per-body entries {@code ShipSpace} used to hold as
 * {@code EnumMap} tables. A body without this profile exists on the star map but
 * cannot be flown to, which is the data form of the legacy
 * {@code PlanetEntry.destination == null} marker.</p>
 *
 * <p>Positions use {@link UniversePosition} rather than a raw vector so a body
 * can eventually sit in a distant sector without the sector being dropped on
 * the way through the definition layer.</p>
 */
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
