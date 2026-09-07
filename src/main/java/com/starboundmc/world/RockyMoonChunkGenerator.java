package com.starboundmc.world;

import com.mojang.serialization.MapCodec;
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

/**
 * Rocky Moon terrain generator: the overworld noise skeleton, then a surface
 * pass that turns the organic crust into dead rock. Grass/dirt become stone or
 * gravel, water is dried exactly like the Barren generator does, and snow has
 * nothing to freeze onto. Biome features (trees, flowers) never run because
 * both moon biomes carry an empty feature list, so the surface stays a grey,
 * impact-scoured regolith.
 */
public class RockyMoonChunkGenerator extends NoiseBasedChunkGenerator
{
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();

    public static final MapCodec<RockyMoonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(RockyMoonChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(RockyMoonChunkGenerator::generatorSettings))
                    .apply(instance, RockyMoonChunkGenerator::new));

    public RockyMoonChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings)
    {
        super(biomeSource, settings);
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk)
    {
        return super.fillFromNoise(blender, randomState, structureManager, chunk)
                .thenApply(RockyMoonChunkGenerator::dryOutWater);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState random, ChunkAccess chunk)
    {
        // The organic crust (dirt/grass/snow) is laid by the overworld surface
        // rule during buildSurface, i.e. after fillFromNoise. Strip it here so
        // the strip runs on the finished surface, then leave bare stone/gravel.
        super.buildSurface(level, structureManager, random, chunk);
        stripOrganicCrust(chunk);
    }

    /**
     * Dries terrain water: supported water becomes gravel (flat dry crater
     * floors), unsupported water becomes air so no gravel ever hangs mid-air
     * and chunk load does not trigger falling-gravel cascades.
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
                        BlockState below = y > 0
                                ? section.getBlockState(x, y - 1, z)
                                : (si > 0 ? sections[si - 1].getBlockState(x, 15, z) : null);
                        section.setBlockState(x, y, z,
                                (below == null || below.isAir()) ? Blocks.AIR.defaultBlockState() : GRAVEL,
                                false);
                    }
                }
            }
        }
        return chunk;
    }

    /**
     * Replaces grass/dirt/snow with grey stone (gravel in a sparse banding) and
     * removes the resulting organic layer so the surface reads as impact
     * regolith. The mix is a cheap hash of the block position.
     */
    private static void stripOrganicCrust(ChunkAccess chunk)
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
                        if (state.isAir() || !isOrganicCrust(state))
                            continue;
                        long hash = (chunk.getPos().x * 1182916L) + ((long) x * 39916801L)
                                + ((long) (si * 16 + y) * 8101L) + ((long) z * 50021L);
                        section.setBlockState(x, y, z, (hash & 3L) == 0L ? GRAVEL : STONE, false);
                    }
                }
            }
        }
    }

    /** The overworld surface family that must not exist on an airless rock. */
    private static boolean isOrganicCrust(BlockState state)
    {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.ICE);
    }
}
