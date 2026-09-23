package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * An independent cloud layer for a body's cockpit-window sphere.
 *
 * <p>The layer is a slightly larger sphere whose cloud density is computed
 * procedurally in the planet surface shader: no texture, no mask derived from
 * the albedo, no seam or polar artefact. The noise field scrolls over time, so
 * the weather evolves rather than merely revolving, and the sphere rotates at
 * its own rate so the clouds drift across the ground.</p>
 *
 * <p>Absent block means no cloud layer, which is every body but the flagship
 * world this was trialled on; the plan explicitly wants the multi-layer
 * architecture proven on one planet before it is copied anywhere else.</p>
 */
public record BodyCloudProfile(float spinRate,
                               float opacity,
                               float coverage)
{
    /**
     * All three fields are required rather than defaulted: the block is new, so
     * there is no legacy data to stay compatible with, and an empty block would
     * otherwise encode as {@code {}} and silently inherit whatever the defaults
     * happen to be at decode time.
     */
    public static final Codec<BodyCloudProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.fieldOf("spin_rate").forGetter(BodyCloudProfile::spinRate),
                    Codec.FLOAT.fieldOf("opacity").forGetter(BodyCloudProfile::opacity),
                    Codec.FLOAT.fieldOf("coverage").forGetter(BodyCloudProfile::coverage)
            ).apply(instance, BodyCloudProfile::new));

    public BodyCloudProfile {
        if (!Float.isFinite(spinRate))
            throw new IllegalArgumentException("spinRate must be finite");
        if (!Float.isFinite(opacity) || opacity < 0.0F || opacity > 1.0F)
            throw new IllegalArgumentException("opacity must be finite and within [0, 1]");
        if (!Float.isFinite(coverage) || coverage < 0.0F || coverage > 1.0F)
            throw new IllegalArgumentException("coverage must be finite and within [0, 1]");
    }
}
