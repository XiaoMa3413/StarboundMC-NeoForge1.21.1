package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * An independent cloud layer for a body's cockpit-window sphere.
 *
 * <p>The layer is a slightly larger sphere with a transparent texture, drawn
 * after the surface and rotated at its own rate, so the clouds drift across
 * the ground instead of being glued to it. Lighting reuses the planet surface
 * shader, which keeps the terminator aligned with the surface's.</p>
 *
 * <p>Absent block means no cloud layer, which is every body but the flagship
 * world this was trialled on; the plan explicitly wants the multi-layer
 * architecture proven on one planet before it is copied anywhere else.</p>
 */
public record BodyCloudProfile(String texture,
                               float spinRate,
                               float opacity)
{
    public static final Codec<BodyCloudProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("texture").forGetter(BodyCloudProfile::texture),
                    Codec.FLOAT.optionalFieldOf("spin_rate", 0.0055F)
                            .forGetter(BodyCloudProfile::spinRate),
                    Codec.FLOAT.optionalFieldOf("opacity", 0.85F)
                            .forGetter(BodyCloudProfile::opacity)
            ).apply(instance, BodyCloudProfile::new));

    public BodyCloudProfile {
        if (texture == null || texture.isEmpty())
            throw new IllegalArgumentException("texture must be present for a cloud layer");
        if (!Float.isFinite(spinRate))
            throw new IllegalArgumentException("spinRate must be finite");
        if (!Float.isFinite(opacity) || opacity < 0.0F || opacity > 1.0F)
            throw new IllegalArgumentException("opacity must be finite and within [0, 1]");
    }
}
