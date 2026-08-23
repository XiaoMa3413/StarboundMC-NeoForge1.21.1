package com.starboundmc.world.starmap;

import com.starboundmc.space.UniversePosition;
import net.minecraft.world.phys.Vec3;

/**
 * Shared visual definition for one system's star. Both the star map and the
 * in-world sky renderers read this immutable profile, so a system cannot show
 * one stellar identity on the console and a different one outside the ship.
 */
public final class StellarVisualProfile
{
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
}
