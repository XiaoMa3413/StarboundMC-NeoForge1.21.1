package com.starboundmc.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Barren Planet terrain generator: the vanilla overworld terrain with its
 * filtered dry biomes, but every water block placed by terrain generation is
 * dried out (replaced with sand), so there are no lakes, rivers or aquifer
 * water. Structures are placed AFTER {@code fillFromNoise}, so building water
 * (e.g. desert wells) still has its water.
 */
public class BarrenChunkGenerator extends NoiseBasedChunkGenerator
{
    private static final BlockState DRY_BLOCK = Blocks.SAND.defaultBlockState();

    public static final Codec<BarrenChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BarrenChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(BarrenChunkGenerator::generatorSettings))
                    .apply(instance, BarrenChunkGenerator::new));

    public BarrenChunkGenerator(BiomeSource biomeSource,
                                Holder<NoiseGeneratorSettings> settings)
    {
        super(biomeSource, settings);
    }

    @Override
    protected Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk)
    {
        return super.fillFromNoise(executor, blender, randomState, structureManager, chunk)
                .thenApply(BarrenChunkGenerator::dryOutWater);
    }

    /**
     * Replaces terrain-generated water with sand where it is supported (dry
     * lakebeds, cave floors), and removes it entirely where it would float
     * (mid-air aquifer pockets) — so no gravity block ever ends up hanging
     * and chunk loading does not trigger falling-sand cascades.
     */
    private static ChunkAccess dryOutWater(ChunkAccess chunk)
    {
        LevelChunkSection[] sections = chunk.getSections();
        for (int si = 0; si < sections.length; si++)
        {
            LevelChunkSection section = sections[si];
            if (section.hasOnlyAir())
                continue;
            for (int x = 0; x < 16; x++)
            {
                for (int z = 0; z < 16; z++)
                {
                    for (int y = 0; y < 16; y++)
                    {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!state.is(Blocks.WATER))
                            continue;

                        // Sections are processed bottom-up, so the block below
                        // has already been dried out and reflects the final state.
                        BlockState below = y > 0
                                ? section.getBlockState(x, y - 1, z)
                                : (si > 0 ? sections[si - 1].getBlockState(x, 15, z) : null);

                        section.setBlockState(x, y, z,
                                (below == null || below.isAir()) ? Blocks.AIR.defaultBlockState() : DRY_BLOCK,
                                false);
                    }
                }
            }
        }
        return chunk;
    }
}
