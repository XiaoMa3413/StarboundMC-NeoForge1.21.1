package com.starboundmc.world;

import com.mojang.serialization.MapCodec;
import com.starboundmc.StarboundMC;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = StarboundMC.MODID)
public final class ModWorldgen {
    // Stage 1 only reserves the legacy codec IDs. Stage 8 restores the
    // preserved ship, molten, barren and filtered implementations.
    public static final ResourceLocation SHIP_CHUNK_GENERATOR_ID = id("ship");
    public static final ResourceLocation MOLTEN_CHUNK_GENERATOR_ID = id("molten");
    public static final ResourceLocation BARREN_CHUNK_GENERATOR_ID = id("barren");
    public static final ResourceLocation FILTERED_BIOME_SOURCE_ID = id("filtered");

    private static final MapCodec<? extends ChunkGenerator> SHIP_CODEC = passthrough(NoiseBasedChunkGenerator.CODEC);
    private static final MapCodec<? extends ChunkGenerator> MOLTEN_CODEC = passthrough(NoiseBasedChunkGenerator.CODEC);
    private static final MapCodec<? extends ChunkGenerator> BARREN_CODEC = passthrough(NoiseBasedChunkGenerator.CODEC);
    private static final MapCodec<FixedBiomeSource> FILTERED_CODEC = passthrough(FixedBiomeSource.CODEC);

    private ModWorldgen() {
    }

    @SubscribeEvent
    public static void registerWorldgenCodecs(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.CHUNK_GENERATOR)) {
            event.register(Registries.CHUNK_GENERATOR, SHIP_CHUNK_GENERATOR_ID, () -> SHIP_CODEC);
            event.register(Registries.CHUNK_GENERATOR, MOLTEN_CHUNK_GENERATOR_ID, () -> MOLTEN_CODEC);
            event.register(Registries.CHUNK_GENERATOR, BARREN_CHUNK_GENERATOR_ID, () -> BARREN_CODEC);
        } else if (event.getRegistryKey().equals(Registries.BIOME_SOURCE)) {
            event.register(Registries.BIOME_SOURCE, FILTERED_BIOME_SOURCE_ID, () -> FILTERED_CODEC);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, path);
    }

    private static <T> MapCodec<T> passthrough(MapCodec<T> codec) {
        return codec.xmap(Function.identity(), Function.identity());
    }
}
