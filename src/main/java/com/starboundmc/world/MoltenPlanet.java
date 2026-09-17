package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The Molten Planet dimension: nether-style terrain and biomes with the
 * bedrock roof removed, so the surface is open like a planet instead of
 * being capped like the vanilla Nether.
 */
public final class MoltenPlanet
{
    public static final ResourceKey<Level> MOLTEN_LEVEL =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "molten"));

    private MoltenPlanet()
    {
    }
}
