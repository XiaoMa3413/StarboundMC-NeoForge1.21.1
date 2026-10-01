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
    private static final String[] BASE_VIEWS = {"sys1:lush", "sys1:barren", "sys1:molten",
            "sys2:frozen", "sys1:gasgiant", "sys1:rockymoon", null, "@stellar", "@galaxy",
            "@atmosphere", "@phase-day", "@phase-half", "@phase-crescent",
            "@terminator-close", "@twilight-atmosphere", "@phase-night", "@window", "@resize", "@reload"};
    private static final String[] BODIES = System.getProperty("starboundmc.debug.spaceSmokeViews") != null
            ? System.getProperty("starboundmc.debug.spaceSmokeViews").split(",")
            : Boolean.getBoolean("starboundmc.debug.spaceSmokeStellarMatrix")
            ? java.util.stream.Stream.concat(Arrays.stream(BASE_VIEWS),java.util.stream.Stream.of(
                    "@stellar-mode-textured-instanced", "@stellar-mode-textured-regular",
                    "@stellar-mode-plain-instanced", "@stellar-mode-plain-regular")).toArray(String[]::new)
            : BASE_VIEWS;
    private static final double[] FRAME_MS = new double[180];
    private static boolean started, finished;
    private static long start, stageStart, previousFrame;
    private static int stage, samples;
    private static float fixtureYaw, fixturePitch;
    private static CompletableFuture<?> setup;
    private static CompletableFuture<Void> reload;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.spaceSmoke") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true;
            Files.deleteIfExists(mc.gameDirectory.toPath().resolve("screenshots")
                    .resolve(System.getProperty("starboundmc.debug.spaceSmokeLabel", "capture")).resolve("complete.txt"));
            start = System.nanoTime();
            if (Boolean.getBoolean("starboundmc.debug.spaceSmokeHidden"))
                GLFW.glfwHideWindow(mc.getWindow().getWindow());
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(4);
            mc.options.fov().set(70);
            StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(SpaceVisualQuality.valueOf(
                    System.getProperty("starboundmc.debug.spaceSmokeQuality", "custom").toUpperCase(java.util.Locale.ROOT)));
            StarfieldClientConfig.SPACE_STARFIELD_BACKEND.set(Boolean.getBoolean("starboundmc.debug.spaceSmokeStellarView")
                    ? StarfieldClientConfig.StarfieldBackend.STELLAR_VIEW : StarfieldClientConfig.StarfieldBackend.NATIVE);
            StarfieldClientConfig.SPACE_PIPELINE_MODE.set(
                    "direct".equalsIgnoreCase(System.getProperty("starboundmc.debug.spaceSmokePipeline"))
                    ? StarfieldClientConfig.PipelineMode.DIRECT : StarfieldClientConfig.PipelineMode.ISOLATED);
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
                // Keep the fixture's support platform below the galaxy camera's field of view.
                player.teleportTo(level, 1000.5, 200, 1000.5, Set.of(), 0, 0);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
            return;
        }
        if (!setup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        setup.join();
        if ("@reload".equals(BODIES[stage])) {
            if (reload == null) { reload = mc.reloadResourcePacks(); return; }
            if (!reload.isDone()) return;
            reload.join();
        }
        if (stageStart == 0) selectView(mc);
        // Initial server teleport packets can otherwise overwrite the first
        // camera pose after selectView, producing a mislabeled reference image.
        mc.player.setYRot(fixtureYaw);
        mc.player.setXRot(fixturePitch);
        mc.player.yRotO = fixtureYaw;
        mc.player.xRotO = fixturePitch;
        long now = System.nanoTime();
        if (previousFrame != 0 && samples < FRAME_MS.length)
            FRAME_MS[samples++] = (now - previousFrame) / 1e6;
        previousFrame = now;
        if ((now - stageStart) / 1e9 < 3 || samples == 0) return;
        if (Boolean.getBoolean("starboundmc.debug.spaceSmokeStellarMatrix")) StellarShaderSmoke.verifyRestoration();
        var output = mc.gameDirectory.toPath().resolve("screenshots")
                .resolve(System.getProperty("starboundmc.debug.spaceSmokeLabel", "capture"));
        Files.createDirectories(output);
        String view = BODIES[stage] == null ? "deep-space" : BODIES[stage].replace(':', '-').replace("@", "");
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
                + "cameraYaw=" + fixtureYaw + " cameraPitch=" + fixturePitch + "\n"
                + GpuSpaceBackground.diagnostics() + "\n" + SpaceSceneTarget.diagnostics() + "\n"
                + "quality=" + StarfieldClientConfig.SPACE_VISUAL_QUALITY.get() + " "
                + com.starboundmc.client.compat.stellarview.StellarViewStarfield.diagnostics() + "\n"
                + SpaceRenderProfiler.report());
        if (++stage == BODIES.length) {
            Files.writeString(output.resolve("depth-checks.txt"), SpaceDepthSmoke.verify());
            SpaceRenderState.resetPoseProvider();
            if (Boolean.getBoolean("starboundmc.debug.spaceSmokeStellarMatrix")) StellarShaderSmoke.restore();
            Files.writeString(output.resolve("complete.txt"), "Completed " + BODIES.length + " fixed space views.\n");
            finished = true;
            mc.stop();
        } else {
            stageStart = previousFrame = 0;
            samples = 0;
        }
    }

    private static void selectView(Minecraft mc) {
        String view = BODIES[stage];
        String body = view != null && !view.startsWith("@") ? view : null;
        UniversePosition position = body == null ? UniversePosition.of(20000, 3000, -10000)
                : UniverseNavigation.universeDock(body);
        var delta = body == null ? new UniverseDelta(0, 0, 1)
                : position.deltaTo(UniverseNavigation.universeBodyPosition(body));
        if ("@stellar".equals(view) || "@window".equals(view)) {
            var star = StarmapUniverse.systemOf("sys1:lush").stellarVisual().getUniversePosition();
            position = star.add(new UniverseDelta(0, 0, 1700));
            delta = position.deltaTo(star);
        } else if ("@galaxy".equals(view)) delta = new UniverseDelta(.87, -.24, 0);
        else if ("@galaxy-outer".equals(view)) delta = new UniverseDelta(-.87,.24,0);
        else if ("@galaxy-side".equals(view)) {
            var direction = new org.joml.Vector3f(.24F,.87F,.43F)
                    .cross(new org.joml.Vector3f(.87F,-.24F,0)).normalize();
            delta = new UniverseDelta(direction.x,direction.y,direction.z);
        }
        else if ("@atmosphere".equals(view)) {
            body = "sys1:lush";
            var sun = UniverseNavigation.sunDirection(body).normalize();
            var tangent = sun.cross(Math.abs(sun.y) < .95
                    ? new net.minecraft.world.phys.Vec3(0,1,0) : new net.minecraft.world.phys.Vec3(1,0,0)).normalize();
            double distance = UniverseNavigation.radius(body)*1.015;
            position = UniverseNavigation.universeBodyPosition(body).add(
                    new UniverseDelta(sun.x*distance,sun.y*distance,sun.z*distance));
            var direction = tangent.scale(.98).subtract(sun.scale(.2));
            delta = new UniverseDelta(direction.x,direction.y,direction.z);
        }
        else if (view != null && (view.startsWith("@phase-") || view.equals("@terminator-close")
                || view.equals("@twilight-atmosphere"))) {
            body = "sys1:lush";
            var sun = UniverseNavigation.sunDirection(body).normalize();
            var tangent = sun.cross(Math.abs(sun.y) < .95
                    ? new net.minecraft.world.phys.Vec3(0,1,0) : new net.minecraft.world.phys.Vec3(1,0,0)).normalize();
            var up = switch (view) {
                case "@phase-day" -> sun;
                case "@phase-night" -> sun.scale(-1);
                case "@phase-crescent" -> tangent.scale(.48).subtract(sun.scale(.877)).normalize();
                case "@twilight-atmosphere" -> tangent.subtract(sun.scale(.015)).normalize();
                default -> tangent;
            };
            double distance = UniverseNavigation.radius(body) * (view.equals("@terminator-close") ? 1.12
                    : view.equals("@twilight-atmosphere") ? 1.003 : 2.8);
            position = UniverseNavigation.universeBodyPosition(body).add(new UniverseDelta(up.x*distance,up.y*distance,up.z*distance));
            delta = position.deltaTo(UniverseNavigation.universeBodyPosition(body));
            if (view.equals("@twilight-atmosphere")) {
                var direction = sun.subtract(up.scale(sun.dot(up)+.07)).normalize();
                delta = new UniverseDelta(direction.x,direction.y,direction.z);
            }
        }
        else if (view != null && view.startsWith("@stellar-mode-")) {
            StellarShaderSmoke.configure(view.contains("textured"),view.endsWith("instanced"));
            delta = new UniverseDelta(.87,-.24,0);
        }
        else if (view != null && view.startsWith("@quality-")) {
            StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(SpaceVisualQuality.valueOf(
                    view.substring("@quality-".length()).toUpperCase(java.util.Locale.ROOT)));
            delta = new UniverseDelta(.87,-.24,0);
        }
        if ("@resize".equals(view)) GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 960, 540);
        if ("@window".equals(view)) {
            mc.getSingleplayerServer().submit(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                var level = player.serverLevel();
                int px = player.blockPosition().getX(), py = (int) Math.floor(player.getEyeY()),
                        pz = player.blockPosition().getZ() - 4;
                for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) {
                    boolean border = Math.abs(x) == 3 || Math.abs(y) == 3;
                    level.setBlockAndUpdate(new BlockPos(px+x, py+y, pz),
                            (border ? Blocks.POLISHED_DEEPSLATE : Blocks.GLASS).defaultBlockState());
                }
                level.setBlockAndUpdate(new BlockPos(px+1, py, pz), Blocks.SEA_LANTERN.defaultBlockState());
            });
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x(), delta.z()));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y(), Math.hypot(delta.x(), delta.z())));
        fixtureYaw = yaw;
        fixturePitch = pitch;
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        SpaceRenderState.setPoseProvider(new FixturePose(position, body,
                "@stellar".equals(view) || "@window".equals(view) ? "sys1" : null));
        PlanetRenderer.resetSceneState();
        SpaceRenderProfiler.resetSamples();
        stageStart = System.nanoTime();
    }

    private record FixturePose(UniversePosition universePosition, String currentBodyId, String hint)
            implements FreeFlightPoseProvider {
        public UniverseDelta universeVelocity() { return new UniverseDelta(0, 0, 0); }
        public double yaw() { return 0; }
        public double pitch() { return 0; }
        public double roll() { return 0; }
        public String currentSystemHint() {
            return hint != null ? hint : currentBodyId == null ? null : StarmapUniverse.systemIdOfEntry(currentBodyId);
        }
    }
}
