package com.starboundmc.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Airless rock: no vanilla sky, and the fog is a thin neutral grey rather than
 * an atmospheric scattering wash, so the black dome and the parent giant stay
 * readable straight after the horizon.
 */
public class RockyMoonDimensionEffects extends DimensionSpecialEffects
{
    public RockyMoonDimensionEffects()
    {
        super(Float.NaN, false, DimensionSpecialEffects.SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness)
    {
        double t = brightness * 0.55D + 0.45D;
        return new Vec3(
                fogColor.x * t * 0.72D + 0.05D,
                fogColor.y * t * 0.72D + 0.05D,
                fogColor.z * t * 0.78D + 0.06D);
    }

    @Override
    public boolean isFoggyAt(int x, int y)
    {
        return true;
    }
}
