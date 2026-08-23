package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = StarboundMC.MODID)
public final class ModWorldgen {
    public static final ResourceLocation SHIP_CHUNK_GENERATOR_ID = id("ship");
    public static final ResourceLocation MOLTEN_CHUNK_GENERATOR_ID = id("molten");
    public static final ResourceLocation BARREN_CHUNK_GENERATOR_ID = id("barren");
    public static final ResourceLocation FILTERED_BIOME_SOURCE_ID = id("filtered");

    private ModWorldgen() {
    }

    @SubscribeEvent
    public static void registerWorldgenCodecs(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.CHUNK_GENERATOR)) {
            event.register(Registries.CHUNK_GENERATOR, SHIP_CHUNK_GENERATOR_ID, () -> ShipChunkGenerator.CODEC);
            event.register(Registries.CHUNK_GENERATOR, MOLTEN_CHUNK_GENERATOR_ID, () -> MoltenChunkGenerator.CODEC);
            event.register(Registries.CHUNK_GENERATOR, BARREN_CHUNK_GENERATOR_ID, () -> BarrenChunkGenerator.CODEC);
        } else if (event.getRegistryKey().equals(Registries.BIOME_SOURCE)) {
            event.register(Registries.BIOME_SOURCE, FILTERED_BIOME_SOURCE_ID, () -> FilteredBiomeSource.CODEC);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, path);
    }
}
