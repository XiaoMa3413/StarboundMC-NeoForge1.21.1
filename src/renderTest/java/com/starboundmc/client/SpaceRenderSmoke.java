package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.FreeFlightPoseProvider;
import com.starboundmc.client.space.SpaceRenderState;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Opt-in isolated world fixture; no user save or server flight state is changed. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class SpaceRenderSmoke {
    private static final String[] BODIES = {"sys1:lush", "sys1:barren", "sys1:molten",
            "sys2:frozen", "sys1:gasgiant", "sys1:rockymoon", null};
    private static final double[] FRAME_MS = new double[180];
    private static boolean started, finished;
    private static long start, stageStart, previousFrame;
    private static int stage, samples;
    private static CompletableFuture<?> setup;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.spaceSmoke") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true;
            start = System.nanoTime();
            if (Boolean.getBoolean("starboundmc.debug.spaceSmokeHidden"))
                GLFW.glfwHideWindow(mc.getWindow().getWindow());
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(4);
            mc.options.fov().set(70);
            StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(SpaceVisualQuality.CUSTOM);
            StarfieldClientConfig.STELLAR_VIEW_BACKGROUND_STARS.set(false);
            StarfieldClientConfig.SPACE_BACKGROUND_MODE.set(
                    "legacy".equalsIgnoreCase(System.getProperty("starboundmc.debug.spaceSmokeBackground"))
                    ? StarfieldClientConfig.BackgroundMode.LEGACY : StarfieldClientConfig.BackgroundMode.PROCEDURAL);
            String name = "space-fixture-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 240)
            throw new IllegalStateException("Space fixture timed out at stage " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null || mc.screen != null) return;
        if (setup == null) {
            setup = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer();
                var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
                var player = server.getPlayerList().getPlayers().getFirst();
                for (int x = 997; x <= 1003; x++) for (int z = 997; z <= 1003; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 188, z), Blocks.SEA_LANTERN.defaultBlockState());
                player.teleportTo(level, 1000.5, 192, 1000.5, Set.of(), 0, 0);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
            return;
        }
        if (!setup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        setup.join();
        if (stageStart == 0) selectView(mc);
        long now = System.nanoTime();
        if (previousFrame != 0 && samples < FRAME_MS.length)
            FRAME_MS[samples++] = (now - previousFrame) / 1e6;
        previousFrame = now;
        if ((now - stageStart) / 1e9 < 3 || samples == 0) return;
        var output = mc.gameDirectory.toPath().resolve("screenshots")
                .resolve(System.getProperty("starboundmc.debug.spaceSmokeLabel", "capture"));
        Files.createDirectories(output);
        String view = BODIES[stage] == null ? "deep-space" : BODIES[stage].replace(':', '-');
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(output.resolve(view + ".png"));
        }
        double[] sorted = Arrays.copyOf(FRAME_MS, samples);
        Arrays.sort(sorted);
        Files.writeString(output.resolve(view + ".txt"),
                "Frame wall time includes the full client; this is not isolated GPU time.\n"
                + "samples=" + samples + " p50_ms=" + sorted[samples / 2]
                + " p95_ms=" + sorted[Math.min(samples - 1, (int) (samples * .95))] + "\n"
                + "resolution=" + mc.getMainRenderTarget().width + "x" + mc.getMainRenderTarget().height + "\n"
                + GpuSpaceBackground.diagnostics() + "\n" + SpaceRenderProfiler.report());
        if (++stage == BODIES.length) {
            SpaceRenderState.resetPoseProvider();
            Files.writeString(output.resolve("complete.txt"), "Completed seven fixed space views.\n");
            finished = true;
            mc.stop();
        } else {
            stageStart = previousFrame = 0;
            samples = 0;
        }
    }

    private static void selectView(Minecraft mc) {
        String body = BODIES[stage];
        UniversePosition position = body == null ? UniversePosition.of(20000, 3000, -10000)
                : UniverseNavigation.universeDock(body);
        var delta = body == null ? new UniverseDelta(0, 0, 1)
                : position.deltaTo(UniverseNavigation.universeBodyPosition(body));
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x(), delta.z()));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y(), Math.hypot(delta.x(), delta.z())));
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        SpaceRenderState.setPoseProvider(new FixturePose(position, body));
        PlanetRenderer.resetSceneState();
        SpaceRenderProfiler.resetSamples();
        stageStart = System.nanoTime();
    }

    private record FixturePose(UniversePosition universePosition, String currentBodyId)
            implements FreeFlightPoseProvider {
        public UniverseDelta universeVelocity() { return new UniverseDelta(0, 0, 0); }
        public double yaw() { return 0; }
        public double pitch() { return 0; }
        public double roll() { return 0; }
        public String currentSystemHint() {
            return currentBodyId == null ? null : StarmapUniverse.systemIdOfEntry(currentBodyId);
        }
    }
}
