package com.starboundmc.world.starmap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.space.UniversePosition;

import java.util.Objects;

/**
 * Shared visual definition for one system's star. Both the star map and the
 * in-world sky renderers read this immutable profile, so a system cannot show
 * one stellar identity on the console and a different one outside the ship.
 */
public final class StellarVisualProfile
{
    /** Datapacks use the same sector/local position contract as the rest of the universe. */
    public static final Codec<StellarVisualProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    UniversePosition.CODEC.fieldOf("position").forGetter(StellarVisualProfile::getUniversePosition),
                    Codec.INT.fieldOf("core_color").forGetter(StellarVisualProfile::getCoreColor),
                    Codec.INT.fieldOf("surface_color").forGetter(StellarVisualProfile::getSurfaceColor),
                    Codec.INT.fieldOf("corona_color").forGetter(StellarVisualProfile::getCoronaColor),
                    StellarDistanceResponse.CODEC.fieldOf("distance_response")
                            .forGetter(StellarVisualProfile::getDistanceResponse),
                    Codec.FLOAT.fieldOf("glow_scale").forGetter(StellarVisualProfile::getGlowScale),
                    Codec.FLOAT.fieldOf("flare_strength").forGetter(StellarVisualProfile::getFlareStrength),
                    Codec.FLOAT.fieldOf("radiation_strength")
                            .forGetter(StellarVisualProfile::getRadiationStrength),
                    Codec.FLOAT.fieldOf("pulse_speed").forGetter(StellarVisualProfile::getPulseSpeed),
                    Codec.INT.fieldOf("starmap_glow_size").forGetter(StellarVisualProfile::getStarMapGlowSize),
                    Codec.INT.optionalFieldOf("starmap_radiation_radius", 0)
                            .forGetter(StellarVisualProfile::getStarMapRadiationRadius)
            ).apply(instance, StellarVisualProfile::new));

    private final UniversePosition universePosition;
    private final int coreColor;
    private final int surfaceColor;
    private final int coronaColor;
    private final StellarDistanceResponse distanceResponse;
    private final float glowScale;
    private final float flareStrength;
    private final float radiationStrength;
    private final float pulseSpeed;
    private final int starMapGlowSize;
    private final int starMapRadiationRadius;

    public StellarVisualProfile(UniversePosition universePosition, int coreColor, int surfaceColor, int coronaColor,
                                StellarDistanceResponse distanceResponse,
                                float glowScale, float flareStrength,
                                float radiationStrength, float pulseSpeed,
                                int starMapGlowSize, int starMapRadiationRadius)
    {
        this.universePosition = Objects.requireNonNull(universePosition, "universePosition");
        this.coreColor = coreColor;
        this.surfaceColor = surfaceColor;
        this.coronaColor = coronaColor;
        this.distanceResponse = distanceResponse;
        this.glowScale = glowScale;
        this.flareStrength = flareStrength;
        this.radiationStrength = radiationStrength;
        this.pulseSpeed = pulseSpeed;
        this.starMapGlowSize = starMapGlowSize;
        this.starMapRadiationRadius = starMapRadiationRadius;
    }

    /** Canonical virtual-space position of the star. */
    public UniversePosition getUniversePosition()
    {
        return universePosition;
    }

    public int getCoreColor()
    {
        return coreColor;
    }

    public int getSurfaceColor()
    {
        return surfaceColor;
    }

    public int getCoronaColor()
    {
        return coronaColor;
    }

    /** Reference radius in sky-shell units; ship views scale it by angular size. */
    public float getApparentRadius()
    {
        return distanceResponse.baseSkyRadius();
    }

    public StellarDistanceResponse getDistanceResponse()
    {
        return distanceResponse;
    }

    public float getGlowScale()
    {
        return glowScale;
    }

    public float getFlareStrength()
    {
        return flareStrength;
    }

    public float getRadiationStrength()
    {
        return radiationStrength;
    }

    public float getPulseSpeed()
    {
        return pulseSpeed;
    }

    public int getStarMapGlowSize()
    {
        return starMapGlowSize;
    }

    public int getStarMapRadiationRadius()
    {
        return starMapRadiationRadius;
    }

    /**
     * Value equality. This is an immutable description of a star's appearance,
     * so two instances built from the same values are interchangeable; the
     * definition layer relies on that to compare a coded round trip against the
     * original.
     */
    @Override
    public boolean equals(Object object)
    {
        if (this == object)
            return true;
        if (!(object instanceof StellarVisualProfile other))
            return false;
        return coreColor == other.coreColor
                && surfaceColor == other.surfaceColor
                && coronaColor == other.coronaColor
                && starMapGlowSize == other.starMapGlowSize
                && starMapRadiationRadius == other.starMapRadiationRadius
                && universePosition.equals(other.universePosition)
                && distanceResponse.equals(other.distanceResponse)
                && Float.compare(glowScale, other.glowScale) == 0
                && Float.compare(flareStrength, other.flareStrength) == 0
                && Float.compare(radiationStrength, other.radiationStrength) == 0
                && Float.compare(pulseSpeed, other.pulseSpeed) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(universePosition, coreColor, surfaceColor, coronaColor,
                distanceResponse, glowScale, flareStrength, radiationStrength, pulseSpeed,
                starMapGlowSize, starMapRadiationRadius);
    }

    @Override
    public String toString()
    {
        return "StellarVisualProfile[" + universePosition
                + ", surface=0x" + Integer.toHexString(surfaceColor) + "]";
    }
}
