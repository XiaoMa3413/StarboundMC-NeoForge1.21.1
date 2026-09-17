package com.starboundmc.world;

import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncStarStatePacket;
import com.starboundmc.story.ShipStoryService;
import com.starboundmc.warp.ShipFuelService;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.BodySurfaceDefinition;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Shared server-authoritative travel boundary.
 *
 * <p>Surface travel reads the destination from the body's {@code surface}
 * definition (migration step A8). The previous implementation switched on the
 * legacy planet enum and called a per-planet teleport, so a new body needed a new
 * enum constant plus a new branch here; now it only needs a definition.</p>
 *
 * <h2>Landing failures are refusals, not relocations</h2>
 *
 * <p>A landing that cannot be performed is refused rather than redirected to the
 * overworld. Sending the player somewhere else on failure is worse than doing
 * nothing: the overworld is a planet surface as far as the story code is
 * concerned, so a redirected landing used to complete the prologue's surface
 * mission and hand out its progress for a planet the player never reached.</p>
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
     * <p>Returns false without moving the player when the landing cannot be
     * performed, and reports why. The story hooks run only after a landing has
     * actually happened, so a refusal can never advance mission progress.</p>
     */
    public static boolean teleportToPlanetSurface(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || player.isSpectator()) {
            return false;
        }
        String currentEntryId = ShipWarpManager.currentEntryId();
        CelestialBodyDefinition body = UniverseNavigation.body(currentEntryId);

        String refusal = landingRefusal(body, hasSurfaceDimension(server, body));
        if (refusal != null) {
            player.displayClientMessage(refusalMessage(refusal, body), true);
            return false;
        }

        if (!SurfaceLandingService.teleportToSurface(player, body)) {
            // The refusal table above already covers every input that makes this
            // fail, so reaching here would mean the two disagree. Refuse rather
            // than relocate, and say so with the same message.
            player.displayClientMessage(refusalMessage(SURFACE_UNAVAILABLE_KEY, body), true);
            return false;
        }

        // Mission progression and the personal tutorial are driven only after the
        // authoritative teleport has placed the player in a surface level.
        ShipStoryService.onPlanetSurfaceArrival(player);
        syncState(player);
        return true;
    }

    /**
     * Why this body cannot be landed on, as a translation key, or null when it can.
     *
     * <p>Pure so the refusal table is testable without a server. It is separate
     * because the case it exists to prevent is invisible at the call site: a
     * fallback to the overworld looks like a successful landing to everything
     * downstream.</p>
     */
    static String landingRefusal(CelestialBodyDefinition body, boolean surfaceDimensionAvailable) {
        // The ship is parked at a body this world does not provide (a removed
        // datapack, or a save from a build that had more content). There is no
        // surface to send the player to, and guessing one would relocate them.
        if (body == null) {
            return UNKNOWN_LANDING_KEY;
        }
        // A body with no surface definition is orbit-only, which is how the gas
        // giant refuses a landing.
        if (!body.isLandable()) {
            return NO_SURFACE_KEY;
        }
        // The body declares a surface but its dimension is absent from this world.
        if (!surfaceDimensionAvailable) {
            return SURFACE_UNAVAILABLE_KEY;
        }
        return null;
    }

    /** Whether the body's declared surface dimension exists in this server's world. */
    private static boolean hasSurfaceDimension(MinecraftServer server, CelestialBodyDefinition body) {
        BodySurfaceDefinition surface = body == null ? null : body.surface().orElse(null);
        return surface != null
                && server.getLevel(SurfaceLandingService.dimensionKey(surface.dimension())) != null;
    }

    /**
     * The refusal message to show the player.
     *
     * <p>Every refusal but the unknown-location one names the body, and that one has
     * no body to name.</p>
     */
    private static Component refusalMessage(String translationKey, CelestialBodyDefinition body) {
        if (body == null || UNKNOWN_LANDING_KEY.equals(translationKey)) {
            return Component.translatable(translationKey);
        }
        return Component.translatable(translationKey, Component.translatable(body.nameKey()));
    }

    private static final String UNKNOWN_LANDING_KEY = "message.starboundmc.warp.unknown_landing";
    private static final String NO_SURFACE_KEY = "message.starboundmc.warp.no_landing";
    private static final String SURFACE_UNAVAILABLE_KEY =
            "message.starboundmc.warp.surface_unavailable";

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
