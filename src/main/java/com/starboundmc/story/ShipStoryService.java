package com.starboundmc.story;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.item.MatterManipulatorItem;
import com.starboundmc.item.ModItems;
import com.starboundmc.block.entity.ShipEngineBlockEntity;
import com.starboundmc.menu.ShipEngineMenu;
import com.starboundmc.world.ShipDimensions;
import com.starboundmc.menu.ShipAiTerminalMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.ShipAiActionPacket;
import com.starboundmc.network.ShipStorySnapshotPacket;
import com.starboundmc.warp.ShipStateData;
import com.starboundmc.world.BarrenPlanet;
import com.starboundmc.world.FrozenPlanet;
import com.starboundmc.world.MoltenPlanet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

/** Server-main-thread authority for terminal actions and owner-specific snapshots. */
public final class ShipStoryService
{
    public static final long CORE_REBOOT_TICKS = 50L;
    public static final long SUBLIGHT_IGNITION_TICKS = 60L;

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

        ShipStoryBroadcastService.onTerminalOpened(player);
        SharedShipProgress shared = ShipStateData.get(server).getStoryProgress();
        PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);
        ModNetwork.sendToPlayer(player, snapshotFor(
                containerId, 0L, shared, personal, server.overworld().getGameTime()).withTasks(NovaTaskService.refresh(player)));
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
                shared.mineralScan(),
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
            case SUBMIT_SUBLIGHT_REPAIR ->
            {
                // Retain wire ID 4 for older clients, but never accept terminal repair requests.
            }
            case CLAIM_TASK_REWARD -> {
                try { NovaTaskService.claim(player, NovaTask.fromId(argument)); }
                catch (IllegalArgumentException ignored) { }
            }
            case TRACK_TASK -> {
                NovaTaskProgress before = NovaTaskService.refresh(player);
                try {
                    NovaTaskProgress after = before.track(argument);
                    if (after != before) player.setData(ModAttachments.NOVA_TASKS, after);
                } catch (IllegalArgumentException ignored) { }
            }
        }

        if (sharedChanged)
        {
            syncOpenTerminalOwners(server, player, requestId);
            ShipEnvironmentService.syncOpenMenus(server);
        }
        else
            sendAcknowledgement(player, containerId, requestId);
    }

    public static void tick(MinecraftServer server)
    {
        ShipStateData ship = ShipStateData.get(server);
        if (ship.finishCoreRebootIfDue(server.overworld().getGameTime()))
        {
            ShipStoryBroadcastService.onCoreOnline(server);
            syncOpenTerminalOwners(server, null, 0L);
            ShipEnvironmentService.syncOpenMenus(server);
        }
        if (ship.finishSublightIgnitionIfDue(server.overworld().getGameTime()))
        {
            ShipStoryBroadcastService.onSublightEngineOnline(server);
            syncOpenTerminalOwners(server, null, 0L);
            ShipEnvironmentService.syncOpenMenus(server);
        }
        ShipStoryBroadcastService.tick(server);
        NovaTaskService.tick(server);
    }

    /**
     * Called only after a server-authoritative teleporter operation has placed
     * the player on a planet surface. The tutorial is personal; the mission
     * completion is shared by every player on the ship.
     */
    public static void onPlanetSurfaceArrival(ServerPlayer player)
    {
        if (player == null || player.isSpectator() || player.getServer() == null
                || !isPlanetSurface(player.level().dimension()))
            return;

        MinecraftServer server = player.getServer();
        ShipStateData ship = ShipStateData.get(server);
        SharedShipProgress shared = ship.getStoryProgress();
        boolean missionChanged = shared.surfaceMission() == SurfaceMissionState.ACTIVE
                && ship.completeSurfaceMission();

        PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);
        if (personal.isWritable() && !personal.hasSeenTutorial(TutorialTopic.MATTER_MANIPULATOR))
            ShipStoryBroadcastService.scheduleMatterManipulatorTutorial(player);

        // Mission completion is shared, but the arrival confirmation is a
        // personal cue so every player gets the same first sentence once.
        ShipStoryBroadcastService.sendSurfaceArrivalOnce(player);
        NovaTaskService.onSurfaceArrival(player);
        if (missionChanged)
        {
            syncOpenTerminalOwners(server, null, 0L);
            ShipEnvironmentService.syncOpenMenus(server);
        }
    }

    static boolean isPlanetSurface(ResourceKey<Level> dimension)
    {
        return Level.OVERWORLD.equals(dimension)
                || MoltenPlanet.MOLTEN_LEVEL.equals(dimension)
                || FrozenPlanet.FROZEN_LEVEL.equals(dimension)
                || BarrenPlanet.BARREN_LEVEL.equals(dimension);
    }

    /** Refreshes all open story-aware screens after an admin/debug state change. */
    public static void syncOpenScreens(MinecraftServer server)
    {
        if (server == null)
            return;
        syncOpenTerminalOwners(server, null, 0L);
        ShipEnvironmentService.syncOpenMenus(server);
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

    /** Called after a vanilla slot transaction, on the server thread. No inventory scanning for payment. */
    public static boolean installSublightCore(ServerPlayer player, ShipEngineBlockEntity engine)
    {
        if (player == null || player.isSpectator() || player.getServer() == null
                || engine == null || !engine.stillValid(player)
                || !player.level().dimension().equals(ShipDimensions.SHIP_LEVEL)
                || !player.level().getBlockState(engine.getBlockPos()).is(ModBlocks.SHIP_ENGINE_UNIT.get())
                || !(player.containerMenu instanceof ShipEngineMenu menu) || !menu.isBoundTo(engine)
                || !engine.getItem(0).is(ModItems.SUBLIGHT_IGNITION_CORE.get())
                || !hasUpgradedManipulator(player)) return false;
        MinecraftServer server = player.getServer();
        ShipStateData ship = ShipStateData.get(server);
        if (!acceptSublightCore(ship, engine, server.overworld().getGameTime()))
            return false;
        ShipStoryBroadcastService.onSublightIgnitionStarted(server);
        syncOpenScreens(server);
        return true;
    }

    /** Transaction boundary after player/menu authorization; also exercised against real registries in GameTest. */
    static boolean acceptSublightCore(ShipStateData ship, ShipEngineBlockEntity engine, long gameTime) {
        if (!engine.getBlockState().is(ModBlocks.SHIP_ENGINE_UNIT.get())) return false;
        if (!engine.getItem(0).is(ModItems.SUBLIGHT_IGNITION_CORE.get())) return false;
        // The shared state transition is the one-shot guard, including across multiple engine blocks.
        if (!ship.beginSublightIgnition(gameTime, SUBLIGHT_IGNITION_TICKS)) return false;
        engine.removeItem(0, 1);
        return true;
    }

    public static boolean hasUpgradedManipulator(ServerPlayer player)
    {
        if (player == null)
            return false;
        for (ItemStack stack : player.getInventory().items)
        {
            if (stack.getItem() instanceof MatterManipulatorItem
                    && MatterManipulatorItem.getMiningLevel(stack) >= 1)
                return true;
        }
        for (ItemStack stack : player.getInventory().offhand)
        {
            if (stack.getItem() instanceof MatterManipulatorItem
                    && MatterManipulatorItem.getMiningLevel(stack) >= 1)
                return true;
        }
        return false;
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
                server.overworld().getGameTime()).withTasks(NovaTaskService.refresh(player)));
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
                        server.overworld().getGameTime()).withTasks(NovaTaskService.refresh(onlinePlayer)));
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
