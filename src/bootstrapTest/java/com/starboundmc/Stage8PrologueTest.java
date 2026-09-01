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
        assertTrue(events.contains("replay_mineral_scan"));
        assertTrue(events.contains("replayMineralScan"));
        assertTrue(events.contains("syncOpenScreens"));
        assertTrue(story.contains("ShipStoryBroadcastService.tick"));
        assertTrue(service.contains("INITIAL_WAKE_DELAY_TICKS"));
        assertTrue(service.contains("TERMINAL_REMINDER_DELAY_TICKS"));
        assertTrue(service.contains("SURFACE_TUTORIAL_DELAY_TICKS = 120L"));
        assertTrue(service.contains("MINERAL_SCAN_START_DELAY_TICKS = 300L"));
        assertTrue(service.contains("MINERAL_SCAN_RESULT_DELAY_TICKS = 100L"));
        assertTrue(service.contains("MINERAL_SCAN_CONCLUSION_DELAY_TICKS = 120L"));
        assertTrue(service.contains("beginMineralScan"));
        assertTrue(service.contains("advanceMineralScanIfDue"));
        assertTrue(service.contains("isPlanetSurface"));
        assertTrue(service.contains("scheduleMatterManipulatorTutorial"));
        assertTrue(service.contains("withTutorialSeen"));
        assertTrue(service.contains("novaMessage"));
        assertTrue(service.contains("ChatFormatting.AQUA"));
        assertTrue(service.contains("ChatFormatting.WHITE"));
        assertTrue(service.contains("new NovaBroadcastPacket"));
        assertTrue(service.contains("ModNetwork.sendToPlayer"));
        assertFalse(service.contains("player.displayClientMessage"));
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
                "message.starboundmc.nova.tutorial.wood_acquired",
                "message.starboundmc.nova.prologue.mineral_scan_started",
                "message.starboundmc.nova.prologue.mineral_scan_result",
                "message.starboundmc.nova.prologue.mineral_scan_sublight_hint"})
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
        assertTrue(chinese.contains("通信链路现已恢复"));
        assertTrue(chinese.contains("生存概率提高了 17 个百分点"));
        assertTrue(chinese.contains("地层矿物扫描中................"));
        assertTrue(english.contains("Mineral survey in progress................"));
        assertTrue(chinese.contains("\"gui.starboundmc.ship_ai.prologue.boot.restarting\": \""
                + "核心系统重启中................\""));
        assertTrue(english.contains("\"gui.starboundmc.ship_ai.prologue.boot.restarting\": \""
                + "Restarting core systems................\""));
        assertTrue(chinese.contains("少量钻石"));
        assertTrue(chinese.contains("幸运的是，我们目前位于一颗宜居星球的轨道上"));
        assertTrue(chinese.contains("核▒心状▓态：#%/无法读取"));
        assertFalse(chinese.contains("确认你仍然存活"));
        assertFalse(chinese.contains("恭喜你获得了木材"));
    }

    @Test
    void terminalKeepsDialogueBodiesWhite()
            throws IOException
    {
        String terminal = source("client/shipai/ShipAiTerminalRoot.java");
        assertTrue(terminal.contains("BODY_COLOR = 0xFFFFFFFF"));
    }

    @Test
    void longStatusDotsUseProgressCadence()
            throws IOException
    {
        String broadcastState = source("client/shipai/ClientNovaBroadcastState.java");
        String timeline = source("client/shipai/NovaBroadcastTimeline.java");
        String hud = source("client/shipai/NovaBroadcastHudLayer.java");
        String root = source("client/shipai/NovaBroadcastHudRoot.java");
        String registrar = source("client/Stage2ClientRegistrar.java");
        String terminal = source("client/shipai/ShipAiTerminalRoot.java");

        assertTrue(timeline.contains("MIN_PROGRESS_DOTS = 6"));
        assertTrue(timeline.contains("isProgressDot(active.body(), revealed)"));
        assertFalse(broadcastState.contains("rescaleChat"));
        assertTrue(broadcastState.contains("addMessage(historyMessage"));
        assertTrue(hud.contains("implements ModularHudLayer"));
        assertTrue(root.contains("new NovaPortraitElement()"));
        assertTrue(registrar.contains("VanillaGuiLayers.CHAT"));
        assertTrue(registrar.contains("NovaBroadcastHudLayer.INSTANCE"));
        assertTrue(terminal.contains("gui.starboundmc.ship_ai.prologue.boot.restarting"));
    }

    @Test
    void terminalProvidesDynamicSharedStatus()
            throws IOException
    {
        String terminal = source("client/shipai/ShipAiTerminalRoot.java");
        String english = Files.readString(Path.of(
                "src/main/resources/assets/starboundmc/lang/en_us.json"));
        String chinese = Files.readString(Path.of(
                "src/main/resources/assets/starboundmc/lang/zh_cn.json"));

        assertFalse(terminal.contains("reviewMatterManipulatorTutorial"));
        assertTrue(terminal.contains("shared.sublightEngine().id()"));
        assertTrue(terminal.contains("shared.hyperdrive().id()"));
        for (String key : new String[]{
                "gui.starboundmc.ship_ai.status.sublight.damaged",
                "gui.starboundmc.ship_ai.status.sublight.online",
                "gui.starboundmc.ship_ai.status.hyperdrive.damaged",
                "gui.starboundmc.ship_ai.status.hyperdrive.online"})
        {
            assertTrue(english.contains("\"" + key + "\""), key + " en_us");
            assertTrue(chinese.contains("\"" + key + "\""), key + " zh_cn");
        }
    }

    @Test
    void earlyManipulatorModuleRecipeSupportsTheDiamondObjective()
            throws IOException
    {
        String recipe = Files.readString(Path.of(
                "src/main/resources/data/starboundmc/recipe/matter_manipulator_module.json"));

        assertTrue(recipe.contains("\" I \""));
        assertTrue(recipe.contains("\"ILI\""));
        assertTrue(recipe.contains("minecraft:iron_ingot"));
        assertTrue(recipe.contains("minecraft:lapis_lazuli"));
        assertTrue(recipe.contains("\"count\": 2"));
        assertFalse(recipe.contains("starboundmc:titanium_ingot"));
        assertFalse(recipe.contains("starboundmc:durasteel_ingot"));
        assertFalse(recipe.contains("starboundmc:star_core_fragment"));
    }

    private static String source(String relativePath) throws IOException
    {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }
}
