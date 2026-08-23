package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = StarboundMC.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModWorldgen
{
    @SubscribeEvent
    public static void registerChunkGenerator(RegisterEvent event)
    {
        if (event.getRegistryKey().equals(Registries.CHUNK_GENERATOR))
        {
            event.register(Registries.CHUNK_GENERATOR,
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "ship"),
                    () -> ShipChunkGenerator.CODEC);
            event.register(Registries.CHUNK_GENERATOR,
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "molten"),
                    () -> MoltenChunkGenerator.CODEC);
            event.register(Registries.CHUNK_GENERATOR,
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "barren"),
                    () -> BarrenChunkGenerator.CODEC);
        }
        else if (event.getRegistryKey().equals(Registries.BIOME_SOURCE))
        {
            event.register(Registries.BIOME_SOURCE,
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "filtered"),
                    () -> FilteredBiomeSource.CODEC);
        }
    }
}
