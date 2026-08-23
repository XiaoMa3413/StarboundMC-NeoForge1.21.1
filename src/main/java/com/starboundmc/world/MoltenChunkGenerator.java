package com.starboundmc.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Molten Planet terrain generator.
 *
 * <p>It is still a {@link NoiseBasedChunkGenerator} so vanilla's nether
 * MultiNoiseBiomeSource can sample biomes normally, but it replaces the
 * Nether's cave/ceiling shape with a simple rolling planet surface made of
 * nether blocks and lava lakes. The biomes still control the top surface
 * block (netherrack, nylium, soul sand, basalt, ...).
 */
public class MoltenChunkGenerator extends NoiseBasedChunkGenerator
{
    private static final int GROUND_BASE_Y = 48;
    private static final int GROUND_AMPLITUDE = 26;
    private static final int SEA_LEVEL = 32;
    private static final int BEDROCK_DEPTH = 4;

    private static final long TERRAIN_SEED = 0x5DEECE66DL;

    public static final Codec<MoltenChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(MoltenChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(MoltenChunkGenerator::generatorSettings))
                    .apply(instance, MoltenChunkGenerator::new));

    private final ImprovedNoise terrainNoise = new ImprovedNoise(new LegacyRandomSource(TERRAIN_SEED));

    public MoltenChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings)
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
        fillTerrain(chunk, randomState);
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState randomState, ChunkAccess chunk)
    {
        // Surface blocks are already placed by fillTerrain; do not apply the
        // vanilla Nether surface rule (which would rebuild the ceiling).
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving carving)
    {
        // Keep the planet surface simple: no vanilla nether cave carving.
    }

    @Override
    public int getGenDepth()
    {
        return 256;
    }

    @Override
    public int getSeaLevel()
    {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY()
    {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor heightAccessor, RandomState randomState)
    {
        return Math.max(getTerrainHeight(x, z), SEA_LEVEL) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState)
    {
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());

        int height = getTerrainHeight(x, z);
        for (int y = 0; y <= height; y++)
        {
            states[y] = y <= BEDROCK_DEPTH ? Blocks.BEDROCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
        }
        if (height < SEA_LEVEL)
        {
            for (int y = height + 1; y <= SEA_LEVEL; y++)
            {
                states[y] = Blocks.LAVA.defaultBlockState();
            }
        }
        return new NoiseColumn(heightAccessor.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(java.util.List<String> info, RandomState randomState, BlockPos pos)
    {
    }

    private void fillTerrain(ChunkAccess chunk, RandomState randomState)
    {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        for (int localX = 0; localX < 16; localX++)
        {
            int worldX = minX + localX;
            for (int localZ = 0; localZ < 16; localZ++)
            {
                int worldZ = minZ + localZ;
                int height = getTerrainHeight(worldX, worldZ);
                Holder<Biome> biome = getBiomeAt(worldX, height, worldZ, randomState);

                for (int y = 0; y <= height; y++)
                {
                    BlockState state;
                    if (y <= BEDROCK_DEPTH)
                    {
                        state = Blocks.BEDROCK.defaultBlockState();
                    }
                    else if (y == height)
                    {
                        state = getTopBlock(biome);
                    }
                    else
                    {
                        state = getUnderBlock(biome, y, height);
                    }
                    chunk.setBlockState(new BlockPos(worldX, y, worldZ), state, false);
                    oceanFloor.update(localX, y, localZ, state);
                    worldSurface.update(localX, y, localZ, state);
                }

                if (height < SEA_LEVEL)
                {
                    for (int y = height + 1; y <= SEA_LEVEL; y++)
                    {
                        BlockState lava = Blocks.LAVA.defaultBlockState();
                        chunk.setBlockState(new BlockPos(worldX, y, worldZ), lava, false);
                        oceanFloor.update(localX, y, localZ, lava);
                        worldSurface.update(localX, y, localZ, lava);
                    }
                }
            }
        }
    }

    private int getTerrainHeight(int x, int z)
    {
        double large = terrainNoise.noise(x * 0.004, 0.0, z * 0.004) * 0.65;
        double detail = terrainNoise.noise(x * 0.02, 0.0, z * 0.02) * 0.35;
        double n = large + detail;
        return Mth.clamp((int) Math.round(GROUND_BASE_Y + n * GROUND_AMPLITUDE), 12, 96);
    }

    private Holder<Biome> getBiomeAt(int x, int y, int z, RandomState randomState)
    {
        return this.getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), randomState.sampler());
    }

    private BlockState getTopBlock(Holder<Biome> biome)
    {
        if (biome.is(Biomes.SOUL_SAND_VALLEY))
            return Blocks.SOUL_SAND.defaultBlockState();
        if (biome.is(Biomes.CRIMSON_FOREST))
            return Blocks.CRIMSON_NYLIUM.defaultBlockState();
        if (biome.is(Biomes.WARPED_FOREST))
            return Blocks.WARPED_NYLIUM.defaultBlockState();
        if (biome.is(Biomes.BASALT_DELTAS))
            return Blocks.BASALT.defaultBlockState();
        return Blocks.NETHERRACK.defaultBlockState();
    }

    private BlockState getUnderBlock(Holder<Biome> biome, int y, int height)
    {
        if (biome.is(Biomes.BASALT_DELTAS))
            return y >= height - 2 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
        if (biome.is(Biomes.SOUL_SAND_VALLEY))
            return y >= height - 1 ? Blocks.SOUL_SOIL.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
        return Blocks.NETHERRACK.defaultBlockState();
    }
}
