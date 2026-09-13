package com.starboundmc.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Giant ore vein: a branching worm of ore in the spirit of vanilla's exposed
 * copper/iron veins — one vein carries dozens up to a hundred-plus ore blocks,
 * an order of magnitude beyond the vanilla {@code minecraft:ore} blob. Its
 * config mirrors vanilla's ore shape but drops the air-exposure discard and
 * allows budgets beyond vanilla's {@code intRange(0, 64)} size cap: {@code size}
 * is the total ore budget for the whole vein, shared across the worm's
 * branches.
 *
 * <p>Every touched block is bounded by
 * {@code WORM_RANGE + STEP_MAX + BALL_MAX_RADIUS} blocks around the vein
 * origin, which must stay inside the 3×3-chunk feature write region for any
 * origin inside the centre chunk (worst-case margin 16 blocks). Reaching
 * outside would make {@code WorldGenRegion} throw, and a failed feature step
 * silently re-queues the chunk forever — the generating thread spins and the
 * server never becomes interactive. {@link #place} therefore also fails soft:
 * an unexpected worldgen exception logs once and keeps the placed blocks.</p>
 */
public class GiantOreVeinFeature extends Feature<GiantOreVeinFeature.Config>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GiantOreVeinFeature.class);
    /**
     * Vanilla ore shape (targets + size), but with the size cap raised:
     * {@link OreConfiguration} clamps {@code size} to 0–64, which a giant vein
     * blows past by design.
     */
    public record Config(List<OreConfiguration.TargetBlockState> targetStates, int size)
            implements FeatureConfiguration
    {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.list(OreConfiguration.TargetBlockState.CODEC).fieldOf("targets")
                                .forGetter(Config::targetStates),
                        Codec.intRange(0, 512).fieldOf("size").forGetter(Config::size))
                        .apply(instance, Config::new));
    }

    private static final int BRANCH_MIN = 2;
    private static final int BRANCH_MAX = 5;
    private static final float MAX_TURN_DEGREES = 34.0F;
    private static final float STEP_MIN = 1.0F;
    private static final float STEP_RANGE = 0.8F;
    private static final float BALL_MIN_RADIUS = 1.6F;
    private static final float BALL_MAX_RADIUS = 2.8F;
    /** Stop depositing after this many consecutive barren steps (worm left the stone). */
    private static final int IDLE_STEP_LIMIT = 24;
    /**
     * Head clamp around the vein origin. Worst-case reach is this plus one
     * step ({@value #STEP_MIN} + {@value #STEP_RANGE}) plus the ball radius
     * ({@value #BALL_MAX_RADIUS}): 11 + 1.8 + 2.8 = 15.6, inside the 16-block
     * minimum margin to the edge of the feature write region.
     */
    private static final int WORM_RANGE = 11;
    /** Cells may never land further than this from the vein origin. */
    private static final int CELL_RANGE = 15;

    public GiantOreVeinFeature(Codec<Config> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context)
    {
        RandomSource random = context.random();
        Config config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        Set<Long> placed = new HashSet<>();
        try
        {
            int branches = BRANCH_MIN + random.nextInt(BRANCH_MAX - BRANCH_MIN + 1);
            for (int branch = 0; branch < branches; branch++)
            {
                // Branch heads fan out around the origin so the worm reads as
                // one connected deposit instead of disjoint blobs.
                int startX = origin.getX() + random.nextInt(9) - 4;
                int startY = origin.getY() + random.nextInt(7) - 3;
                int startZ = origin.getZ() + random.nextInt(9) - 4;
                carveWorm(level, random, config, startX, startY, startZ,
                        origin.getX(), origin.getZ(),
                        Math.max(8, config.size() / branches), placed);
            }
        }
        catch (RuntimeException exception)
        {
            // Never let a vein abort the chunk: a failed feature step re-queues
            // the chunk silently and the generating thread spins forever.
            LOGGER.error("Giant ore vein placement aborted at {}", origin, exception);
        }
        return !placed.isEmpty();
    }

    /**
     * Random-walk worm that deposits overlapping ore balls until its budget is
     * spent or the vein leaves matching stone. Steering pulls the head back
     * toward the origin when it nears the write-region clamp; the reach math
     * that keeps every ball inside the region is documented on
     * {@link #WORM_RANGE}.
     */
    private static void carveWorm(WorldGenLevel level, RandomSource random,
                                  Config config,
                                  double startX, double startY, double startZ,
                                  int clampX, int clampZ,
                                  int budget, Set<Long> placed)
    {
        float yaw = random.nextFloat() * (float) (Math.PI * 2.0);
        float pitch = (random.nextFloat() - 0.5F) * 0.9F;
        double x = startX;
        double y = Mth.clamp(startY, level.getMinBuildHeight() + 2, level.getMaxBuildHeight() - 3);
        double z = startZ;

        int placedCount = 0;
        int idleSteps = 0;
        int stepLimit = budget * 3;
        while (placedCount < budget && idleSteps < IDLE_STEP_LIMIT && stepLimit-- > 0)
        {
            yaw += (random.nextFloat() - 0.5F) * (float) Math.toRadians(MAX_TURN_DEGREES) * 2.0F;
            pitch = Mth.clamp(pitch
                    + (random.nextFloat() - 0.5F) * (float) Math.toRadians(MAX_TURN_DEGREES),
                    -1.1F, 1.1F);
            // Near the clamp the head is steered back toward the vein origin,
            // folding the worm into the region instead of dropping blocks.
            if (Math.abs(x - clampX) > WORM_RANGE || Math.abs(z - clampZ) > WORM_RANGE)
            {
                yaw = (float) Math.atan2(clampZ - z, clampX - x)
                        + (random.nextFloat() - 0.5F) * 0.8F;
                pitch *= 0.3F;
            }
            float step = STEP_MIN + random.nextFloat() * STEP_RANGE;
            x += Mth.cos(yaw) * Mth.cos(pitch) * step;
            y += Mth.sin(pitch) * step;
            z += Mth.sin(yaw) * Mth.cos(pitch) * step;
            y = Mth.clamp(y, level.getMinBuildHeight() + 2, level.getMaxBuildHeight() - 3);

            float radius = BALL_MIN_RADIUS
                    + random.nextFloat() * (BALL_MAX_RADIUS - BALL_MIN_RADIUS);
            int before = placed.size();
            depositBall(level, random, config, x, y, z, clampX, clampZ, radius, placed);
            if (placed.size() == before)
                idleSteps++;
            else
                idleSteps = 0;
            placedCount = placed.size();
        }
    }

    private static void depositBall(WorldGenLevel level, RandomSource random,
                                    Config config,
                                    double cx, double cy, double cz,
                                    int clampX, int clampZ,
                                    float radius, Set<Long> placed)
    {
        int r = Mth.ceil(radius);
        try (BulkSectionAccess access = new BulkSectionAccess(level))
        {
            for (int dx = -r; dx <= r; dx++)
            {
                for (int dy = -r; dy <= r; dy++)
                {
                    for (int dz = -r; dz <= r; dz++)
                    {
                        float distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq > radius * radius + 0.4F)
                            continue;
                        BlockPos pos = BlockPos.containing(cx + dx, cy + dy, cz + dz);
                        // Hard guard: reaching outside the 3×3-chunk write
                        // region must be impossible, not merely unlikely.
                        if (Math.abs(pos.getX() - clampX) > CELL_RANGE
                                || Math.abs(pos.getZ() - clampZ) > CELL_RANGE)
                            continue;
                        if (!placed.add(pos.asLong()))
                            continue;
                        LevelChunkSection section = access.getSection(pos);
                        if (section == null)
                            continue;
                        int relX = SectionPos.sectionRelative(pos.getX());
                        int relY = SectionPos.sectionRelative(pos.getY());
                        int relZ = SectionPos.sectionRelative(pos.getZ());
                        BlockState current = section.getBlockState(relX, relY, relZ);
                        if (current.isAir())
                            continue;
                        BlockState ore = pickOreState(config, current, random);
                        if (ore == null)
                            continue;
                        // Write straight through the section the access has
                        // acquired, exactly like vanilla OreFeature. Going
                        // through WorldGenRegion.setBlock would re-enter the
                        // PalettedContainer thread lock the access already
                        // holds and deadlock the generation worker.
                        section.setBlockState(relX, relY, relZ, ore, false);
                    }
                }
            }
        }
    }

    private static BlockState pickOreState(Config config, BlockState current,
                                           RandomSource random)
    {
        for (OreConfiguration.TargetBlockState target : config.targetStates())
        {
            if (target.target.test(current, random))
                return target.state;
        }
        return null;
    }
}
