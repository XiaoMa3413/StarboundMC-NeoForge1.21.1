package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncPlanetPacket;
import com.starboundmc.warp.ShipFuelService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Stage 6 travel boundary. The real ship and planet dimensions are restored in
 * stage 8; until then, a missing destination safely falls back to overworld spawn.
 */
public final class Stage6TravelService {
    public static final ResourceKey<Level> SHIP_LEVEL = levelKey("ship");
    public static final BlockPos SHIP_POS = new BlockPos(0, 102, 0);

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
        // Custom planet dimensions and their safe landing rules belong to stage 8.
        teleportToOverworldSpawn(player, true);
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

    private static ResourceKey<Level> levelKey(String path) {
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, path));
    }
}
