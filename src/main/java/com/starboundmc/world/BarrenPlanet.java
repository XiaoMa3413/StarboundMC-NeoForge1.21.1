package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** The Barren Planet dimension: overworld-like terrain restricted to dry/barren biomes. */
public final class BarrenPlanet
{
    public static final ResourceKey<Level> BARREN_LEVEL =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "barren"));

    private BarrenPlanet()
    {
    }
}
