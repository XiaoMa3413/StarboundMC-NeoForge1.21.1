package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Lightweight surface material for a body's cockpit-window sphere.
 *
 * <p>Deliberately not PBR: one roughness drives a broad Blinn-Phong highlight,
 * one specular strength its intensity, and one fresnel strength the limb
 * sheen. Emissive is additive on top of the day/night lighting so lava keeps
 * glowing on the night side, masked by an optional texture.</p>
 *
 * <p>The ocean pair modulates the highlight for water: the shader derives the
 * water mask from the albedo itself (blue-dominant pixels read as water), so a
 * hand-drawn mask cannot drift out of alignment with the texture. A body that
 * leaves {@code oceanSpecular} at zero keeps one uniform roughness everywhere,
 * which is what every non-ocean world in the catalog does.</p>
 *
 * <p>Every field is optional in data with a default that reproduces the
 * pre-material look (no highlight, no sheen, no emission), so universe data
 * written before this record existed decodes unchanged.</p>
 */
public record BodyMaterialProfile(float roughness,
                                  float specularStrength,
                                  float fresnelStrength,
                                  float oceanRoughness,
                                  float oceanSpecular,
                                  float emissiveStrength,
                                  int emissiveColor,
                                  Optional<String> emissiveMask)
{
    /**
     * The pre-material look: diffuse day/night shading only. Used when a body
     * has no material block, which is also the renderer's fallback visual.
     */
    public static final BodyMaterialProfile DEFAULT =
            new BodyMaterialProfile(0.9F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0, Optional.empty());

    public static final Codec<BodyMaterialProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.optionalFieldOf("roughness", DEFAULT.roughness)
                            .forGetter(BodyMaterialProfile::roughness),
                    Codec.FLOAT.optionalFieldOf("specular_strength", DEFAULT.specularStrength)
                            .forGetter(BodyMaterialProfile::specularStrength),
                    Codec.FLOAT.optionalFieldOf("fresnel_strength", DEFAULT.fresnelStrength)
                            .forGetter(BodyMaterialProfile::fresnelStrength),
                    Codec.FLOAT.optionalFieldOf("ocean_roughness", DEFAULT.oceanRoughness)
                            .forGetter(BodyMaterialProfile::oceanRoughness),
                    Codec.FLOAT.optionalFieldOf("ocean_specular", DEFAULT.oceanSpecular)
                            .forGetter(BodyMaterialProfile::oceanSpecular),
                    Codec.FLOAT.optionalFieldOf("emissive_strength", DEFAULT.emissiveStrength)
                            .forGetter(BodyMaterialProfile::emissiveStrength),
                    Codec.INT.optionalFieldOf("emissive_color", DEFAULT.emissiveColor)
                            .forGetter(BodyMaterialProfile::emissiveColor),
                    Codec.STRING.optionalFieldOf("emissive_mask")
                            .forGetter(BodyMaterialProfile::emissiveMask)
            ).apply(instance, BodyMaterialProfile::new));

    public BodyMaterialProfile {
        requireUnitRange("roughness", roughness);
        requireUnitRange("specularStrength", specularStrength);
        requireUnitRange("fresnelStrength", fresnelStrength);
        requireUnitRange("oceanRoughness", oceanRoughness);
        requireUnitRange("oceanSpecular", oceanSpecular);
        if (!Float.isFinite(emissiveStrength) || emissiveStrength < 0.0F)
            throw new IllegalArgumentException("emissiveStrength must be finite and >= 0");
        emissiveMask = emissiveMask == null ? Optional.empty() : emissiveMask;
    }

    private static void requireUnitRange(String name, float value)
    {
        if (!Float.isFinite(value) || value < 0.0F || value > 1.0F)
            throw new IllegalArgumentException(name + " must be finite and within [0, 1]");
    }
}
