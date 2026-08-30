package com.starboundmc.story;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.menu.ShipAiTerminalMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.ShipAiActionPacket;
import com.starboundmc.network.ShipStorySnapshotPacket;
import com.starboundmc.warp.ShipStateData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-main-thread authority for terminal actions and owner-specific snapshots. */
public final class ShipStoryService
{
    public static final long CORE_REBOOT_TICKS = 50L;

    private ShipStoryService()
    {
    }

    public static void sendSnapshot(ServerPlayer player, int containerId)
    {
        if (!validOpenTerminal(player, containerId))
            return;
        MinecraftServer server = player.getServer();
        if (server == null)
            return;

        SharedShipProgress shared = ShipStateData.get(server).getStoryProgress();
        PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);
        ModNetwork.sendToPlayer(player, snapshotFor(
                containerId, 0L, shared, personal, server.overworld().getGameTime()));
    }

    static ShipStorySnapshotPacket snapshotFor(int containerId,
                                               long acknowledgedRequestId,
                                               SharedShipProgress shared,
                                               PlayerStoryState personal,
                                               long gameTime)
    {
        long remaining = shared.core() == CoreState.REBOOTING
                ? Math.max(0L, shared.rebootCompleteGameTime() - gameTime) : 0L;
        int remainingTicks = (int) Math.min(Integer.MAX_VALUE, remaining);

        return new ShipStorySnapshotPacket(
                containerId,
                acknowledgedRequestId,
                shared.schemaVersion(),
                shared.revision(),
                shared.core(),
                shared.surfaceMission(),
                shared.sublightEngine(),
                shared.hyperdrive(),
                remainingTicks,
                personal.schemaVersion(),
                personal.revision(),
                personal.identityConfirmed(),
                personal.readSituationMask(),
                personal.tutorialMask(),
                personal.dismissedHintMask());
    }

    public static void handleTerminalAction(ServerPlayer player, int containerId,
                                            long requestId,
                                            ShipAiActionPacket.Action action, int argument)
    {
        if (!validOpenTerminal(player, containerId))
            return;
        MinecraftServer server = player.getServer();
        if (server == null)
            return;

        ShipStateData ship = ShipStateData.get(server);
        SharedShipProgress shared = ship.getStoryProgress();
        PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);
        if (!terminalActionsSupported(shared, personal))
        {
            sendAcknowledgement(player, containerId, requestId);
            return;
        }
        boolean sharedChanged = false;

        switch (action)
        {
            case BEGIN_CORE_REBOOT -> sharedChanged = ship.beginCoreReboot(
                    server.overworld().getGameTime(), CORE_REBOOT_TICKS);
            case CONFIRM_IDENTITY ->
            {
                if (shared.core() == CoreState.ONLINE)
                    storePersonalIfChanged(player, personal, personal.confirmIdentity());
            }
            case MARK_SITUATION_READ ->
            {
                if (shared.core() == CoreState.ONLINE && personal.identityConfirmed())
                {
                    SituationTopic topic;
                    try
                    {
                        topic = SituationTopic.fromMask(argument);
                    }
                    catch (IllegalArgumentException ignored)
                    {
                        sendAcknowledgement(player, containerId, requestId);
                        return;
                    }
                    storePersonalIfChanged(player, personal, personal.withReadTopic(topic));
                }
            }
            case ACTIVATE_SURFACE_MISSION ->
            {
                if (shared.core() == CoreState.ONLINE && personal.identityConfirmed()
                        && personal.hasReadAllRequiredTopics())
                    sharedChanged = ship.activateSurfaceMission();
            }
        }

        if (sharedChanged)
        {
            syncOpenTerminalOwners(server, player, requestId);
        }
        else
            sendAcknowledgement(player, containerId, requestId);
    }

    public static void tick(MinecraftServer server)
    {
        ShipStateData ship = ShipStateData.get(server);
        if (ship.finishCoreRebootIfDue(server.overworld().getGameTime()))
            syncOpenTerminalOwners(server, null, 0L);
    }

    private static void storePersonalIfChanged(ServerPlayer player,
                                               PlayerStoryState previous,
                                               PlayerStoryState updated)
    {
        if (updated != previous)
            player.setData(ModAttachments.PLAYER_STORY, updated);
    }

    static boolean terminalActionsSupported(SharedShipProgress shared,
                                            PlayerStoryState personal)
    {
        return shared.isWritable()
                && shared.schemaVersion() <= SharedShipProgress.CURRENT_SCHEMA_VERSION
                && personal.schemaVersion() <= PlayerStoryState.CURRENT_SCHEMA_VERSION;
    }

    private static void sendAcknowledgement(ServerPlayer player, int containerId,
                                            long requestId)
    {
        if (!validOpenTerminal(player, containerId))
            return;
        MinecraftServer server = player.getServer();
        if (server == null)
            return;
        ModNetwork.sendToPlayer(player, snapshotFor(
                containerId, requestId,
                ShipStateData.get(server).getStoryProgress(),
                player.getData(ModAttachments.PLAYER_STORY),
                server.overworld().getGameTime()));
    }

    private static void syncOpenTerminalOwners(MinecraftServer server,
                                               ServerPlayer acknowledgedPlayer,
                                               long acknowledgedRequestId)
    {
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers())
        {
            if (onlinePlayer.containerMenu instanceof ShipAiTerminalMenu menu
                    && validOpenTerminal(onlinePlayer, menu.containerId))
            {
                long acknowledgement = onlinePlayer == acknowledgedPlayer
                        ? acknowledgedRequestId : 0L;
                ModNetwork.sendToPlayer(onlinePlayer, snapshotFor(
                        menu.containerId, acknowledgement,
                        ShipStateData.get(server).getStoryProgress(),
                        onlinePlayer.getData(ModAttachments.PLAYER_STORY),
                        server.overworld().getGameTime()));
            }
        }
    }

    private static boolean validOpenTerminal(ServerPlayer player, int containerId)
    {
        return !player.isSpectator()
                && player.containerMenu instanceof ShipAiTerminalMenu menu
                && menu.containerId == containerId
                && menu.isBoundToBlock()
                && menu.stillValid(player)
                && player.level().getBlockState(menu.blockPos()).is(ModBlocks.SHIP_AI_TERMINAL.get());
    }
}
