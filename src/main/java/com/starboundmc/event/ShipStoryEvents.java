package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.story.ShipStoryBroadcastService;
import com.starboundmc.story.ShipStoryService;
import com.starboundmc.warp.ShipStateData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Keeps server-owned prologue timers progressing even while every UI is closed. */
@EventBusSubscriber(modid = StarboundMC.MODID)
public final class ShipStoryEvents
{
    private ShipStoryEvents()
    {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        ShipStoryService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            ShipStoryBroadcastService.onPlayerLoggedIn(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            ShipStoryBroadcastService.onPlayerLoggedOut(player);
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event)
    {
        if (event.getPlayer() instanceof ServerPlayer player)
            ShipStoryBroadcastService.onWoodAcquired(player, event.getOriginalStack());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        ShipStoryBroadcastService.reset();
    }

    /** Admin-only progression helpers used while engine repair quests are unfinished. */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("starboundmc")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("debug")
                        .then(Commands.literal("restore_engine")
                                .then(Commands.literal("sublight")
                                        .executes(context -> restoreEngines(
                                                context.getSource(), true, false,
                                                "command.starboundmc.debug.engine.sublight_restored")))
                                .then(Commands.literal("hyperdrive")
                                        .executes(context -> restoreEngines(
                                                context.getSource(), false, true,
                                                "command.starboundmc.debug.engine.hyperdrive_restored")))
                                .then(Commands.literal("all")
                                        .executes(context -> restoreEngines(
                                                context.getSource(), true, true,
                                                "command.starboundmc.debug.engine.all_restored"))))));
    }

    private static int restoreEngines(CommandSourceStack source,
                                      boolean restoreSublight,
                                      boolean restoreHyperdrive,
                                      String successTranslationKey)
    {
        MinecraftServer server = source.getServer();
        ShipStateData ship = ShipStateData.get(server);
        boolean changed = false;
        if (restoreSublight)
            changed = ship.restoreSublightEngine();
        if (restoreHyperdrive)
            changed = ship.restoreHyperdrive() || changed;

        if (!changed)
        {
            source.sendFailure(Component.translatable(
                    "command.starboundmc.debug.engine.not_restored"));
            return 0;
        }

        ShipStoryService.syncOpenScreens(server);
        source.sendSuccess(() -> Component.translatable(successTranslationKey), true);
        return 1;
    }
}
