package com.starboundmc.story;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.menu.StarmapTerminalMenu;
import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.ShipEnvironmentSnapshotPacket;
import com.starboundmc.warp.ShipStateData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server authority and synchronization boundary for ship-connected screens. */
public final class ShipEnvironmentService
{
    private static final int MAX_REBOOT_TICKS = Integer.MAX_VALUE;

    private ShipEnvironmentService()
    {
    }

    /** Sends the current shared environment state to one valid open menu. */
    public static void sendSnapshot(ServerPlayer player, int containerId)
    {
        if (!validOpenMenu(player, containerId))
            return;
        MinecraftServer server = player.getServer();
        if (server == null)
            return;
        ModNetwork.sendToPlayer(player, snapshotFor(
                containerId, ShipStateData.get(server).getStoryProgress(),
                server.overworld().getGameTime()));
    }

    /** Refreshes all currently open star-map and teleporter screens. */
    public static void syncOpenMenus(MinecraftServer server)
    {
        if (server == null)
            return;
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player.containerMenu instanceof StarmapTerminalMenu menu
                    && validStarmap(player, menu))
            {
                sendSnapshot(player, menu.containerId);
            }
            else if (player.containerMenu instanceof TeleporterMenu menu
                    && validTeleporter(player, menu))
            {
                sendSnapshot(player, menu.containerId);
            }
        }
    }

    public static boolean isCoreOnline(MinecraftServer server)
    {
        return server != null
                && ShipStateData.get(server).getStoryProgress().canUseTeleporter();
    }

    public static boolean canTravelWithinSystem(MinecraftServer server)
    {
        return server != null
                && ShipStateData.get(server).getStoryProgress().canTravelWithinSystem();
    }

    public static boolean canTravelBetweenSystems(MinecraftServer server)
    {
        return server != null
                && ShipStateData.get(server).getStoryProgress().canTravelBetweenSystems();
    }

    static ShipEnvironmentSnapshotPacket snapshotFor(int containerId,
                                                      SharedShipProgress progress,
                                                      long gameTime)
    {
        long remaining = progress.core() == CoreState.REBOOTING
                ? Math.max(0L, progress.rebootCompleteGameTime() - gameTime) : 0L;
        int remainingTicks = (int) Math.min(MAX_REBOOT_TICKS, remaining);
        return new ShipEnvironmentSnapshotPacket(containerId, progress.schemaVersion(),
                progress.revision(), progress.core(), progress.sublightEngine(),
                progress.hyperdrive(), remainingTicks);
    }

    private static boolean validOpenMenu(ServerPlayer player, int containerId)
    {
        if (player == null || player.isSpectator() || player.containerMenu.containerId != containerId)
            return false;
        if (player.containerMenu instanceof StarmapTerminalMenu menu)
            return validStarmap(player, menu);
        if (player.containerMenu instanceof TeleporterMenu menu)
            return validTeleporter(player, menu);
        return false;
    }

    private static boolean validStarmap(ServerPlayer player, StarmapTerminalMenu menu)
    {
        return menu.isBoundToBlock()
                && menu.stillValid(player)
                && player.level().getBlockState(menu.blockPos()).is(ModBlocks.STARMAP_TERMINAL.get());
    }

    private static boolean validTeleporter(ServerPlayer player, TeleporterMenu menu)
    {
        return menu.isBoundToBlock()
                && menu.stillValid(player)
                && player.level().getBlockState(menu.pos).is(ModBlocks.TELEPORTER.get());
    }
}
