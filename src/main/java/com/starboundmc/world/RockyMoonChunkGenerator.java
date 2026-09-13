package com.starboundmc.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
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
 *
 * <p>The teleporter landing site is levelled into a broad, gently undulating
 * gravel plain: the bench height is the local natural noise average (so the
 * flat never sinks below its surroundings like an excavated crater), the plain
 * radii are wobbled by angular noise so the boundary never draws a perfect
 * circle, and terrain eases to the bench over a wide smoothstep skirt. The
 * descent therefore always ends on open, walkable regolith.</p>
 */
public class RockyMoonChunkGenerator extends NoiseBasedChunkGenerator
{
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();

    /** Lazily sampled natural surface average around the landing point. */
    private int landingBenchY = Integer.MIN_VALUE;

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
        // Benign race: workers may sample the bench twice, always to the same value.
        if (this.landingBenchY == Integer.MIN_VALUE)
        {
            this.landingBenchY = RockyMoonLandingPlain.computeBenchY(
                    (x, z) -> this.getBaseHeight(x, z,
                            Heightmap.Types.WORLD_SURFACE_WG, chunk, randomState));
        }
        return super.fillFromNoise(blender, randomState, structureManager, chunk)
                .thenApply(RockyMoonChunkGenerator::dryOutWater)
                .thenApply(this::flattenLandingPlain);
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
     * Replaces grass/dirt/snow with grey stone and spreads gravel in smooth,
     * connected regolith patches so the surface reads as impact-scoured rock.
     * Columns inside the landing plain stay gravel-dominant with a light stone
     * speckle instead of following the patch noise.
     */
    private static void stripOrganicCrust(ChunkAccess chunk)
    {
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int si = 0; si < sections.length; si++)
        {
            LevelChunkSection section = sections[si];
            if (section.hasOnlyAir())
                continue;
            for (int x = 0; x < 16; x++)
            {
                for (int z = 0; z < 16; z++)
                {
                    int wx = baseX + x;
                    int wz = baseZ + z;
                    boolean gravel = RockyMoonLandingPlain.inPlain(wx, wz)
                            ? RockyMoonLandingPlain.surfaceIsGravel(wx, wz)
                            : isGravelPatch(wx, wz);
                    for (int y = 0; y < 16; y++)
                    {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.isAir() || !isOrganicCrust(state))
                            continue;
                        section.setBlockState(x, y, z, gravel ? GRAVEL : STONE, false);
                    }
                }
            }
        }
    }

    /**
     * Reshapes noise terrain into the landing plain. Runs right after noise
     * (before the surface rule lays its crust, which the strip then converts),
     * and only inside the plain skirt: columns keep their natural surface
     * elsewhere. Filling raises stone to the bench; lowering exposes the
     * crater-like regolith under the old crust. Writes go through
     * {@code chunk.setBlockState} so the {@code WORLD_SURFACE_WG} heightmap
     * the surface rule anchors on follows the reshape.
     */
    private ChunkAccess flattenLandingPlain(ChunkAccess chunk)
    {
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        // Whole-chunk reject: closest point of the chunk box to the landing
        // point decides whether the skirt can reach this chunk at all.
        int closestX = Math.max(baseX, Math.min(
                RockyMoonPlanet.DEFAULT_SPAWN.getX(), baseX + 15));
        int closestZ = Math.max(baseZ, Math.min(
                RockyMoonPlanet.DEFAULT_SPAWN.getZ(), baseZ + 15));
        if (!RockyMoonLandingPlain.inPlain(closestX, closestZ))
            return chunk;

        LevelChunkSection[] sections = chunk.getSections();
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                int wx = baseX + x;
                int wz = baseZ + z;
                if (!RockyMoonLandingPlain.inPlain(wx, wz))
                    continue;
                int naturalY = topSolidY(sections, x, z);
                int targetY = RockyMoonLandingPlain.targetY(wx, wz, naturalY, this.landingBenchY);
                if (targetY != naturalY)
                {
                    int lo = Math.min(naturalY, targetY) + 1;
                    int hi = Math.max(naturalY, targetY);
                    for (int y = lo; y <= hi; y++)
                    {
                        BlockPos pos = BlockPos.containing(wx, y, wz);
                        BlockState current = chunk.getBlockState(pos);
                        if (y <= targetY)
                        {
                            if (current.isAir())
                                chunk.setBlockState(pos, STONE, false);
                        }
                        else if (!current.isAir() && !current.is(Blocks.BEDROCK))
                        {
                            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                        }
                    }
                }
                // Support plug: the overworld noise skeleton carries cheese
                // caves, and one opening right under the bench would punch a
                // hole through the landing surface. Bridge the first eight
                // blocks under the bench so the plain stays walkable; deeper
                // caverns remain untouched natural terrain.
                for (int y = targetY; y >= targetY - 7; y--)
                {
                    BlockPos pos = BlockPos.containing(wx, y, wz);
                    if (chunk.getBlockState(pos).isAir())
                        chunk.setBlockState(pos, STONE, false);
                    else
                        break;
                }
            }
        }
        return chunk;
    }

    /** Highest non-air block of a column, or the world bottom when all air. */
    private static int topSolidY(LevelChunkSection[] sections, int x, int z)
    {
        for (int si = sections.length - 1; si >= 0; si--)
        {
            LevelChunkSection section = sections[si];
            if (section.hasOnlyAir())
                continue;
            for (int y = 15; y >= 0; y--)
            {
                if (!section.getBlockState(x, y, z).isAir())
                    return si * 16 + y;
            }
        }
        return 0;
    }

    /**
     * Low-frequency value noise on a 9-block lattice: gravel gathers into
     * organic, cross-chunk-coherent patches. The previous per-block linear
     * hash aliased into visible diagonal stripes across the surface.
     */
    private static boolean isGravelPatch(int x, int z)
    {
        int x0 = Math.floorDiv(x, 9);
        int z0 = Math.floorDiv(z, 9);
        double tx = RockyMoonLandingPlain.smoothstep((x - x0 * 9) / 9.0);
        double tz = RockyMoonLandingPlain.smoothstep((z - z0 * 9) / 9.0);
        double a = RockyMoonLandingPlain.lattice(x0, z0)
                + (RockyMoonLandingPlain.lattice(x0 + 1, z0) - RockyMoonLandingPlain.lattice(x0, z0)) * tx;
        double b = RockyMoonLandingPlain.lattice(x0, z0 + 1)
                + (RockyMoonLandingPlain.lattice(x0 + 1, z0 + 1) - RockyMoonLandingPlain.lattice(x0, z0 + 1)) * tx;
        return a + (b - a) * tz > 0.68;
    }

    /** The overworld surface family that must not exist on an airless rock. */
    private static boolean isOrganicCrust(BlockState state)
    {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.ICE);
    }
}
