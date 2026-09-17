package com.starboundmc.world.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * How a body is drawn from the cockpit window.
 *
 * <p>This mirrors the per-planet tables inside {@code PlanetRenderer}: the
 * atmosphere tint and peak alpha, the fixed body orientation, the point colour
 * used when the body is too far to shade, and the planet texture. The renderer
 * keeps its own tables for now; a later batch moves it onto this profile, at
 * which point the two must agree exactly.</p>
 *
 * <p>The atmosphere tint is stored as three floats rather than a packed colour
 * because that is how the renderer holds it. Rounding it into an int here would
 * silently change the glow the moment the renderer starts reading this value.</p>
 *
 * <p>Orientation is {@code (axis tilt, fixed yaw, fixed roll)} in degrees. There
 * is no orbital or axial animation in the current model, so these are static.</p>
 *
 * <p>A present {@code ringTexture} means the body draws a ring band. The ring's
 * proportions, opacity and tint are deliberately not per-body values: they are
 * the canonical shared constants in {@code GasGiantGeometry}, which the
 * flight-route planner also reads to keep a corridor clear of the ring plane.
 * Keeping them in one place is what stops the drawn ring and the planned
 * clearance from drifting apart, so only the texture varies per body.</p>
 */
public record BodySpaceVisualProfile(Optional<String> texture,
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
                                     Optional<String> ringTexture)
{
    public static final Codec<BodySpaceVisualProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.optionalFieldOf("texture")
                            .forGetter(BodySpaceVisualProfile::texture),
                    Codec.FLOAT.optionalFieldOf("atmosphere_red", 0.0F)
                            .forGetter(BodySpaceVisualProfile::atmosphereRed),
                    Codec.FLOAT.optionalFieldOf("atmosphere_green", 0.0F)
                            .forGetter(BodySpaceVisualProfile::atmosphereGreen),
                    Codec.FLOAT.optionalFieldOf("atmosphere_blue", 0.0F)
                            .forGetter(BodySpaceVisualProfile::atmosphereBlue),
                    Codec.FLOAT.optionalFieldOf("atmosphere_peak", 0.0F)
                            .forGetter(BodySpaceVisualProfile::atmospherePeak),
                    Codec.FLOAT.optionalFieldOf("orientation_tilt", 0.0F)
                            .forGetter(BodySpaceVisualProfile::orientationTilt),
                    Codec.FLOAT.optionalFieldOf("orientation_yaw", 0.0F)
                            .forGetter(BodySpaceVisualProfile::orientationYaw),
                    Codec.FLOAT.optionalFieldOf("orientation_roll", 0.0F)
                            .forGetter(BodySpaceVisualProfile::orientationRoll),
                    Codec.INT.fieldOf("point_color").forGetter(BodySpaceVisualProfile::pointColor),
                    // Shading controls. These were per-planet switches in the
                    // renderer; moving them here is what lets the renderer stop
                    // branching on the body's identity.
                    Codec.FLOAT.optionalFieldOf("terminator_width", 0.20F)
                            .forGetter(BodySpaceVisualProfile::terminatorWidth),
                    Codec.FLOAT.optionalFieldOf("spin_rate", 0.00375F)
                            .forGetter(BodySpaceVisualProfile::spinRate),
                    Codec.FLOAT.optionalFieldOf("night_floor", 0.10F)
                            .forGetter(BodySpaceVisualProfile::nightFloor),
                    Codec.STRING.optionalFieldOf("ring_texture")
                            .forGetter(BodySpaceVisualProfile::ringTexture)
            ).apply(instance, BodySpaceVisualProfile::new));

    public BodySpaceVisualProfile
    {
        texture = texture == null ? Optional.empty() : texture;
        ringTexture = ringTexture == null ? Optional.empty() : ringTexture;
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
