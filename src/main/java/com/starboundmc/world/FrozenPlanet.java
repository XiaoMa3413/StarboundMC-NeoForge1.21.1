package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The Frozen Planet dimension: overworld-like terrain restricted to cold biomes. */
public class FrozenPlanet
{
    public static final ResourceKey<Level> FROZEN_LEVEL =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "frozen"));

    private static final BlockPos DEFAULT_SPAWN = new BlockPos(8, 80, 8);

    public static void teleportToFrozen(ServerPlayer player)
    {
        if (player.getServer() == null)
            return;
        ServerLevel level = player.getServer().getLevel(FROZEN_LEVEL);
        if (level == null)
            return;

        BlockPos spawn = findSurfaceSpawn(level);
        player.teleportTo(level,
                spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    private static BlockPos findSurfaceSpawn(ServerLevel level)
    {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int centerX = DEFAULT_SPAWN.getX();
        int centerZ = DEFAULT_SPAWN.getZ();

        for (int radius = 0; radius <= 64; radius += 8)
        {
            for (int dx = -radius; dx <= radius; dx += 8)
            {
                for (int dz = -radius; dz <= radius; dz += 8)
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
