package com.starboundmc.world;

import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncStarStatePacket;
import com.starboundmc.story.ShipStoryService;
import com.starboundmc.warp.ShipFuelService;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Shared server-authoritative travel boundary. Missing datapack dimensions
 * always fall back to the overworld instead of stranding a player.
 *
 * <p>Surface travel reads the destination from the body's {@code surface}
 * definition (migration step A8). The previous implementation switched on the
 * legacy planet enum and called a per-planet teleport, so a new body needed a new
 * enum constant plus a new branch here; now it only needs a definition.</p>
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
            SurfaceLandingService.teleportToOverworldSpawn(player, false);
        } else {
            BlockPos destination = ShipDimensions.shipTeleporterDestination(ship);
            player.stopRiding();
            player.teleportTo(ship, destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }
        syncState(player);
        return true;
    }

    /**
     * Sends the player down to the surface of whichever body the ship is at.
     *
     * <p>The destination is the body's own dimension. A body with no surface
     * definition (the gas giant), or one whose dimension no longer exists, falls
     * back to the overworld rather than leaving the player in the ship.</p>
     */
    public static boolean teleportToPlanetSurface(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || player.isSpectator()) {
            return false;
        }
        String currentEntryId = ShipWarpManager.currentEntryId();
        CelestialBodyDefinition body = UniverseNavigation.body(currentEntryId);

        boolean landed = body != null && SurfaceLandingService.teleportToSurface(player, body);
        if (!landed) {
            // Ship at an unknown body, or a body with no landable surface.
            SurfaceLandingService.teleportToOverworldSpawn(player, true);
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
            // The star-state packet is the authoritative body identity. It also
            // carries the arrival cue, which is why it is sent here after a
            // surface teleport as well as on warp arrival.
            ModNetwork.sendToPlayer(player, new SyncStarStatePacket(
                    new java.util.ArrayList<>(ShipWarpManager.visitedEntries()),
                    ShipWarpManager.currentEntryId()));
        }
    }
}
