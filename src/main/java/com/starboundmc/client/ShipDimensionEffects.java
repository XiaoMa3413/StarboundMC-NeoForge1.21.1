package com.starboundmc.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

public class ShipDimensionEffects extends DimensionSpecialEffects
{
    public ShipDimensionEffects()
    {
        // SkyType.NONE: no vanilla sun/moon/stars. PlanetRenderer owns the space
        // dome + dense starfield + planet instead, so the whole sky can rotate
        // with the ship's turn heading during warp.
        super(Float.NaN, false, DimensionSpecialEffects.SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness)
    {
        return fogColor.multiply(brightness * 0.94f + 0.06f, brightness * 0.94f + 0.06f, brightness * 0.91f + 0.09f);
    }

    @Override
    public boolean isFoggyAt(int x, int y)
    {
        return false;
    }
}
