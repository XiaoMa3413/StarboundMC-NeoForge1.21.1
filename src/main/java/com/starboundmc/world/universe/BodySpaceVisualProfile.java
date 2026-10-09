package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * How a body is drawn from the cockpit window.
 *
 * <p>The authored profile owns the
 * atmosphere tint and peak alpha, the fixed body orientation, the point colour
 * used when the body is too far to shade, the planet texture, and the basic
 * view-dependent surface response. The surface shader reads the material
 * controls here; renderers do not keep separate per-body tables.</p>
 *
 * <p>An optional {@code materialMask} uses its red channel as the
 * specular/smooth-surface mask (0 is rough, 1 is smooth) and its green channel
 * as the emissive mask (0 is unlit, 1 is emissive). Blue and alpha are reserved.</p>
 *
 * <p>The atmosphere tint is stored as three floats rather than a packed colour
 * to retain the authored tint precision.</p>
 *
 * <p>Orientation is {@code (axis tilt, fixed yaw, fixed roll)} in degrees. The fixed orientation is combined with the authored spin rate by the renderer.</p>
 *
 * <p>A present {@code ringTexture} means the body draws a ring band. The ring's
 * proportions, opacity and tint are deliberately not per-body values: they are
 * the canonical shared constants in {@code GasGiantGeometry}, which the
 * flight-route planner also reads to keep a corridor clear of the ring plane.
 * Keeping them in one place is what stops the drawn ring and the planned
 * clearance from drifting apart, so only the texture varies per body.</p>
 */
public record BodySpaceVisualProfile(Optional<String> texture,
                                     Optional<String> materialMask,
                                     float emissiveStrength,
                                     float atmosphereRed,
                                     float atmosphereGreen,
                                     float atmosphereBlue,
                                     float atmospherePeak,
                                     float orientationTilt,
                                     float orientationYaw,
                                     float orientationRoll,
                                     int pointColor,
                                     float terminatorWidth,
                                     float spinRate,
                                     float nightFloor,
                                     float specularStrength,
                                     float roughness,
                                     float fresnelStrength,
                                     Optional<String> ringTexture,
                                     float atmosphereShellScale,
                                     float atmosphereNightFraction,
                                     float atmosphereTwilightStrength,
                                     Optional<String> cloudTexture,
                                     float cloudShellScale,
                                     float cloudOpacity,
                                     float cloudDriftRate)
{
    public static final float DEFAULT_ATMOSPHERE_SHELL_SCALE = 1.055F;
    public static final float DEFAULT_ATMOSPHERE_NIGHT_FRACTION = 0.08F;
    public static final float DEFAULT_CLOUD_SHELL_SCALE = 1.008F;
    public static final float DEFAULT_CLOUD_OPACITY = 1.0F;
    public static final float DEFAULT_CLOUD_DRIFT_RATE = 0.0F;

    private record TextureFields(Optional<String> texture, Optional<String> materialMask,
                                 float emissiveStrength)
    {
        private static final MapCodec<TextureFields> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("texture")
                                .forGetter(TextureFields::texture),
                        Codec.STRING.optionalFieldOf("material_mask")
                                .forGetter(TextureFields::materialMask),
                        Codec.FLOAT.optionalFieldOf("emissive_strength", 0.0F)
                                .forGetter(TextureFields::emissiveStrength)
                ).apply(instance, TextureFields::new));
    }

    private record AtmosphereFields(float red, float green, float blue, float peak,
                                    float shellScale, float nightFraction, float twilightStrength)
    {
        private static final MapCodec<AtmosphereFields> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.FLOAT.optionalFieldOf("atmosphere_red", 0.0F)
                                .forGetter(AtmosphereFields::red),
                        Codec.FLOAT.optionalFieldOf("atmosphere_green", 0.0F)
                                .forGetter(AtmosphereFields::green),
                        Codec.FLOAT.optionalFieldOf("atmosphere_blue", 0.0F)
                                .forGetter(AtmosphereFields::blue),
                        Codec.FLOAT.optionalFieldOf("atmosphere_peak", 0.0F)
                                .forGetter(AtmosphereFields::peak),
                        Codec.FLOAT.optionalFieldOf("atmosphere_shell_scale", DEFAULT_ATMOSPHERE_SHELL_SCALE)
                                .forGetter(AtmosphereFields::shellScale),
                        Codec.FLOAT.optionalFieldOf("atmosphere_night_fraction", DEFAULT_ATMOSPHERE_NIGHT_FRACTION)
                                .forGetter(AtmosphereFields::nightFraction),
                        Codec.FLOAT.optionalFieldOf("atmosphere_twilight_strength", 0.0F)
                                .forGetter(AtmosphereFields::twilightStrength)
                ).apply(instance, AtmosphereFields::new));
    }

    private record CloudFields(Optional<String> texture, Optional<Float> shellScale,
                               float opacity, float driftRate)
    {
        private static final MapCodec<CloudFields> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("cloud_texture")
                                .forGetter(CloudFields::texture),
                        Codec.FLOAT.optionalFieldOf("cloud_shell_scale")
                                .forGetter(CloudFields::shellScale),
                        Codec.FLOAT.optionalFieldOf("cloud_opacity", DEFAULT_CLOUD_OPACITY)
                                .forGetter(CloudFields::opacity),
                        Codec.FLOAT.optionalFieldOf("cloud_drift_rate", DEFAULT_CLOUD_DRIFT_RATE)
                                .forGetter(CloudFields::driftRate)
                ).apply(instance, CloudFields::new));
    }

    public static final Codec<BodySpaceVisualProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    TextureFields.CODEC.forGetter(profile ->
                            new TextureFields(profile.texture(), profile.materialMask(),
                                    profile.emissiveStrength())),
                    AtmosphereFields.CODEC.forGetter(profile -> new AtmosphereFields(
                            profile.atmosphereRed(), profile.atmosphereGreen(),
                            profile.atmosphereBlue(), profile.atmospherePeak(),
                            profile.atmosphereShellScale(), profile.atmosphereNightFraction(),
                            profile.atmosphereTwilightStrength())),
                    CloudFields.CODEC.forGetter(profile -> new CloudFields(
                            profile.cloudTexture(), profile.cloudTexture().isPresent()
                                    || Float.compare(profile.cloudShellScale(), DEFAULT_CLOUD_SHELL_SCALE) != 0
                                    ? Optional.of(profile.cloudShellScale()) : Optional.empty(),
                            profile.cloudOpacity(), profile.cloudDriftRate())),
                    Codec.FLOAT.optionalFieldOf("orientation_tilt", 0.0F)
                            .forGetter(BodySpaceVisualProfile::orientationTilt),
                    Codec.FLOAT.optionalFieldOf("orientation_yaw", 0.0F)
                            .forGetter(BodySpaceVisualProfile::orientationYaw),
                    Codec.FLOAT.optionalFieldOf("orientation_roll", 0.0F)
                            .forGetter(BodySpaceVisualProfile::orientationRoll),
                    Codec.INT.fieldOf("point_color").forGetter(BodySpaceVisualProfile::pointColor),
                    // Shading and material controls live in data so the renderer
                    // does not branch on body identity. Omitted fields select the
                    // current matte, non-emissive surface defaults.
                    Codec.FLOAT.optionalFieldOf("terminator_width", 0.20F)
                            .forGetter(BodySpaceVisualProfile::terminatorWidth),
                    Codec.FLOAT.optionalFieldOf("spin_rate", 0.00375F)
                            .forGetter(BodySpaceVisualProfile::spinRate),
                    Codec.FLOAT.optionalFieldOf("night_floor", 0.10F)
                            .forGetter(BodySpaceVisualProfile::nightFloor),
                    Codec.FLOAT.optionalFieldOf("specular_strength", 0.0F)
                            .forGetter(BodySpaceVisualProfile::specularStrength),
                    Codec.FLOAT.optionalFieldOf("roughness", 1.0F)
                            .forGetter(BodySpaceVisualProfile::roughness),
                    Codec.FLOAT.optionalFieldOf("fresnel_strength", 0.0F)
                            .forGetter(BodySpaceVisualProfile::fresnelStrength),
                    Codec.STRING.optionalFieldOf("ring_texture")
                            .forGetter(BodySpaceVisualProfile::ringTexture)
            ).apply(instance, BodySpaceVisualProfile::new));

    private BodySpaceVisualProfile(TextureFields textures,
                                   AtmosphereFields atmosphere,
                                   CloudFields clouds,
                                   float orientationTilt,
                                   float orientationYaw,
                                   float orientationRoll,
                                   int pointColor,
                                   float terminatorWidth,
                                   float spinRate,
                                   float nightFloor,
                                   float specularStrength,
                                   float roughness,
                                   float fresnelStrength,
                                   Optional<String> ringTexture)
    {
        this(textures.texture(), textures.materialMask(), textures.emissiveStrength(),
                atmosphere.red(), atmosphere.green(), atmosphere.blue(), atmosphere.peak(),
                orientationTilt, orientationYaw, orientationRoll,
                pointColor, terminatorWidth, spinRate, nightFloor, specularStrength, roughness,
                fresnelStrength, ringTexture, atmosphere.shellScale(), atmosphere.nightFraction(),
                atmosphere.twilightStrength(), clouds.texture(),
                clouds.shellScale().orElse(DEFAULT_CLOUD_SHELL_SCALE),
                clouds.opacity(), clouds.driftRate());
    }

    public BodySpaceVisualProfile
    {
        texture = java.util.Objects.requireNonNull(texture, "texture");
        materialMask = java.util.Objects.requireNonNull(materialMask, "materialMask");
        ringTexture = java.util.Objects.requireNonNull(ringTexture, "ringTexture");
        cloudTexture = java.util.Objects.requireNonNull(cloudTexture, "cloudTexture");
        requireUnitRange("emissiveStrength", emissiveStrength);
        requireUnitRange("atmosphereRed", atmosphereRed);
        requireUnitRange("atmosphereGreen", atmosphereGreen);
        requireUnitRange("atmosphereBlue", atmosphereBlue);
        if (!Float.isFinite(atmospherePeak) || atmospherePeak < 0.0F || atmospherePeak > 1.0F)
            throw new IllegalArgumentException("atmospherePeak must be finite and within [0, 1]");
        requireFinite("orientationTilt", orientationTilt);
        requireFinite("orientationYaw", orientationYaw);
        requireFinite("orientationRoll", orientationRoll);
        if (!Float.isFinite(terminatorWidth) || terminatorWidth < 0.0F || terminatorWidth > 1.0F)
            throw new IllegalArgumentException("terminatorWidth must be finite and within [0, 1]");
        requireFinite("spinRate", spinRate);
        if (!Float.isFinite(nightFloor) || nightFloor < 0.0F || nightFloor > 1.0F)
            throw new IllegalArgumentException("nightFloor must be finite and within [0, 1]");
        requireUnitRange("specularStrength", specularStrength);
        requireUnitRange("roughness", roughness);
        requireUnitRange("fresnelStrength", fresnelStrength);
        if (!Float.isFinite(atmosphereShellScale)
                || atmosphereShellScale < 1.005F || atmosphereShellScale > 1.10F)
            throw new IllegalArgumentException("atmosphereShellScale must be finite and within [1.005, 1.10]");
        if (!Float.isFinite(atmosphereNightFraction)
                || atmosphereNightFraction < 0.0F || atmosphereNightFraction > 0.25F)
            throw new IllegalArgumentException("atmosphereNightFraction must be finite and within [0, 0.25]");
        requireUnitRange("atmosphereTwilightStrength", atmosphereTwilightStrength);
        if (!Float.isFinite(cloudShellScale) || cloudShellScale < 1.001F || cloudShellScale > 1.05F)
            throw new IllegalArgumentException("cloudShellScale must be finite and within [1.001, 1.05]");
        requireUnitRange("cloudOpacity", cloudOpacity);
        requireFinite("cloudDriftRate", cloudDriftRate);
    }

    /** The atmosphere tint as a vector, for renderers that work in float triples. */
    public float atmosphereRed() { return atmosphereRed; }
    public float atmosphereGreen() { return atmosphereGreen; }
    public float atmosphereBlue() { return atmosphereBlue; }

    public boolean hasAtmosphere()
    {
        return atmospherePeak > 0.0F;
    }

    /** Whether this body draws a ring band. */
    public boolean hasRings()
    {
        return ringTexture.isPresent();
    }

    /** Whether this body has an authored cloud shell texture. */
    public boolean hasClouds()
    {
        return cloudTexture.isPresent();
    }

    private static void requireUnitRange(String name, float value)
    {
        if (!Float.isFinite(value) || value < 0.0F || value > 1.0F)
            throw new IllegalArgumentException(name + " must be finite and within [0, 1]");
    }

    private static void requireFinite(String name, float value)
    {
        if (!Float.isFinite(value))
            throw new IllegalArgumentException(name + " must be finite");
    }
}
