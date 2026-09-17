package com.starboundmc.world.starmap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.space.UniversePosition;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Shared visual definition for one system's star. Both the star map and the
 * in-world sky renderers read this immutable profile, so a system cannot show
 * one stellar identity on the console and a different one outside the ship.
 */
public final class StellarVisualProfile
{
    /**
     * Datapack form. The star position is authored as a plain {@code x/y/z}
     * vector because it is virtual-space lighting geometry, not a navigable
     * location; it is converted to a {@link UniversePosition} on construction,
     * exactly as the legacy static definition did.
     */
    public static final Codec<StellarVisualProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("position_x")
                            .forGetter(p -> p.getVirtualPosition().x),
                    Codec.DOUBLE.fieldOf("position_y")
                            .forGetter(p -> p.getVirtualPosition().y),
                    Codec.DOUBLE.fieldOf("position_z")
                            .forGetter(p -> p.getVirtualPosition().z),
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
            ).apply(instance, StellarVisualProfile::fromCodec));

    private static StellarVisualProfile fromCodec(double x, double y, double z,
                                                  int coreColor, int surfaceColor, int coronaColor,
                                                  StellarDistanceResponse distanceResponse,
                                                  float glowScale, float flareStrength,
                                                  float radiationStrength, float pulseSpeed,
                                                  int starMapGlowSize, int starMapRadiationRadius)
    {
        return new StellarVisualProfile(new Vec3(x, y, z), coreColor, surfaceColor, coronaColor,
                distanceResponse, glowScale, flareStrength, radiationStrength, pulseSpeed,
                starMapGlowSize, starMapRadiationRadius);
    }

    private final Vec3 virtualPosition;
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

    public StellarVisualProfile(Vec3 virtualPosition, int coreColor, int surfaceColor, int coronaColor,
                                StellarDistanceResponse distanceResponse,
                                float glowScale, float flareStrength,
                                float radiationStrength, float pulseSpeed,
                                int starMapGlowSize, int starMapRadiationRadius)
    {
        this.virtualPosition = virtualPosition;
        this.universePosition = UniversePosition.fromLegacy(virtualPosition);
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

    public Vec3 getVirtualPosition()
    {
        return virtualPosition;
    }

    /** Continuous-universe adapter; current stellar coordinates remain unchanged. */
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
                && virtualPosition.equals(other.virtualPosition)
                && distanceResponse.equals(other.distanceResponse)
                && Float.compare(glowScale, other.glowScale) == 0
                && Float.compare(flareStrength, other.flareStrength) == 0
                && Float.compare(radiationStrength, other.radiationStrength) == 0
                && Float.compare(pulseSpeed, other.pulseSpeed) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(virtualPosition, coreColor, surfaceColor, coronaColor,
                distanceResponse, glowScale, flareStrength, radiationStrength, pulseSpeed,
                starMapGlowSize, starMapRadiationRadius);
    }

    @Override
    public String toString()
    {
        return "StellarVisualProfile[" + virtualPosition
                + ", surface=0x" + Integer.toHexString(surfaceColor) + "]";
    }
}
