package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** The Frozen Planet dimension: overworld-like terrain restricted to cold biomes. */
public final class FrozenPlanet
{
    public static final ResourceKey<Level> FROZEN_LEVEL =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "frozen"));

    private FrozenPlanet()
    {
    }
}
