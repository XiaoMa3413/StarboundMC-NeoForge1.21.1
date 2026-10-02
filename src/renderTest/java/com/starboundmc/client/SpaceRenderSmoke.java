package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.FreeFlightPoseProvider;
import com.starboundmc.client.space.SpaceRenderState;
import com.starboundmc.client.space.BackgroundStarCatalog;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    private static final boolean BENCHMARK=Boolean.getBoolean("starboundmc.debug.spaceSmokeBenchmark");
    private static final double BENCH_SECONDS=30,WARM_SECONDS=5;
    private static final double[] FRAME_MS = new double[BENCHMARK?24000:180];
    private static boolean started, finished;
    private static long start, stageStart, previousFrame;
    private static int stage, samples;
    private static float fixtureYaw, fixturePitch;
    private static CompletableFuture<?> setup;
    private static CompletableFuture<Void> reload;
    private static long reloadGeneration;
    private static int discSequenceFrame;

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
            mc.options.renderDistance().set(BENCHMARK?12:4);
            if(BENCHMARK) {
                mc.options.enableVsync().set(false);
                mc.options.framerateLimit().set(260);
            }
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
        if ((System.nanoTime() - start) / 1e9 > (BENCHMARK?120+BODIES.length*(BENCH_SECONDS+WARM_SECONDS):240))
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
        if ("@reload".equals(BODIES[stage]) || "@spyglass-reload".equals(BODIES[stage])
                || "@query-reload".equals(BODIES[stage]) || "@lens-reload".equals(BODIES[stage])
                || "@disc-reload".equals(BODIES[stage])) {
            if (reload == null) { reloadGeneration=GpuSkyQuery.reloads();reload = mc.reloadResourcePacks(); return; }
            if (!reload.isDone()) return;
            reload.join();
            if(GpuSkyQuery.reloads()<=reloadGeneration)throw new IllegalStateException("Requested shader reload did not run");
        }
        if (stageStart == 0) selectView(mc);
        boolean discSequence="@disc-motion".equals(BODIES[stage]) || "@disc-animation".equals(BODIES[stage]);
        if("@disc-animation".equals(BODIES[stage]))
            GpuSkyQuery.discTimeOverride=180F+(float)((System.nanoTime()-stageStart)/1e9)*6F;
        // Initial server teleport packets can otherwise overwrite the first
        // camera pose after selectView, producing a mislabeled reference image.
        float movingYaw=fixtureYaw;
        if("@query-motion".equals(BODIES[stage]) || "@lens-motion".equals(BODIES[stage]) || "@disc-motion".equals(BODIES[stage]))
            movingYaw+=(float)Math.sin((System.nanoTime()-stageStart)/1e9*2)*.15F;
        mc.player.setYRot(movingYaw);
        mc.player.setXRot(fixturePitch);
        mc.player.yRotO = movingYaw;
        mc.player.xRotO = fixturePitch;
        long now = System.nanoTime();
        double stageSeconds=(now-stageStart)/1e9;
        if(discSequence && !BENCHMARK && stageSeconds>=2 && discSequenceFrame<12) {
            int frame=(int)((stageSeconds-2)/.2);
            if(frame>=discSequenceFrame) {
                var directory=mc.gameDirectory.toPath().resolve("screenshots")
                        .resolve(System.getProperty("starboundmc.debug.spaceSmokeLabel","capture"))
                        .resolve("@disc-animation".equals(BODIES[stage])?"disc-animation-sequence":"disc-motion-sequence");
                Files.createDirectories(directory);
                String name=String.format(java.util.Locale.ROOT,"%02d",discSequenceFrame);
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {image.writeToFile(directory.resolve(name+".png"));}
                Files.writeString(directory.resolve(name+".txt"),"stageSeconds="+stageSeconds+" cameraYaw="+mc.gameRenderer.getMainCamera().getYRot()
                        +" discTime="+GpuSkyQuery.lastDiscTime+"\n");discSequenceFrame++;
            }
        }
        if(!BENCHMARK || stageSeconds>=WARM_SECONDS) {
            if (previousFrame != 0 && samples < FRAME_MS.length)
                FRAME_MS[samples++] = (now - previousFrame) / 1e6;
            previousFrame=now;
        } else previousFrame=0;
        if (stageSeconds < (BENCHMARK?BENCH_SECONDS+WARM_SECONDS:discSequence?5:3) || samples == 0
                || !BENCHMARK && discSequence && discSequenceFrame<12) return;
        if(GpuSkyQuery.mode!=GpuSkyQuery.Mode.DISABLED && !GpuSkyQuery.active())
            throw new IllegalStateException("Research view silently fell back: "+GpuSkyQuery.diagnostics());
        if (BODIES[stage] != null && (BODIES[stage].startsWith("@spyglass-") || "@disc-spyglass".equals(BODIES[stage]))
                && !mc.player.isScoping()) throw new IllegalStateException("Spyglass fixture did not start using the real item");
        if (Boolean.getBoolean("starboundmc.debug.spaceSmokeStellarMatrix")) StellarShaderSmoke.verifyRestoration();
        var output = mc.gameDirectory.toPath().resolve("screenshots")
                .resolve(System.getProperty("starboundmc.debug.spaceSmokeLabel", "capture"));
        Files.createDirectories(output);
        String view = BODIES[stage] == null ? "deep-space" : BODIES[stage].replace(':', '-').replace("@", "");
        int occurrence=1;
        for(int i=0;i<stage;i++)if(java.util.Objects.equals(BODIES[i],BODIES[stage]))occurrence++;
        if(occurrence>1)view+="-"+occurrence;
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(output.resolve(view + ".png"));
        }
        double[] sorted = Arrays.copyOf(FRAME_MS, samples);
        Arrays.sort(sorted);
        Files.writeString(output.resolve(view + ".txt"),
                "Frame wall time includes the full client; this is not isolated GPU time.\n"
                + "samples=" + samples + " p50_ms=" + sorted[samples / 2]
                + " p95_ms=" + sorted[Math.min(samples - 1, (int) (samples * .95))] + "\n"
                + benchmarkReport(sorted,mc)
                + "resolution=" + mc.getMainRenderTarget().width + "x" + mc.getMainRenderTarget().height + "\n"
                + "cameraYaw=" + fixtureYaw + " cameraPitch=" + fixturePitch + "\n"
                + "renderCameraYaw="+mc.gameRenderer.getMainCamera().getYRot()+" renderCameraPitch="+mc.gameRenderer.getMainCamera().getXRot()+"\n"
                + "scoping=" + mc.player.isScoping() + " ordinaryFov=" + mc.options.fov().get() + "\n"
                + GpuSpaceBackground.diagnostics() + "\n" + SpaceSceneTarget.diagnostics() + "\n"
                + GpuSkyQuery.diagnostics() + "\n"
                + "quality=" + StarfieldClientConfig.SPACE_VISUAL_QUALITY.get() + " "
                + com.starboundmc.client.compat.stellarview.StellarViewStarfield.diagnostics() + "\n"
                + SpaceRenderProfiler.report());
        if (++stage == BODIES.length) {
            mc.options.keyUse.setDown(false);
            if (mc.player.isUsingItem()) mc.gameMode.releaseUsingItem(mc.player);
            Files.writeString(output.resolve("depth-checks.txt"), SpaceDepthSmoke.verify());
            Files.writeString(output.resolve("sky-query-checks.txt"), SkyQuerySmoke.verify());
            Files.writeString(output.resolve("disc-checks.txt"), AccretionDiscSmoke.verify());
            SpaceRenderState.resetPoseProvider();
            if (Boolean.getBoolean("starboundmc.debug.spaceSmokeStellarMatrix")) StellarShaderSmoke.restore();
            Files.writeString(output.resolve("complete.txt"), "Completed " + BODIES.length + " fixed space views.\n");
            finished = true;
            mc.stop();
        } else {
            reload = null;
            stageStart = previousFrame = 0;
            samples = 0;
        }
    }

    private static String benchmarkReport(double[] sorted,Minecraft mc) {
        if(!BENCHMARK)return "";
        double sum=Arrays.stream(sorted).sum(),p95=sorted[(int)(sorted.length*.95)],p99=sorted[(int)(sorted.length*.99)];
        long slow=Arrays.stream(sorted).filter(ms->ms>20).count();
        double fps=sorted.length*1000/sum;
        boolean passes=fps>=50 && p95<=20 && mc.getMainRenderTarget().width==1920 && mc.getMainRenderTarget().height==1080
                && StarfieldClientConfig.SPACE_VISUAL_QUALITY.get()==SpaceVisualQuality.ULTRA;
        return "fullClientBenchmark=true warmup_seconds="+WARM_SECONDS+" measured_seconds="+sum/1000
                +" average_fps="+fps+" p99_ms="+p99+" worst_ms="+sorted[sorted.length-1]+" frames_over_20ms="+slow
                +" over20_percent="+slow*100.0/sorted.length+" floor50_avg_and_p95="+passes
                +" renderDistance="+mc.options.renderDistance().get()+" vsync="+mc.options.enableVsync().get()
                +" framerateLimitSetting="+mc.options.framerateLimit().get()+"\n";
    }
    private static void selectView(Minecraft mc) {
        String view = BODIES[stage];
        GpuSkyQuery.mode=view!=null && view.startsWith("@disc-") ? GpuSkyQuery.Mode.DISC
                :view!=null && view.startsWith("@lens-") ? GpuSkyQuery.Mode.SCHWARZSCHILD
                : view!=null && view.startsWith("@query-") ? GpuSkyQuery.Mode.QUERY : GpuSkyQuery.Mode.DISABLED;
        GpuSkyQuery.discDiagnostic="@disc-diagnostic".equals(view);
        GpuSkyQuery.discPhysical="@disc-physical".equals(view);
        GpuSkyQuery.discFiniteBeam=!"@disc-single".equals(view) && !"@disc-volume-single".equals(view);
        GpuSkyQuery.discVolume=!"@disc-single".equals(view) && !"@disc-thin".equals(view);
        GpuSkyQuery.discDifferentials=view==null || !view.endsWith("-full-foot");
        GpuSkyQuery.discWideGlare=!"@disc-clean".equals(view);
        GpuSkyQuery.discInclination="@disc-face".equals(view)?0F:"@disc-inclined".equals(view)?60F:86.56F;
        GpuSkyQuery.discObserverRadius="@disc-near".equals(view)?12F:view!=null && view.startsWith("@disc-close")?6F:20F;
        if(view!=null && view.startsWith("@disc-volume-inside")) {
            GpuSkyQuery.discObserverRadius=6F;GpuSkyQuery.discInclination=89.5F;
        }
        GpuSkyQuery.discTimeOverride="@disc-time".equals(view)?400F:180F;
        if(GpuSkyQuery.mode==GpuSkyQuery.Mode.DISABLED) GpuSkyQuery.release();
        if("@query-performance".equals(view) || "@lens-performance".equals(view) || "@disc-performance".equals(view))
            StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(SpaceVisualQuality.PERFORMANCE);
        if("@disc-balanced".equals(view))StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(SpaceVisualQuality.BALANCED);
        if("@disc-high".equals(view))StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(SpaceVisualQuality.HIGH);
        if("@query-ultra".equals(view) || "@lens-ultra".equals(view) || "@disc-ultra".equals(view))
            StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(SpaceVisualQuality.ULTRA);
        mc.options.hideGui=!"@spyglass-overlay".equals(view);
        mc.options.keyUse.setDown(false);
        if (mc.player.isUsingItem()) mc.gameMode.releaseUsingItem(mc.player);
        if (view != null && (view.startsWith("@spyglass-") || view.equals("@disc-spyglass"))) {
            // Equip through the ordinary creative inventory packet and hold use;
            // GameRenderer then supplies the real interpolated spyglass projection.
            mc.player.getInventory().selected=0;
            var spyglass=new ItemStack(Items.SPYGLASS);
            mc.player.setItemInHand(InteractionHand.MAIN_HAND,spyglass);
            mc.gameMode.handleCreativeModeItemAdd(spyglass,36);
            mc.options.keyUse.setDown(true);
        }
        String body = view != null && !view.startsWith("@") ? view : null;
        UniversePosition position = body == null ? UniversePosition.of(20000, 3000, -10000)
                : UniverseNavigation.universeDock(body);
        var delta = body == null ? new UniverseDelta(0, 0, 1)
                : position.deltaTo(UniverseNavigation.universeBodyPosition(body));
        if ("@stellar".equals(view) || "@window".equals(view)) {
            var star = StarmapUniverse.systemOf("sys1:lush").stellarVisual().getUniversePosition();
            position = star.add(new UniverseDelta(0, 0, 1700));
            delta = position.deltaTo(star);
        } else if ("@star-wide".equals(view) || "@star-return".equals(view)
                || view != null && view.startsWith("@spyglass-")
                && !"@spyglass-galaxy".equals(view)) {
            var star=Arrays.stream(BackgroundStarCatalog.generate(2000))
                    .filter(s -> s.dustFraction() == 0 && Math.abs(s.x()*.24+s.y()*.87+s.z()*.43) > .2)
                    .max(java.util.Comparator.comparingDouble(BackgroundStarCatalog.Star::flux)).orElseThrow();
            delta=new UniverseDelta(star.x(),star.y(),star.z());
        } else if ("@galaxy".equals(view) || "@galaxy-return".equals(view) || "@spyglass-galaxy".equals(view)
                || GpuSkyQuery.mode!=GpuSkyQuery.Mode.DISABLED) delta = new UniverseDelta(.87, -.24, 0);
        else if ("@galaxy-outer".equals(view)) delta = new UniverseDelta(-.87,.24,0);
        else if ("@galaxy-side".equals(view)) {
            var direction = new org.joml.Vector3f(.24F,.87F,.43F)
                    .cross(new org.joml.Vector3f(.87F,-.24F,0)).normalize();
            delta = new UniverseDelta(direction.x,direction.y,direction.z);
        }
        else if (view != null && view.startsWith("@ring-")) {
            body="sys1:gasgiant";
            var sun=UniverseNavigation.sunDirection(body).normalize();
            var profile=StarmapUniverse.body(body).spaceVisual().orElseThrow();
            var n=RingRenderer.bodyOrientation(profile).transformDirection(new org.joml.Vector3f(0,1,0));
            var normal=new net.minecraft.world.phys.Vec3(n.x,n.y,n.z);
            var planar=sun.subtract(normal.scale(sun.dot(normal))).normalize();
            double side=sun.dot(normal) < 0 ? -1 : 1;
            var up=switch(view) {
                case "@ring-backlit" -> sun.scale(-1).subtract(normal.scale(side*.1)).normalize();
                case "@ring-edge" -> planar.add(normal.scale(side*.045)).normalize();
                case "@ring-shadow" -> sun.cross(normal).add(sun.scale(.35)).add(normal.scale(side*.55)).normalize();
                case "@ring-overlap" -> planar.add(normal.scale(side*.22)).normalize();
                default -> sun.add(normal.scale(side*.55)).normalize();
            };
            double distance=UniverseNavigation.radius(body)*4.3;
            position=UniverseNavigation.universeBodyPosition(body).add(new UniverseDelta(up.x*distance,up.y*distance,up.z*distance));
            delta=position.deltaTo(UniverseNavigation.universeBodyPosition(body));
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
        if("@query-pole".equals(view))delta=new UniverseDelta(.24,.87,.43);
        if("@query-edge".equals(view))delta=new UniverseDelta(1,1,1);
        if("@lens-offset".equals(view))delta=new UniverseDelta(.87,-.24,.20);
        if ("@resize".equals(view) || "@spyglass-resize".equals(view)
                || "@query-resize".equals(view) || "@lens-resize".equals(view) || "@disc-resize".equals(view))
            GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 960, 540);
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
        discSequenceFrame=0;
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
