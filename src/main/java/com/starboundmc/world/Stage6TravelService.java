package com.starboundmc.world;

import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncPlanetPacket;
import com.starboundmc.story.ShipStoryService;
import com.starboundmc.warp.ShipFuelService;
import com.starboundmc.warp.ShipWarpManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Shared server-authoritative travel boundary. Missing datapack dimensions
 * always fall back to the overworld instead of stranding a player.
 */
public final class Stage6TravelService {
    public static final ResourceKey<Level> SHIP_LEVEL = ShipDimensions.SHIP_LEVEL;
    public static final BlockPos SHIP_POS = ShipDimensions.SHIP_POS;

    private Stage6TravelService() {
    }

    public static boolean teleportToShip(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || player.isSpectator()) {
            return false;
        }
        ServerLevel ship = server.getLevel(SHIP_LEVEL);
        if (ship == null) {
            teleportToOverworldSpawn(player, false);
        } else {
            player.stopRiding();
            player.teleportTo(ship, SHIP_POS.getX() + 0.5, SHIP_POS.getY(), SHIP_POS.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }
        syncState(player);
        return true;
    }

    public static boolean teleportToPlanetSurface(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || player.isSpectator()) {
            return false;
        }
        Planet current = ShipWarpManager.getCurrentPlanet();
        ResourceKey<Level> destination = switch (current) {
            case MOLTEN -> MoltenPlanet.MOLTEN_LEVEL;
            case FROZEN -> FrozenPlanet.FROZEN_LEVEL;
            case BARREN -> BarrenPlanet.BARREN_LEVEL;
            case LUSH -> Level.OVERWORLD;
        };
        if (Level.OVERWORLD.equals(destination) || server.getLevel(destination) == null) {
            teleportToOverworldSpawn(player, true);
        } else {
            player.stopRiding();
            switch (current) {
                case MOLTEN -> MoltenPlanet.teleportToMolten(player);
                case FROZEN -> FrozenPlanet.teleportToFrozen(player);
                case BARREN -> BarrenPlanet.teleportToBarren(player);
                case LUSH -> throw new IllegalStateException("Lush must use the overworld");
            }
        }
        // Mission progression and the personal tutorial are driven only after
        // the authoritative teleport has placed the player in a surface level.
        ShipStoryService.onPlanetSurfaceArrival(player);
        syncState(player);
        return true;
    }

    public static void syncState(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            ShipFuelService.syncToPlayer(player);
            ModNetwork.sendToPlayer(player, new SyncPlanetPacket(
                    com.starboundmc.warp.ShipStateData.get(server).getPlanet()));
        }
    }

    private static void teleportToOverworldSpawn(ServerPlayer player, boolean useRespawn) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        BlockPos target = overworld.getSharedSpawnPos();
        float yaw = overworld.getSharedSpawnAngle();
        if (useRespawn && player.getRespawnPosition() != null
                && Level.OVERWORLD.equals(player.getRespawnDimension())) {
            target = player.getRespawnPosition();
            yaw = player.getRespawnAngle();
        }
        player.stopRiding();
        player.teleportTo(overworld, target.getX() + 0.5, target.getY() + 0.1, target.getZ() + 0.5,
                yaw, 0.0F);
    }
}
