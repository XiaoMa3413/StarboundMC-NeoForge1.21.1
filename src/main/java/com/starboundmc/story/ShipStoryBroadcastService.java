package com.starboundmc.story;

import com.starboundmc.warp.ShipStateData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side scheduler for personal N.O.V.A. broadcasts.
 *
 * <p>Broadcast delivery is deliberately separate from the terminal menu. A
 * player may close the menu, move around the ship, or reconnect without
 * causing the same chat line to be emitted repeatedly.</p>
 */
public final class ShipStoryBroadcastService
{
    /** A short grace period lets the player finish spawning before chat opens. */
    public static final long INITIAL_WAKE_DELAY_TICKS = 20L;
    /** The locating hint is sent once, after roughly six seconds without contact. */
    public static final long TERMINAL_REMINDER_DELAY_TICKS = 120L;
    /** Give the player time to arrive and orient before the surface tutorial appears. */
    public static final long SURFACE_TUTORIAL_DELAY_TICKS = 120L;

    private static final Map<UUID, Long> wakeDueAt = new HashMap<>();
    private static final Map<UUID, Long> reminderDueAt = new HashMap<>();
    private static final Map<UUID, Long> tutorialDueAt = new HashMap<>();

    private ShipStoryBroadcastService()
    {
    }

    /** Schedules personal prologue cues for a player entering an offline ship. */
    public static void onPlayerLoggedIn(ServerPlayer player)
    {
        if (player == null || player.getServer() == null)
            return;

        MinecraftServer server = player.getServer();
        SharedShipProgress shared = ShipStateData.get(server).getStoryProgress();
        PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);
        boolean prologuePending = shared.isWritable() && shared.core() == CoreState.OFFLINE
                && personal.isWritable() && !personal.hasFlag(PlayerStoryFlag.TERMINAL_CONTACTED);
        if (!prologuePending)
            clearPending(player.getUUID());
        else
        {
            long now = server.overworld().getGameTime();
            if (!personal.hasFlag(PlayerStoryFlag.INITIAL_WAKE_BROADCAST))
                wakeDueAt.putIfAbsent(player.getUUID(), safeAdd(now, INITIAL_WAKE_DELAY_TICKS));

            if (!personal.hasFlag(PlayerStoryFlag.TERMINAL_REMINDER_BROADCAST))
            {
                long delay = personal.hasFlag(PlayerStoryFlag.INITIAL_WAKE_BROADCAST)
                        ? TERMINAL_REMINDER_DELAY_TICKS
                        : INITIAL_WAKE_DELAY_TICKS + TERMINAL_REMINDER_DELAY_TICKS;
                reminderDueAt.putIfAbsent(player.getUUID(), safeAdd(now, delay));
            }
        }

        // A reconnect during the landing grace period must not lose the
        // personal tutorial. New players are teleported to the ship shortly
        // afterward, so the surface check below naturally cancels that case.
        if (ShipStoryService.isPlanetSurface(player.level().dimension())
                && personal.isWritable()
                && !personal.hasSeenTutorial(TutorialTopic.MATTER_MANIPULATOR))
            scheduleMatterManipulatorTutorial(player);
    }

    /** Stops pending timers when a player leaves the server. */
    public static void onPlayerLoggedOut(ServerPlayer player)
    {
        if (player != null)
            clearPending(player.getUUID());
    }

    /** Schedules the personal surface tutorial without marking it read early. */
    public static boolean scheduleMatterManipulatorTutorial(ServerPlayer player)
    {
        if (player == null || player.getServer() == null || player.isSpectator())
            return false;
        PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);
        if (!personal.isWritable() || personal.hasSeenTutorial(TutorialTopic.MATTER_MANIPULATOR))
        {
            tutorialDueAt.remove(player.getUUID());
            return false;
        }
        long now = player.getServer().overworld().getGameTime();
        return tutorialDueAt.putIfAbsent(player.getUUID(),
                safeAdd(now, SURFACE_TUTORIAL_DELAY_TICKS)) == null;
    }

    /**
     * Sends the one-shot resource cue after the player picks up a wood log on
     * the planet surface. The mission and dimension checks keep early ship
     * inventory actions from consuming the personal cue.
     */
    public static boolean onWoodAcquired(ServerPlayer player, ItemStack stack)
    {
        if (player == null || stack == null || stack.isEmpty() || !stack.is(ItemTags.LOGS)
                || player.getServer() == null || player.isSpectator()
                || !ShipStoryService.isPlanetSurface(player.level().dimension()))
            return false;

        SharedShipProgress shared = ShipStateData.get(player.getServer()).getStoryProgress();
        if (!shared.isWritable() || shared.surfaceMission() != SurfaceMissionState.COMPLETE)
            return false;

        return sendOnce(player, PlayerStoryFlag.WOOD_ACQUIRED_BROADCAST,
                "message.starboundmc.nova.tutorial.wood_acquired");
    }

    /** Sends the immediate first-arrival confirmation once per player. */
    public static boolean sendSurfaceArrivalOnce(ServerPlayer player)
    {
        if (player == null || player.getServer() == null || player.isSpectator()
                || !ShipStoryService.isPlanetSurface(player.level().dimension()))
            return false;
        return sendOnce(player, PlayerStoryFlag.SURFACE_ARRIVAL_BROADCAST,
                "message.starboundmc.nova.prologue.first_landing_complete");
    }

    /** Marks the terminal as found and cancels only the locating reminder. */
    public static void onTerminalOpened(ServerPlayer player)
    {
        if (player == null)
            return;
        UUID id = player.getUUID();
        reminderDueAt.remove(id);

        PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);
        if (!personal.isWritable() || personal.hasFlag(PlayerStoryFlag.TERMINAL_CONTACTED))
            return;
        player.setData(ModAttachments.PLAYER_STORY,
                personal.withFlag(PlayerStoryFlag.TERMINAL_CONTACTED));
    }

    /** Emits due cues on the server thread. */
    public static void tick(MinecraftServer server)
    {
        if (server == null || server.overworld() == null)
            return;

        long now = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            UUID id = player.getUUID();
            SharedShipProgress shared = ShipStateData.get(server).getStoryProgress();
            PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);

            Long tutorialAt = tutorialDueAt.get(id);
            if (tutorialAt != null && now >= tutorialAt)
            {
                tutorialDueAt.remove(id);
                if (!ShipStoryService.isPlanetSurface(player.level().dimension()))
                    continue;
                sendMatterManipulatorTutorialOnce(player);
            }

            if (!shared.isWritable() || shared.core() != CoreState.OFFLINE
                    || !personal.isWritable() || personal.hasFlag(PlayerStoryFlag.TERMINAL_CONTACTED))
            {
                clearProloguePending(id);
                continue;
            }

            Long wakeAt = wakeDueAt.get(id);
            if (wakeAt != null && now >= wakeAt)
            {
                wakeDueAt.remove(id);
                if (sendOnce(player, PlayerStoryFlag.INITIAL_WAKE_BROADCAST,
                        "message.starboundmc.nova.prologue.emergency"))
                {
                    reminderDueAt.putIfAbsent(id, safeAdd(now, TERMINAL_REMINDER_DELAY_TICKS));
                }
            }

            Long reminderAt = reminderDueAt.get(id);
            if (reminderAt != null && now >= reminderAt)
            {
                reminderDueAt.remove(id);
                sendOnce(player, PlayerStoryFlag.TERMINAL_REMINDER_BROADCAST,
                        "message.starboundmc.nova.prologue.locate_terminal");
            }
        }
    }

    /** Broadcast sent once when the shared core changes from rebooting to online. */
    public static void onCoreOnline(MinecraftServer server)
    {
        if (server == null)
            return;
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            sendOnce(player, PlayerStoryFlag.CORE_ONLINE_BROADCAST,
                    "message.starboundmc.nova.prologue.core_online");
        }
    }

    /** Clears timers at a server lifecycle boundary. */
    public static void reset()
    {
        wakeDueAt.clear();
        reminderDueAt.clear();
        tutorialDueAt.clear();
    }

    /** Builds a localized message with a separately styled NOVA speaker name. */
    public static Component novaMessage(String translationKey)
    {
        return Component.literal("[N.O.V.A.] ").withStyle(ChatFormatting.AQUA)
                .append(Component.translatable(translationKey)
                        .withStyle(ChatFormatting.WHITE));
    }

    private static boolean sendOnce(ServerPlayer player, PlayerStoryFlag flag, String translationKey)
    {
        PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);
        if (!personal.isWritable() || personal.hasFlag(flag))
            return false;
        player.setData(ModAttachments.PLAYER_STORY, personal.withFlag(flag));
        player.displayClientMessage(novaMessage(translationKey), false);
        return true;
    }

    private static boolean sendMatterManipulatorTutorialOnce(ServerPlayer player)
    {
        PlayerStoryState personal = player.getData(ModAttachments.PLAYER_STORY);
        if (!personal.isWritable() || personal.hasSeenTutorial(TutorialTopic.MATTER_MANIPULATOR))
            return false;
        player.setData(ModAttachments.PLAYER_STORY,
                personal.withTutorialSeen(TutorialTopic.MATTER_MANIPULATOR));
        player.displayClientMessage(novaMessage(
                "message.starboundmc.nova.tutorial.matter_manipulator"), false);
        return true;
    }

    private static void clearPending(UUID id)
    {
        clearProloguePending(id);
        tutorialDueAt.remove(id);
    }

    private static void clearProloguePending(UUID id)
    {
        wakeDueAt.remove(id);
        reminderDueAt.remove(id);
    }

    private static long safeAdd(long value, long delta)
    {
        return value > Long.MAX_VALUE - delta ? Long.MAX_VALUE : value + Math.max(0L, delta);
    }
}
