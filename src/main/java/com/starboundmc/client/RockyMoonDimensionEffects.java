package com.starboundmc.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Airless rock: no vanilla sky and no fog at all — the black dome and the
 * parent giant stay readable straight to the render-distance edge. Terrain
 * fog is zeroed per frame by {@link RockyMoonSkyRenderer#onRenderFog}; here we
 * only refuse the nether-style thick fog and tint any residual fog toward the
 * black of space.
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
        return Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int x, int y)
    {
        return false;
    }
}
