package com.starboundmc.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** Harsh, cold sky for the Frozen Planet: no vanilla sun, heavy cold fog. */
public class FrozenDimensionEffects extends DimensionSpecialEffects
{
    public FrozenDimensionEffects()
    {
        super(Float.NaN, false, DimensionSpecialEffects.SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness)
    {
        double t = brightness * 0.55D + 0.45D;
        return new Vec3(
                fogColor.x * t * 0.55D + 0.08D,
                fogColor.y * t * 0.60D + 0.12D,
                fogColor.z * t * 0.72D + 0.22D);
    }

    @Override
    public boolean isFoggyAt(int x, int y)
    {
        return true;
    }
}
