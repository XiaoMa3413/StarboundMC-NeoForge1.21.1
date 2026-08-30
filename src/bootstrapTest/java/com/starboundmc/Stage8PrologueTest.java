package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static wiring checks for the first playable prologue cue chain. */
final class Stage8PrologueTest
{
    @Test
    void newPlayersReceiveOnlyTheMatterManipulatorStarterItem()
            throws IOException
    {
        String spawn = source("event/SpawnHandler.java");
        assertTrue(spawn.contains("ModItems.MATTER_MANIPULATOR.get()"));
        assertFalse(spawn.contains("ModItems.TELEPORTER_ITEM.get()"));
    }

    @Test
    void broadcastSchedulerIsBoundToPlayerLifecycleAndServerTicks()
            throws IOException
    {
        String events = source("event/ShipStoryEvents.java");
        String story = source("story/ShipStoryService.java");
        String service = source("story/ShipStoryBroadcastService.java");
        assertTrue(events.contains("PlayerLoggedInEvent"));
        assertTrue(events.contains("PlayerLoggedOutEvent"));
        assertTrue(events.contains("RegisterCommandsEvent"));
        assertTrue(events.contains("Commands.LEVEL_GAMEMASTERS"));
        assertTrue(events.contains("restore_engine"));
        assertTrue(events.contains("restoreSublightEngine"));
        assertTrue(events.contains("restoreHyperdrive"));
        assertTrue(events.contains("syncOpenScreens"));
        assertTrue(story.contains("ShipStoryBroadcastService.tick"));
        assertTrue(service.contains("INITIAL_WAKE_DELAY_TICKS"));
        assertTrue(service.contains("TERMINAL_REMINDER_DELAY_TICKS"));
        assertTrue(service.contains("SURFACE_TUTORIAL_DELAY_TICKS = 120L"));
        assertTrue(service.contains("isPlanetSurface"));
        assertTrue(service.contains("scheduleMatterManipulatorTutorial"));
        assertTrue(service.contains("withTutorialSeen"));
        assertTrue(service.contains("novaMessage"));
        assertTrue(service.contains("ChatFormatting.AQUA"));
        assertTrue(service.contains("ChatFormatting.WHITE"));
        assertTrue(service.contains("displayClientMessage"));
    }

    @Test
    void terminalContactCancelsReminderAndLandingUsesAuthoritativeTravelCallbacks()
            throws IOException
    {
        String broadcast = source("story/ShipStoryBroadcastService.java");
        String story = source("story/ShipStoryService.java");
        String travel = source("world/Stage6TravelService.java");
        String teleporter = source("world/TeleporterManager.java");
        assertTrue(broadcast.contains("onTerminalOpened"));
        assertTrue(broadcast.contains("TERMINAL_CONTACTED"));
        assertTrue(broadcast.contains("SURFACE_ARRIVAL_BROADCAST"));
        assertTrue(story.contains("onPlanetSurfaceArrival"));
        assertTrue(story.contains("sendSurfaceArrivalOnce"));
        assertTrue(travel.contains("ShipStoryService.onPlanetSurfaceArrival(player)"));
        assertTrue(teleporter.contains("ShipStoryService.onPlanetSurfaceArrival(player)"));
    }

    @Test
    void firstWoodPickupUsesAOneShotServerSideCue()
            throws IOException
    {
        String events = source("event/ShipStoryEvents.java");
        String service = source("story/ShipStoryBroadcastService.java");
        String flags = source("story/PlayerStoryFlag.java");
        assertTrue(events.contains("ItemEntityPickupEvent.Post"));
        assertTrue(events.contains("ShipStoryBroadcastService.onWoodAcquired"));
        assertTrue(service.contains("ItemTags.LOGS"));
        assertTrue(service.contains("SurfaceMissionState.COMPLETE"));
        assertTrue(service.contains("WOOD_ACQUIRED_BROADCAST"));
        assertTrue(flags.contains("WOOD_ACQUIRED_BROADCAST(0x10)"));
    }

    @Test
    void novaBroadcastsHaveBothLanguageEntries()
            throws IOException
    {
        String english = Files.readString(Path.of(
                "src/main/resources/assets/starboundmc/lang/en_us.json"));
        String chinese = Files.readString(Path.of(
                "src/main/resources/assets/starboundmc/lang/zh_cn.json"));
        for (String key : new String[]{
                "message.starboundmc.nova.prologue.emergency",
                "message.starboundmc.nova.prologue.locate_terminal",
                "message.starboundmc.nova.prologue.core_online",
                "message.starboundmc.nova.prologue.first_landing_complete",
                "message.starboundmc.nova.tutorial.matter_manipulator",
                "message.starboundmc.nova.tutorial.wood_acquired"})
        {
            assertTrue(english.contains("\"" + key + "\""), key + " en_us");
            assertTrue(chinese.contains("\"" + key + "\""), key + " zh_cn");
        }
        assertFalse(english.contains("message.starboundmc.nova.tutorial.matter_manipulator\": \"[N.O.V.A.]"));
        assertFalse(chinese.contains("message.starboundmc.nova.tutorial.matter_manipulator\": \"[N.O.V.A.]"));
        assertFalse(english.contains("left-click does not perform normal mining"));
        assertFalse(chinese.contains("左键不会执行普通挖掘"));
        assertFalse(english.contains("Left-click mining is disabled"));
        assertFalse(chinese.contains("左键无法挖掘"));
        assertTrue(chinese.contains("通讯链路已连接"));
        assertTrue(chinese.contains("存活概率上升了17个百分点"));
    }

    @Test
    void terminalKeepsDialogueBodiesWhite()
            throws IOException
    {
        String terminal = source("client/shipai/ShipAiTerminalRoot.java");
        assertTrue(terminal.contains("BODY_COLOR = 0xFFFFFFFF"));
    }

    private static String source(String relativePath) throws IOException
    {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }
}
