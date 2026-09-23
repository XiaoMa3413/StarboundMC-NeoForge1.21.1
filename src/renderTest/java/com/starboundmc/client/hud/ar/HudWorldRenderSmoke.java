// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.hud.HudBootController;
import com.starboundmc.client.hud.provider.*;
import com.starboundmc.story.*;
import com.starboundmc.warp.ShipStateData;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Opt-in integrated-server check. Creates only a fresh world in an isolated run directory. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class HudWorldRenderSmoke {
    private static final String WORLD = "hud-validation-" + System.currentTimeMillis();
    private static final ResourceLocation TARGET = ResourceLocation.fromNamespaceAndPath("starboundmc", "smoke_planet");
    private static final Set<String> CAPTURES = new HashSet<>();
    private static boolean started, finished, identified, changingWorld;
    private static int stage;
    private static long start, stageStart;
    private static final StringBuilder report = new StringBuilder();
    private static CompletableFuture<?> serverWork;
    private static double frozenArTime;

    @SubscribeEvent
    public static void afterFrame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.hudWorldSmoke") || finished || changingWorld) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true;
            start = System.nanoTime();
            mc.options.pauseOnLostFocus = false;
            mc.options.guiScale().set(3);
            mc.options.renderDistance().set(6);
            mc.resizeDisplay();
            mc.createWorldOpenFlows().createFreshLevel(WORLD,
                    new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(3413, false, false),
                    registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                            .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return;
        }
        if (seconds(start) > 240) throw new IllegalStateException("HUD world check timed out at stage " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        if (stageStart == 0) stageStart = System.nanoTime();
        var boot = HudBootController.INSTANCE;
        float age = (float) seconds(stageStart);
        if (stage == 0) {
            var presentation = boot.presentation(0);
            if (mc.screen == null && presentation.statusOpacity() > .8)
                capture("boot-" + presentation.cue());
            if (age < 14 || boot.state() != HudBootController.State.SAFE_MODE || boot.defersCommunication()) return;
            require(CAPTURES.contains("boot-VISUAL_RESTORE") && CAPTURES.contains("boot-SAFE_MODE"), "fresh world ran real wake boot");
            capture("terminal-guidance");
            serverWork = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer();
                var player = server.getPlayerList().getPlayers().getFirst();
                // This fresh fixture knowingly uses modded dimensions; suppress only its repeat warning.
                if (server.getWorldData() instanceof net.minecraft.world.level.storage.PrimaryLevelData data)
                    data.withConfirmedWarning(true);
                player.setData(ModAttachments.PLAYER_STORY, player.getData(ModAttachments.PLAYER_STORY)
                        .withFlag(PlayerStoryFlag.TERMINAL_CONTACTED));
                ShipStateData.get(server).beginCoreReboot(server.overworld().getGameTime(), 50);
                HudStateService.syncAll(server);
            });
            next();
        } else if (stage == 1) {
            if (boot.state() != HudBootController.State.LINKING) return;
            mc.setScreen(new InventoryScreen(mc.player));
            next();
        } else if (stage == 2) {
            if (age < 4) return;
            require(boot.state() == HudBootController.State.ONLINE, "server ONLINE arrives while inventory is open");
            require(boot.defersCommunication(), "hidden completion waits for visible presentation");
            mc.setScreen(null);
            next();
        } else if (stage == 3) {
            capture("core-online");
            if (age < 4) return;
            require(!boot.defersCommunication(), "core completion releases communication");
            serverWork = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer();
                var player = server.getPlayerList().getPlayers().getFirst();
                if (!player.getData(ModAttachments.PLAYER_STORY).hasFlag(PlayerStoryFlag.HUD_CORE_LINK_PRESENTED))
                    throw new IllegalStateException("server did not persist the presentation receipt");
                player.teleportTo(server.overworld(), .5, -60, .5, Set.of(), 0, 0);
                server.overworld().setBlockAndUpdate(new net.minecraft.core.BlockPos(0, -60, 16),
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                server.overworld().setBlockAndUpdate(new net.minecraft.core.BlockPos(0, -59, 16),
                        net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState());
            });
            next();
        } else if (stage == 4) {
            if (!serverWork.isDone() || !mc.level.dimension().equals(Level.OVERWORLD) || mc.screen != null) return;
            serverWork.join();
            // Test provider joins the real singleton pass. No production test API or gameplay provider is added.
            set(ArWorldRenderer.INSTANCE, "collector", new ArTargetCollector(List.of(
                    new TutorialTargetProvider(), new ShipBeaconProvider(), new RelayPoiProvider(),
                    (context, output) -> {
                        if (!context.dimension().equals(Level.OVERWORLD)) return;
                        output.accept(new ArTarget(TARGET, ArTargetCategory.POI,
                                new Vec3(.5, -58.5, 16.5), Component.literal(identified ? "ABANDONED RELAY  16m" : "UNKNOWN SIGNAL  16m"),
                                identified ? ArGuidanceMode.TARGET : ArGuidanceMode.SIGNAL, 80, 100, 0x95E8E2,
                                identified ? "relay" : "signal"));
                    })));
            next();
        } else if (stage == 5) {
            if (age < 2) return;
            require(ArWorldRenderer.INSTANCE.hasTargets(), "normal-world provider reaches registered AR pass outside EVA");
            require(targetState().known(), "front target finishes acquisition");
            capture("planet-front");
            mc.player.setYRot(100);
            next();
        } else if (stage == 6) {
            if (age < 1) return;
            capture("planet-edge");
            mc.player.setYRot(180);
            next();
        } else if (stage == 7) {
            if (age < 1) return;
            capture("planet-behind");
            mc.player.setYRot(0);
            next();
        } else if (stage == 8) {
            if (age < .5) return;
            require(targetState().recovering(), "reentry preserves recognition memory");
            capture("planet-reentry");
            identified = true;
            next();
        } else if (stage == 9) {
            if (age < 1) return;
            require(targetState().previousLabel() == null && targetState().identityBlend() == 1, "identity change completes");
            capture("planet-identified");
            mc.options.hideGui = true;
            frozenArTime = arTime();
            next();
        } else if (stage == 10) {
            if (age < 1) return;
            require(arTime() == frozenArTime, "F1 freezes AR presentation");
            capture("f1");
            mc.options.hideGui = false;
            mc.setScreen(new PauseScreen(true));
            next();
        } else if (stage == 11) {
            if (age < 1) return;
            require(arTime() == frozenArTime, "pause menu freezes AR presentation");
            mc.setScreen(null);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            next();
        } else if (stage == 12) {
            if (age < 1) return;
            require(arTime() == frozenArTime, "third-person camera suspends AR");
            capture("third-person");
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            var camera = new ArmorStand(mc.level, .5, -60, 2.5);
            mc.setCameraEntity(camera);
            next();
        } else if (stage == 13) {
            if (age < 1) return;
            require(targetState().known(), "camera entity change retains known targets");
            mc.setCameraEntity(mc.player);
            mc.options.fov().set(110);
            next();
        } else if (stage == 14) {
            if (age < 1) return;
            capture("fov-110");
            mc.options.fov().set(70);
            // Reconnect through the actual save/load and login path.
            next();
            changingWorld = true;
            try {
                mc.level.disconnect();
                mc.disconnect(new TitleScreen());
            } finally { changingWorld = false; }
        } else if (stage == 15) {
            // Handled before the world-presence guard below on the next frame.
        } else if (stage == 16) {
            if (age < 2 || mc.screen != null) return;
            require(boot.state() == HudBootController.State.ONLINE && !boot.defersCommunication(), "reconnect skips completed personal link");
            capture("reconnect-online");
            Files.writeString(mc.gameDirectory.toPath().resolve("hud-world-passed.txt"), report);
            finished = true;
            mc.stop();
        }
    }

    @SubscribeEvent
    public static void reconnect(RenderFrameEvent.Pre event) {
        if (!Boolean.getBoolean("starboundmc.debug.hudWorldSmoke") || changingWorld || stage != 15) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null && mc.screen instanceof TitleScreen) {
            next();
            mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("reconnect failed"); });
        }
    }

    private static ArVisualStateCache.State targetState() throws Exception {
        var visuals = get(ArWorldRenderer.INSTANCE, "visuals");
        return (ArVisualStateCache.State) ((Map<?, ?>) get(visuals, "states")).get(TARGET);
    }
    private static double arTime() throws Exception { return (double) get(get(ArWorldRenderer.INSTANCE, "visuals"), "time"); }
    private static Object get(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static void set(Object owner, String name, Object value) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }
    private static double seconds(long time) { return (System.nanoTime() - time) / 1e9; }
    private static void next() { stage++; stageStart = System.nanoTime(); }
    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
        report.append("PASS: ").append(message).append('\n');
        org.slf4j.LoggerFactory.getLogger(HudWorldRenderSmoke.class).info("HUD WORLD PASS: {}", message);
    }
    private static void capture(String name) throws Exception {
        if (!CAPTURES.add(name)) return;
        var mc = Minecraft.getInstance();
        var folder = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(folder);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(folder.resolve(name + ".png"));
        }
    }
}
