package com.starboundmc.world;

import com.starboundmc.world.universe.BodySurfaceDefinition;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a player on a body's surface (migration step A8).
 *
 * <p>Previously each planet dimension owned a near-identical copy of this scan,
 * and the caller chose between them with a {@code switch} over the legacy planet
 * enum. The scan is the same algorithm in all cases, so it lives here once and
 * the destination comes from the body's {@code surface} definition instead of
 * from a branch.</p>
 *
 * <p>The scan itself is unchanged: the plan explicitly forbids altering the
 * current safe-landing behaviour, so only the dispatch around it moved.</p>
 */
public final class SurfaceLandingService
{
    /** Search centre, and the fallback when no suitable column is found. */
    private static final BlockPos DEFAULT_SPAWN = new BlockPos(8, 80, 8);
    private static final int MAX_SEARCH_RADIUS = 64;
    private static final int SEARCH_STEP = 8;

    private SurfaceLandingService()
    {
    }

    /**
     * Sends a player to the supplied body's surface.
     *
     * <p>Returns false when the body has no surface (a gas giant, or a body
     * whose datapack is gone), so the caller can report the failure rather than
     * dropping the player somewhere unintended.</p>
     */
    public static boolean teleportToSurface(ServerPlayer player, CelestialBodyDefinition body)
    {
        if (player == null || body == null)
            return false;
        MinecraftServer server = player.getServer();
        if (server == null || player.isSpectator())
            return false;

        BodySurfaceDefinition surface = body.surface().orElse(null);
        if (surface == null)
            return false;

        ResourceKey<Level> dimensionKey = dimensionKey(surface.dimension());
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null)
            return false;

        // The overworld uses the respawn anchor so a player returns to their bed;
        // every other body scans for a safe column.
        if (surface.landingPolicy() == BodySurfaceDefinition.LandingPolicy.OVERWORLD_RESPAWN)
        {
            teleportToOverworldSpawn(player, true);
            return true;
        }

        player.stopRiding();
        BlockPos spawn = findSurfaceSpawn(level);
        player.teleportTo(level,
                spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        return true;
    }

    /**
     * Sends a player to the overworld, optionally honouring their respawn anchor.
     *
     * <p>Also the shared fallback for a missing dimension, which is why it takes
     * the flag rather than always using the anchor.</p>
     */
    public static void teleportToOverworldSpawn(ServerPlayer player, boolean useRespawn)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
            return;
        ServerLevel overworld = server.overworld();
        BlockPos target = overworld.getSharedSpawnPos();
        float yaw = overworld.getSharedSpawnAngle();
        if (useRespawn && player.getRespawnPosition() != null
                && Level.OVERWORLD.equals(player.getRespawnDimension()))
        {
            target = player.getRespawnPosition();
            yaw = player.getRespawnAngle();
        }
        player.stopRiding();
        player.teleportTo(overworld, target.getX() + 0.5, target.getY() + 0.1, target.getZ() + 0.5,
                yaw, 0.0F);
    }

    /** Converts a surface definition's dimension id to a level key. */
    public static ResourceKey<Level> dimensionKey(ResourceLocation dimension)
    {
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension);
    }

    /**
     * Finds a column whose top is solid and not fluid.
     *
     * <p>Teleporting into an open lava lake would be unpleasant, so nearby columns
     * are tried in outward rings before giving up on the default spawn.</p>
     */
    static BlockPos findSurfaceSpawn(ServerLevel level)
    {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int centerX = DEFAULT_SPAWN.getX();
        int centerZ = DEFAULT_SPAWN.getZ();

        for (int radius = 0; radius <= MAX_SEARCH_RADIUS; radius += SEARCH_STEP)
        {
            for (int dx = -radius; dx <= radius; dx += SEARCH_STEP)
            {
                for (int dz = -radius; dz <= radius; dz += SEARCH_STEP)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius)
                        continue;

                    pos.set(centerX + dx, 0, centerZ + dz);
                    int topY = findTopSolidY(level, pos);
                    if (topY > level.getMinBuildHeight() + 1)
                        return pos.setY(topY + 1).immutable();
                }
            }
        }
        return DEFAULT_SPAWN;
    }

    private static int findTopSolidY(ServerLevel level, BlockPos.MutableBlockPos pos)
    {
        int x = pos.getX();
        int z = pos.getZ();
        for (int y = level.getMaxBuildHeight() - 1; y > level.getMinBuildHeight(); y--)
        {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (!state.isAir() && state.getFluidState().isEmpty())
                return y;
        }
        return level.getMinBuildHeight();
    }
}
