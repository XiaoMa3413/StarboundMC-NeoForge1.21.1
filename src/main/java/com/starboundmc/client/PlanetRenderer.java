package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.CelestialLod;
import com.starboundmc.client.space.CelestialLodPolicy;
import com.starboundmc.client.space.CelestialLodTransitions;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.space.SpaceRenderState;
import com.starboundmc.client.space.GalaxyEnvironmentBlend;
import com.starboundmc.client.space.StarSystemResolver;
import com.starboundmc.client.space.StellarLod;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.ShipSpace;
import com.starboundmc.world.Planet;
import com.starboundmc.world.ShipDimensions;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import com.starboundmc.world.starmap.StellarVisualProfile;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Renders the ship dimension's space surroundings and the warp sequence.
 *
 * <p>The AFTER_SKY pose matrix contains camera rotation only (no translation),
 * and in that local frame <b>+Z points forward</b> from the camera. All sky
 * elements are drawn as fixed offsets in this frame, rotated by the ship's
 * turn "heading" during the warp.</p>
 *
 * <p>Warp sequence: the ship turns away from the planet first (the starfield
 * and the planet visibly sweep across the view), then the streak tunnel
 * engages, and on arrival the target planet reappears head-on from the front
 * while the starfield fades back in. The dimension uses SkyType.NONE, so this
 * renderer also owns the space dome and the starfield.</p>
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public class PlanetRenderer
{
    static final float PLANET_RADIUS = 50.0F;

    /** Longer velocity build-up prevents a visible kick when the tunnel starts. */
    private static final float MOTION_RAMP = 0.10F;
    /** Minimum streak length so the "dots" stay visible before stretching. */
    private static final double MIN_LENGTH = 0.04;
    /** Extra stretch past the cruise length during the intro, so the handoff
     *  into the motion phase feels punchier and the length settles smoothly. */
    private static final float STRETCH_OVERSHOOT = 1.2F;
    /** All tunnel geometry stays in front of the camera's near plane. */
    private static final double TUNNEL_NEAR_Z = 8.0;
    private static final double TUNNEL_FAR_Z = 260.0;

    // ---- Space starfield (drawn by us; the dimension uses SkyType.NONE) ----
    private static final int STAR_COUNT = 2000;
    private static final float STAR_DISTANCE = 200.0F;
    private static final float STAR_SIZE_SCALE = 0.85F;

    // ---- Streak tunnel ----
    private static final int STREAK_COUNT = 220;
    /** Close, bright streaks that provide speed parallax near the cockpit. */
    private static final int NEAR_STREAK_COUNT = 120;
    /** Thin, slow, dim background streaks drawn behind the main tunnel for depth. */
    private static final int TUNNEL_STAR_COUNT = 420;
    /** Fast, wide-angle particles that skim the cockpit edges and sell speed. */
    private static final int EDGE_STREAK_COUNT = 96;
    /** Full surround layer extending ahead, beside and behind the ship. */
    private static final int SURROUND_STREAK_COUNT = 260;

    private static final Map<Planet, Vector3f> ATMOSPHERE_COLORS = new EnumMap<>(Planet.class);
    private static final Map<Planet, Float> ATMOSPHERE_PEAK = new EnumMap<>(Planet.class);
    /** Fixed body orientation in virtual space; no orbital or axial animation yet. */
    private static final Map<Planet, Vector3f> BODY_ORIENTATION = new EnumMap<>(Planet.class);
    private static final Map<Planet, Vector3f> FIXED_SUN_DIRECTIONS = new EnumMap<>(Planet.class);
    private static final Map<Planet, Vector3f> STELLAR_CORONA_COLORS = new EnumMap<>(Planet.class);
    private static final Planet[] PLANET_DRAW_ORDER = Planet.values();
    private static final double[] PLANET_DISTANCE_SQ = new double[PLANET_DRAW_ORDER.length];
    /** Distance-driven body quality with a temporal blend to avoid popping. */
    private static final CelestialLodTransitions PLANET_LOD_TRANSITIONS =
            new CelestialLodTransitions(PLANET_DRAW_ORDER.length * 2);
    private static final int[] PLANET_POINT_COLORS = new int[PLANET_DRAW_ORDER.length];
    /** Resource locations are immutable and reused; distant bodies never touch the texture manager. */
    private static final Map<Planet, ResourceLocation> PLANET_TEXTURES = new EnumMap<>(Planet.class);
    private static final float PLANET_SKY_DISTANCE = 280.0F;
    private static final float MIN_PLANET_SKY_RADIUS = 0.12F;
    /** Surface geometry and fixed lighting are uploaded once, then transformed on the GPU. */
    private static final Map<Planet, VertexBuffer> PLANET_SURFACE_BUFFERS = new EnumMap<>(Planet.class);
    /** The overworld moon changes lighting only when its discrete moon phase changes. */
    private static VertexBuffer moonSurfaceBuffer;
    private static float moonSurfaceSunX = Float.NaN;
    private static float moonSurfaceSunY = Float.NaN;
    private static float moonSurfaceSunZ = Float.NaN;

    static
    {
        // Atmospheric glow per planet: tint and peak alpha of the additive halo.
        ATMOSPHERE_COLORS.put(Planet.LUSH, new Vector3f(0.30F, 0.60F, 1.0F));
        ATMOSPHERE_COLORS.put(Planet.MOLTEN, new Vector3f(1.0F, 0.45F, 0.20F));
        ATMOSPHERE_COLORS.put(Planet.FROZEN, new Vector3f(0.55F, 0.78F, 1.0F));
        ATMOSPHERE_COLORS.put(Planet.BARREN, new Vector3f(0.75F, 0.65F, 0.50F));

        ATMOSPHERE_PEAK.put(Planet.LUSH, 0.20F);
        ATMOSPHERE_PEAK.put(Planet.MOLTEN, 0.26F);
        ATMOSPHERE_PEAK.put(Planet.FROZEN, 0.23F);
        ATMOSPHERE_PEAK.put(Planet.BARREN, 0.15F);

        // x=axis tilt, y=fixed body yaw, z=fixed axial roll. These orient the
        // texture and poles in the common virtual-space frame but never animate.
        BODY_ORIENTATION.put(Planet.LUSH, new Vector3f(23.0F, 15.0F, 0.0F));
        BODY_ORIENTATION.put(Planet.MOLTEN, new Vector3f(6.0F, 210.0F, 0.0F));
        BODY_ORIENTATION.put(Planet.FROZEN, new Vector3f(32.0F, 125.0F, 0.0F));
        BODY_ORIENTATION.put(Planet.BARREN, new Vector3f(12.0F, 285.0F, 0.0F));

        for (Planet planet : Planet.values())
        {
            PLANET_TEXTURES.put(planet, planet.texture());
            Vec3 sun = ShipSpace.sunDirection(planet);
            FIXED_SUN_DIRECTIONS.put(planet, new Vector3f((float) sun.x, (float) sun.y, (float) sun.z));
            StellarVisualProfile profile = stellarProfile(planet);
            int color = profile.getCoronaColor();
            STELLAR_CORONA_COLORS.put(planet, new Vector3f(
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F));
            PLANET_POINT_COLORS[planet.ordinal()] = pointColor(planet);
        }
    }

    private static int pointColor(Planet planet)
    {
        return switch (planet)
        {
            case LUSH -> 0xFF68D68A;
            case MOLTEN -> 0xFFFF8A4C;
            case FROZEN -> 0xFF8FD7FF;
            case BARREN -> 0xFFD0B07A;
        };
    }

    // Arrival crossfade: the target planet grows and fades in over the last ~28%
    // of the warp while the ship swings back to face it. Package-visible so the
    // star map (ShipConsoleScreen) can sync its ship animation to the same timing.

    // ---- Deterministic starfield: directions on the unit sphere + size/brightness ----
    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final float[] STAR_Z = new float[STAR_COUNT];
    private static final float[] STAR_SIZE = new float[STAR_COUNT];
    private static final float[] STAR_BRIGHT = new float[STAR_COUNT];
    private static final float[] STAR_R = new float[STAR_COUNT];
    private static final float[] STAR_G = new float[STAR_COUNT];
    private static final float[] STAR_B = new float[STAR_COUNT];
    private static final float[] STAR_TWINKLE = new float[STAR_COUNT];

    // Static tunnel attributes keep each star's identity stable from frame to
    // frame. Only its phase advances during the flight, preventing random
    // re-seeding from turning into shimmer or a post-pause position jump.
    private static final float[] TUNNEL_THETA = new float[TUNNEL_STAR_COUNT];
    private static final float[] TUNNEL_RADIUS = new float[TUNNEL_STAR_COUNT];
    private static final float[] TUNNEL_PHASE = new float[TUNNEL_STAR_COUNT];
    private static final float[] TUNNEL_PERIOD = new float[TUNNEL_STAR_COUNT];
    private static final float[] TUNNEL_WIDTH = new float[TUNNEL_STAR_COUNT];
    private static final float[] STREAK_THETA = new float[STREAK_COUNT];
    private static final float[] STREAK_RADIUS = new float[STREAK_COUNT];
    private static final float[] STREAK_PHASE = new float[STREAK_COUNT];
    private static final float[] STREAK_PERIOD = new float[STREAK_COUNT];
    private static final float[] STREAK_WIDTH = new float[STREAK_COUNT];
    private static final float[] NEAR_THETA = new float[NEAR_STREAK_COUNT];
    private static final float[] NEAR_RADIUS = new float[NEAR_STREAK_COUNT];
    private static final float[] NEAR_PHASE = new float[NEAR_STREAK_COUNT];
    private static final float[] NEAR_PERIOD = new float[NEAR_STREAK_COUNT];
    private static final float[] NEAR_WIDTH = new float[NEAR_STREAK_COUNT];
    private static final float[] EDGE_THETA = new float[EDGE_STREAK_COUNT];
    private static final float[] EDGE_RADIUS = new float[EDGE_STREAK_COUNT];
    private static final float[] EDGE_PHASE = new float[EDGE_STREAK_COUNT];
    private static final float[] EDGE_PERIOD = new float[EDGE_STREAK_COUNT];
    private static final float[] EDGE_WIDTH = new float[EDGE_STREAK_COUNT];
    private static final float[] SURROUND_THETA = new float[SURROUND_STREAK_COUNT];
    private static final float[] SURROUND_RADIUS = new float[SURROUND_STREAK_COUNT];
    private static final float[] SURROUND_PHASE = new float[SURROUND_STREAK_COUNT];
    private static final float[] SURROUND_PERIOD = new float[SURROUND_STREAK_COUNT];
    private static final float[] SURROUND_WIDTH = new float[SURROUND_STREAK_COUNT];

    static
    {
        Random random = new Random(42424242L);
        for (int i = 0; i < STAR_COUNT; i++)
        {
            // Uniform direction on the sphere.
            double z = 1.0 - 2.0 * random.nextDouble();
            double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));
            double phi = random.nextDouble() * Math.PI * 2.0;
            STAR_X[i] = (float) (r * Math.cos(phi));
            STAR_Y[i] = (float) (r * Math.sin(phi));
            STAR_Z[i] = (float) z;

            // Sizes: mostly 1-2 px, a few bright 3-4 px stars.
            double sizeRoll = random.nextDouble();
            if (sizeRoll < 0.70)
                STAR_SIZE[i] = 0.10F + (float) random.nextDouble() * 0.10F;
            else if (sizeRoll < 0.95)
                STAR_SIZE[i] = 0.20F + (float) random.nextDouble() * 0.20F;
            else
                STAR_SIZE[i] = 0.40F + (float) random.nextDouble() * 0.15F;
            STAR_SIZE[i] *= STAR_SIZE_SCALE;

            STAR_BRIGHT[i] = 0.55F + (float) random.nextDouble() * 0.45F;

            double tint = random.nextDouble();
            if (tint < 0.08)
            {
                // rare orange-red giants
                STAR_R[i] = 1.00F; STAR_G[i] = 0.62F; STAR_B[i] = 0.45F;
            }
            else if (tint < 0.20)
            {
                // warm yellow-white
                STAR_R[i] = 1.00F; STAR_G[i] = 0.88F; STAR_B[i] = 0.68F;
            }
            else if (tint < 0.42)
            {
                // blue-white
                STAR_R[i] = 0.72F; STAR_G[i] = 0.83F; STAR_B[i] = 1.00F;
            }
            else
            {
                // white
                STAR_R[i] = 0.93F; STAR_G[i] = 0.96F; STAR_B[i] = 1.00F;
            }

            STAR_TWINKLE[i] = (float) (random.nextDouble() * Math.PI * 2.0);
        }

        Random far = new Random(987654321L);
        for (int i = 0; i < TUNNEL_STAR_COUNT; i++)
        {
            TUNNEL_THETA[i] = (float) (far.nextDouble() * Math.PI * 2.0);
            // Keep the centre readable while filling the full field of view.
            TUNNEL_RADIUS[i] = (float) (5.0 + Math.sqrt(far.nextDouble()) * 58.0);
            TUNNEL_PHASE[i] = (float) (far.nextDouble() * 120.0);
            TUNNEL_PERIOD[i] = (float) (64.0 + far.nextDouble() * 72.0);
            TUNNEL_WIDTH[i] = (float) (0.05 + far.nextDouble() * 0.16);
        }

        Random mid = new Random(1234567L);
        for (int i = 0; i < STREAK_COUNT; i++)
        {
            STREAK_THETA[i] = (float) (mid.nextDouble() * Math.PI * 2.0);
            STREAK_RADIUS[i] = (float) (4.0 + Math.sqrt(mid.nextDouble()) * 46.0);
            STREAK_PHASE[i] = (float) (mid.nextDouble() * 96.0);
            STREAK_PERIOD[i] = (float) (44.0 + mid.nextDouble() * 52.0);
            STREAK_WIDTH[i] = (float) (0.20 + mid.nextDouble() * 0.70);
        }

        Random near = new Random(7654321L);
        for (int i = 0; i < NEAR_STREAK_COUNT; i++)
        {
            NEAR_THETA[i] = (float) (near.nextDouble() * Math.PI * 2.0);
            NEAR_RADIUS[i] = (float) (7.0 + Math.sqrt(near.nextDouble()) * 30.0);
            NEAR_PHASE[i] = (float) (near.nextDouble() * 72.0);
            NEAR_PERIOD[i] = (float) (30.0 + near.nextDouble() * 34.0);
            NEAR_WIDTH[i] = (float) (0.28 + near.nextDouble() * 0.78);
        }

        Random edge = new Random(246813579L);
        for (int i = 0; i < EDGE_STREAK_COUNT; i++)
        {
            EDGE_THETA[i] = (float) (edge.nextDouble() * Math.PI * 2.0);
            EDGE_RADIUS[i] = (float) (22.0 + Math.sqrt(edge.nextDouble()) * 54.0);
            EDGE_PHASE[i] = (float) (edge.nextDouble() * 48.0);
            EDGE_PERIOD[i] = (float) (16.0 + edge.nextDouble() * 18.0);
            EDGE_WIDTH[i] = (float) (0.18 + edge.nextDouble() * 0.48);
        }

        Random surround = new Random(135792468L);
        for (int i = 0; i < SURROUND_STREAK_COUNT; i++)
        {
            SURROUND_THETA[i] = (float) (surround.nextDouble() * Math.PI * 2.0);
            // A hollow shell leaves the cockpit readable while surrounding it
            // with motion that remains visible through side and rear windows.
            SURROUND_RADIUS[i] = (float) (28.0 + Math.sqrt(surround.nextDouble()) * 68.0);
            SURROUND_PHASE[i] = (float) (surround.nextDouble() * 110.0);
            SURROUND_PERIOD[i] = (float) (76.0 + surround.nextDouble() * 54.0);
            SURROUND_WIDTH[i] = (float) (0.12 + surround.nextDouble() * 0.34);
        }
    }

    // Cached UV-sphere geometry - 32×64 for 4k/8k textures, silky round (was 16×32, faceting visible at 38°)
    private static final int SPHERE_STACKS = 32;
    private static final int SPHERE_SLICES = 64;
    static final float[] SPHERE_X;
    static final float[] SPHERE_Y;
    static final float[] SPHERE_Z;
    static final float[] SPHERE_U;
    static final float[] SPHERE_V;

    // The additive atmosphere is deliberately lower-detail than the textured
    // surface: its soft alpha gradient hides the extra facets, while this cuts
    // its per-frame CPU work to one quarter of the surface mesh.
    private static final int HALO_STACKS = 16;
    private static final int HALO_SLICES = 32;
    private static final float[] HALO_X;
    private static final float[] HALO_Y;
    private static final float[] HALO_Z;

    static
    {
        int vertices = SPHERE_STACKS * SPHERE_SLICES * 4;
        SPHERE_X = new float[vertices];
        SPHERE_Y = new float[vertices];
        SPHERE_Z = new float[vertices];
        SPHERE_U = new float[vertices];
        SPHERE_V = new float[vertices];

        int idx = 0;
        for (int i = 0; i < SPHERE_STACKS; i++)
        {
            float phi0 = (float) (Math.PI * i / SPHERE_STACKS);
            float phi1 = (float) (Math.PI * (i + 1) / SPHERE_STACKS);
            float v0 = (float) i / SPHERE_STACKS;
            float v1 = (float) (i + 1) / SPHERE_STACKS;
            for (int j = 0; j < SPHERE_SLICES; j++)
            {
                float theta0 = (float) (2.0 * Math.PI * j / SPHERE_SLICES);
                float theta1 = (float) (2.0 * Math.PI * (j + 1) / SPHERE_SLICES);
                float u0 = (float) j / SPHERE_SLICES;
                float u1 = (float) (j + 1) / SPHERE_SLICES;

                putSphereVertex(idx++, phi0, theta0, u0, v0);
                putSphereVertex(idx++, phi0, theta1, u1, v0);
                putSphereVertex(idx++, phi1, theta1, u1, v1);
                putSphereVertex(idx++, phi1, theta0, u0, v1);
            }
        }

        int haloVertices = HALO_STACKS * HALO_SLICES * 4;
        HALO_X = new float[haloVertices];
        HALO_Y = new float[haloVertices];
        HALO_Z = new float[haloVertices];
        idx = 0;
        for (int i = 0; i < HALO_STACKS; i++)
        {
            float phi0 = (float) (Math.PI * i / HALO_STACKS);
            float phi1 = (float) (Math.PI * (i + 1) / HALO_STACKS);
            for (int j = 0; j < HALO_SLICES; j++)
            {
                float theta0 = (float) (2.0 * Math.PI * j / HALO_SLICES);
                float theta1 = (float) (2.0 * Math.PI * (j + 1) / HALO_SLICES);
                putHaloVertex(idx++, phi0, theta0);
                putHaloVertex(idx++, phi0, theta1);
                putHaloVertex(idx++, phi1, theta1);
                putHaloVertex(idx++, phi1, theta0);
            }
        }
    }

    private static void putSphereVertex(int idx, float phi, float theta, float u, float v)
    {
        float sinPhi = (float) Math.sin(phi);
        SPHERE_X[idx] = PLANET_RADIUS * sinPhi * (float) Math.cos(theta);
        SPHERE_Y[idx] = PLANET_RADIUS * (float) Math.cos(phi);
        SPHERE_Z[idx] = PLANET_RADIUS * sinPhi * (float) Math.sin(theta);
        SPHERE_U[idx] = u;
        SPHERE_V[idx] = v;
    }

    private static void putHaloVertex(int idx, float phi, float theta)
    {
        float sinPhi = (float) Math.sin(phi);
        HALO_X[idx] = PLANET_RADIUS * sinPhi * (float) Math.cos(theta);
        HALO_Y[idx] = PLANET_RADIUS * (float) Math.cos(phi);
        HALO_Z[idx] = PLANET_RADIUS * sinPhi * (float) Math.sin(theta);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        if (!mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL))
            return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        SpaceRenderContext space = SpaceRenderState.capture(mc.level.getGameTime() + partialTick);
        // AFTER_SKY already carries the camera rotation. Keep the space scene
        // anchored to the ship rather than subtracting the player eye position:
        // walking around the bridge must not drag the distant planet across view.
        double yaw = space.yaw();
        double pitch = space.pitch();
        FlightPhase phase = space.flightPhase();

        // Rebuild the camera rotation without GameRenderer's walk/view bobbing.
        // The event stack contains bobbing when that option is enabled, which
        // incorrectly makes astronomical bodies shake relative to the window.
        PoseStack skyPose = stableCameraPose(event.getCamera());
        // Visual-only bank: rotate the outside universe, never the player entity/camera.
        skyPose.mulPose(Axis.ZP.rotationDegrees((float) -space.roll()));
        renderSpaceDome(skyPose);
        float starAlpha = 1.0F;
        float starConvergence = 0.0F;
        if (space.warping() && space.warpDurationTicks() > ShipFlightController.SHORT_ROUTE_TICKS)
        {
            float warpProgress = space.warpProgress();
            int duration = Math.max(1, space.warpDurationTicks());
            float accelStart = ShipFlightController.TURN_TICKS / (float) duration;
            float hyperspaceStart = (ShipFlightController.TURN_TICKS + ShipFlightController.ACCEL_TICKS)
                    / (float) duration;
            float enter = smoothstep((warpProgress - 0.16F) / 0.18F);
            float exit = smoothstep((warpProgress - 0.68F) / 0.24F);
            float convergenceIn = smoothstep((warpProgress - accelStart)
                    / Math.max(0.0001F, hyperspaceStart - accelStart));
            starConvergence = convergenceIn * (1.0F - exit);
            // During the jump the moving tunnel is the environment. Leaving a
            // bright static shell behind makes the ship feel stationary.
            starAlpha = 1.0F - 0.94F * enter * (1.0F - exit);
        }
        StarSystemResolver.ResolvedStarField stars = StarSystemResolver.resolve(space);
        GalaxyEnvironmentBlend environment = stars.environment();
        renderStarField(skyPose, (float) -yaw, (float) -pitch, starAlpha, starConvergence,
                environment.skyTintColor(), environment.skyTintAmount());

        // Stars live on a projection-safe shell and are drawn before planets,
        // allowing a planet disc to pass cleanly in front of its system's star.
        renderSystemStars(skyPose, space, stars);

        // Coordinate visibility replaces the old current/target special cases.
        // It works unchanged when a manual controller supplies an arbitrary pose.
        renderVisiblePlanets(skyPose, event.getCamera(), space, stars);

        // Pre-stretch belongs only to routes that will actually enter
        // HYPERSPACE. Short sublight routes share ACCELERATE but transition
        // into CRUISE, so they must never render warp streaks.
        boolean hyperspaceRoute = space.warpDurationTicks() > ShipFlightController.SHORT_ROUTE_TICKS;
        if (hyperspaceRoute
                && (phase == FlightPhase.HYPERSPACE || phase == FlightPhase.DECELERATE
                    || phase == FlightPhase.ACCELERATE))
            renderWarpStreaks(skyPose, event.getCamera(), partialTick, space);
    }

    private static void renderVisiblePlanets(PoseStack pose, Camera camera, SpaceRenderContext space,
                                              StarSystemResolver.ResolvedStarField stars)
    {
        boolean longRoute = space.warpDurationTicks() > ShipFlightController.SHORT_ROUTE_TICKS;
        float warpProgress = space.warpProgress();
        UniversePosition ship = space.universePosition();
        StarSystem sourceSystem = space.currentBody() == null
                ? null : StarSystems.systemOfPlanet(space.currentBody());
        StarSystem targetSystem = space.targetBody() == null
                ? null : StarSystems.systemOfPlanet(space.targetBody());
        for (int i = 0; i < PLANET_DRAW_ORDER.length; i++)
            PLANET_DISTANCE_SQ[i] = ShipSpace.universeBodyPosition(PLANET_DRAW_ORDER[i]).distanceToSqr(ship);

        // Reusable insertion sort; four planets make this cheaper than building
        // and sorting a per-frame celestial collection.
        for (int i = 1; i < PLANET_DRAW_ORDER.length; i++)
        {
            Planet body = PLANET_DRAW_ORDER[i];
            double distance = PLANET_DISTANCE_SQ[i];
            int j = i;
            while (j > 0 && PLANET_DISTANCE_SQ[j - 1] < distance)
            {
                PLANET_DRAW_ORDER[j] = PLANET_DRAW_ORDER[j - 1];
                PLANET_DISTANCE_SQ[j] = PLANET_DISTANCE_SQ[j - 1];
                j--;
            }
            PLANET_DRAW_ORDER[j] = body;
            PLANET_DISTANCE_SQ[j] = distance;
        }

        for (Planet body : PLANET_DRAW_ORDER)
        {
            StarSystem system = StarSystems.systemOfPlanet(body);
            float systemVisibility = stellarVisibility(stars, system);
            boolean departingSystemBody = space.warping() && longRoute
                    && warpProgress < WarpVisualTiming.ARRIVAL_FADE_START
                    && sourceSystem != null && system == sourceSystem;
            boolean arrivingSystemBody = space.warping() && longRoute
                    && warpProgress >= WarpVisualTiming.ARRIVAL_FADE_START
                    && targetSystem != null && system == targetSystem;
            // Keep the entire source system during the departure leg. This
            // preserves the primary/companion relationship (for example the
            // lush world and its molten moon) instead of dropping every body
            // except the one the ship is docked at.
            boolean routePriority = departingSystemBody || arrivingSystemBody;
            if (longRoute && space.warping() && !routePriority)
            {
                updatePlanetLod(body, CelestialLod.CULLED, space.animationTicks());
                continue;
            }

            Vec3 bodyCenter = virtualToView(ShipSpace.universeBodyPosition(body), space);
            double distance = bodyCenter.length();
            double angularDiameter = CelestialLodPolicy.angularDiameterDegrees(
                    ShipSpace.radius(body), distance);
            CelestialLod previous = PLANET_LOD_TRANSITIONS.currentLod(body.getId());
            CelestialLod requested = CelestialLodPolicy.hysteretic(
                    angularDiameter, previous, routePriority ? CelestialLod.POINT : CelestialLod.CULLED);
            if (systemVisibility <= 0.20F && !routePriority)
                requested = CelestialLod.CULLED;
            float detail = updatePlanetLod(body, requested, space.animationTicks());

            // The hand-off still switches at the shared arrival boundary, but
            // the body itself is no longer faded in. Distance LOD blending is
            // the only visual transition, so an approaching planet cannot
            // appear, disappear, and then restart a second fade.
            if (detail > 0.001F)
                renderVirtualPlanet(pose, camera, body, space, 1.0F, detail);
        }
    }

    private static float updatePlanetLod(Planet body, CelestialLod requested, float animationTicks)
    {
        return PLANET_LOD_TRANSITIONS.update(body.getId(), requested, animationTicks);
    }
    private static float stellarVisibility(StarSystemResolver.ResolvedStarField stars, StarSystem system)
    {
        if (system == null)
            return 0.0F;
        for (int i = 0; i < stars.count(); i++)
        {
            StarSystemResolver.VisibleStar star = stars.star(i);
            if (star.system() == system)
                return star.alpha();
        }
        return 0.0F;
    }

    /** Draw one body at true near distance or angularly projected on the sky shell. */
    private static void renderVirtualPlanet(PoseStack pose, Camera camera, Planet body,
                                            SpaceRenderContext space, float alpha, float lodDetail)
    {
        Vec3 bodyCenter = virtualToView(ShipSpace.universeBodyPosition(body), space);
        float bodyScale = (float) (ShipSpace.radius(body) / PLANET_RADIUS);
        double distance = bodyCenter.length();
        if (distance > PLANET_SKY_DISTANCE)
        {
            float projectionScale = (float) (PLANET_SKY_DISTANCE / distance);
            bodyCenter = bodyCenter.scale(projectionScale);
            bodyScale *= projectionScale;
        }
        float renderedRadius = PLANET_RADIUS * bodyScale;
        float pointWeight = CelestialLodTransitions.pointWeight(lodDetail);
        float reducedWeight = CelestialLodTransitions.reducedWeight(lodDetail);
        float fullWeight = CelestialLodTransitions.fullWeight(lodDetail);
        float bodyWeight = pointWeight + reducedWeight + fullWeight;
        if (renderedRadius < MIN_PLANET_SKY_RADIUS)
        {
            // A body can cross the point -> reduced boundary while it is still
            // smaller than the minimum sphere radius. Keep one marker alive
            // during that transition instead of fading the point out before a
            // reduced sphere can become visible.
            if (bodyWeight > 0.002F)
                renderPlanetPoint(pose, body, alpha * bodyWeight, (float) bodyCenter.x,
                        (float) bodyCenter.y, (float) bodyCenter.z, renderedRadius);
            return;
        }
        float cx = (float) bodyCenter.x;
        float cy = (float) bodyCenter.y;
        float cz = (float) bodyCenter.z;
        if (reducedWeight + fullWeight > 0.002F)
            renderPlanet(pose, camera, body, bodyScale, alpha * (reducedWeight + fullWeight),
                    cx, cy, cz, (float) space.yaw(), (float) space.pitch());
        if (fullWeight > 0.002F && renderedRadius >= 0.45F)
            renderAtmosphereGlow(pose, body, bodyScale, alpha * fullWeight,
                    cx, cy, cz);
        if (pointWeight > 0.002F)
            renderPlanetPoint(pose, body, alpha * pointWeight, cx, cy, cz, renderedRadius);
    }

    /** Camera rotation only: deliberately excludes walk/view bobbing. */
    private static PoseStack stableCameraPose(Camera camera)
    {
        PoseStack pose = new PoseStack();
        pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        pose.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        return pose;
    }

    /** Transform a universe coordinate into the rotation-only sky frame. */
    private static Vec3 virtualToView(UniversePosition universePoint, SpaceRenderContext space)
    {
        Vec3 relative = space.universePosition().deltaTo(universePoint).toVec3();
        relative = ShipSpace.rotateYaw(relative, -space.yaw());
        return ShipSpace.rotatePitch(relative, -space.pitch());
    }

    private static StellarVisualProfile stellarProfile(Planet planet)
    {
        StarSystem system = StarSystems.systemOfPlanet(planet);
        return system == null ? null : system.getStellarVisual();
    }

    private static void renderSystemStars(PoseStack pose, SpaceRenderContext space,
                                          StarSystemResolver.ResolvedStarField stars)
    {
        double yawRadians = Math.toRadians(-space.yaw());
        double yawCos = Math.cos(yawRadians);
        double yawSin = Math.sin(yawRadians);
        double pitchRadians = Math.toRadians(-space.pitch());
        double pitchCos = Math.cos(pitchRadians);
        double pitchSin = Math.sin(pitchRadians);

        // LOD-2 points are submitted first in one additive batch. Nearer
        // simplified/full discs render afterwards and can cover aligned points.
        StellarPointBatchRenderer.render(pose, stars, yawCos, yawSin,
                pitchCos, pitchSin);

        for (int i = 0; i < stars.count(); i++)
        {
            StarSystemResolver.VisibleStar star = stars.star(i);
            float simplifiedWeight = star.simplifiedLodWeight();
            float fullWeight = star.fullLodWeight();
            if (simplifiedWeight <= 0.002F && fullWeight <= 0.002F)
                continue;
            double viewX = star.relativeX() * yawCos + star.relativeZ() * yawSin;
            double yawZ = -star.relativeX() * yawSin + star.relativeZ() * yawCos;
            double viewY = star.relativeY() * pitchCos - yawZ * pitchSin;
            double viewZ = star.relativeY() * pitchSin + yawZ * pitchCos;
            StellarVisualProfile profile = star.system().getStellarVisual();
            float apparentScale = star.projectedRadius() / profile.getApparentRadius();
            if (simplifiedWeight > 0.002F)
                StellarRenderer.render(pose, profile, viewX, viewY, viewZ,
                        StellarRenderer.SHIP_SKY_DISTANCE, star.stellarBrightness() * simplifiedWeight,
                        space.animationTicks(), apparentScale, star.coronaDetail(),
                        star.effectDetail(), StellarLod.SIMPLIFIED);
            if (fullWeight > 0.002F)
                StellarRenderer.render(pose, profile, viewX, viewY, viewZ,
                        StellarRenderer.SHIP_SKY_DISTANCE, star.stellarBrightness() * fullWeight,
                        space.animationTicks(), apparentScale, star.coronaDetail(),
                        star.effectDetail(), StellarLod.FULL);
        }
    }

    /**
     * Very dark space dome with a subtle blue gradient. The ship dimension uses
     * SkyType.NONE, so without this the sky behind the starfield is just the
     * clear color; the dome guarantees a proper deep-space backdrop.
     */
    private static void renderSpaceDome(PoseStack pose)
    {
        Matrix4f matrix = pose.last().pose();
        FogRenderer.setupNoFog();
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 430 keeps the cube corners (430*√3 ≈ 745) inside the default far plane
        // (render distance 12 → 768); a larger dome gets clipped at the corners.
        float s = 430.0F;
        addDomeFace(bb, matrix, -s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s);
        addDomeFace(bb, matrix, -s, -s, s, s, -s, s, s, s, s, -s, s, s);
        addDomeFace(bb, matrix, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s);
        addDomeFace(bb, matrix, s, -s, -s, s, -s, s, s, s, s, s, s, -s);
        addDomeFace(bb, matrix, -s, s, -s, s, s, -s, s, s, s, -s, s, s);
        addDomeFace(bb, matrix, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s);

        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void addDomeFace(BufferBuilder bb, Matrix4f matrix,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float x4, float y4, float z4)
    {
        vertexColor(bb, matrix, x1, y1, z1, domeColor(y1));
        vertexColor(bb, matrix, x2, y2, z2, domeColor(y2));
        vertexColor(bb, matrix, x3, y3, z3, domeColor(y3));
        vertexColor(bb, matrix, x4, y4, z4, domeColor(y4));
    }

    /** Slightly blue at the top, near-black at the bottom. */
    private static float[] domeColor(float y)
    {
        float t = Math.max(0.0F, Math.min(1.0F, (y + 430.0F) / 860.0F));
        return new float[] { lerp(0.010F, 0.028F, t), lerp(0.014F, 0.038F, t), lerp(0.035F, 0.095F, t), 1.0F };
    }

    /**
     * Dense additive starfield on a shell at STAR_DISTANCE. The whole shell is
     * rotated by the ship heading, so during the turn the stars sweep across the
     * view exactly like the planet does.
     */
    private static void renderStarField(PoseStack pose, float yawDeg, float pitchDeg,
                                        float alpha, float convergence,
                                        int tintColor, float tintAmount)
    {
        if (alpha <= 0.01F)
            return;

        Matrix4f matrix = pose.last().pose();
        long now = System.currentTimeMillis();
        float cy = (float) Math.cos(Math.toRadians(yawDeg));
        float sy = (float) Math.sin(Math.toRadians(yawDeg));
        float cp = (float) Math.cos(Math.toRadians(pitchDeg));
        float sp = (float) Math.sin(Math.toRadians(pitchDeg));
        float tintR = ((tintColor >> 16) & 0xFF) / 255.0F;
        float tintG = ((tintColor >> 8) & 0xFF) / 255.0F;
        float tintB = (tintColor & 0xFF) / 255.0F;

        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < STAR_COUNT; i++)
        {
            // Yaw around Y, then pitch around X (ship heading).
            float x1 = STAR_X[i] * cy + STAR_Z[i] * sy;
            float z1 = -STAR_X[i] * sy + STAR_Z[i] * cy;
            float y2 = STAR_Y[i] * cp - z1 * sp;
            float z2 = STAR_Y[i] * sp + z1 * cp;

            // Hyperspace entrance: stars in the forward hemisphere collapse
            // toward the flight axis before the tunnel takes over. Renormalize
            // after squeezing so the star shell stays at a stable distance.
            float frontWeight = smoothstep((z2 + 0.05F) / 0.95F);
            float squeeze = 1.0F - 0.82F * convergence * frontWeight;
            x1 *= squeeze;
            y2 *= squeeze;
            float dirLength = (float) Math.sqrt(x1 * x1 + y2 * y2 + z2 * z2);
            if (dirLength > 0.0001F)
            {
                x1 /= dirLength;
                y2 /= dirLength;
                z2 /= dirLength;
            }

            float px = x1 * STAR_DISTANCE;
            float py = y2 * STAR_DISTANCE;
            float pz = z2 * STAR_DISTANCE;

            // Billboard basis perpendicular to the star's own direction (the
            // same trick vanilla stars use): the quad faces the camera from
            // every direction and shrinks gracefully at grazing angles instead
            // of blowing up, so no per-star frustum culling is needed and the
            // whole sky stays populated.
            float bx, bz;
            if (Math.abs(y2) > 0.99F)
            {
                bx = 1.0F;
                bz = 0.0F;
            }
            else
            {
                float inv = 1.0F / (float) Math.sqrt(z2 * z2 + x1 * x1);
                bx = -z2 * inv;
                bz = x1 * inv;
            }
            float ux = -bz * y2;
            float uy = bz * x1 - bx * z2;
            float uz = bx * y2;

            float s = STAR_SIZE[i] * (1.0F + convergence * frontWeight * 0.65F);
            float twinkle = 0.85F + 0.15F * (float) Math.sin(now * 0.003 + STAR_TWINKLE[i]);
            float focusBrightness = 1.0F + convergence * frontWeight * 1.15F;
            float a = Math.min(1.0F, STAR_BRIGHT[i] * alpha * twinkle * focusBrightness);

            float r = lerp(STAR_R[i], tintR, tintAmount);
            float g = lerp(STAR_G[i], tintG, tintAmount);
            float b = lerp(STAR_B[i], tintB, tintAmount);
            vertexColor(bb, matrix, px + (bx + ux) * s, py + uy * s, pz + (bz + uz) * s, r, g, b, a);
            vertexColor(bb, matrix, px + (ux - bx) * s, py + uy * s, pz + (uz - bz) * s, r, g, b, a);
            vertexColor(bb, matrix, px - (bx + ux) * s, py - uy * s, pz - (bz + uz) * s, r, g, b, a);
            vertexColor(bb, matrix, px + (bx - ux) * s, py - uy * s, pz + (bz - uz) * s, r, g, b, a);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Additive limb glow drawn as a single sphere shell around the planet. Alpha
     * is computed from the angular distance to the planet's projected limb: it is
     * brightest at the limb and fades to zero at the outer edge of the shell, so
     * the halo always hugs the planet and never has a bright outer rim.
     */
    private static void renderAtmosphereGlow(PoseStack pose, Planet planet, float scale, float alpha,
                                             float cx, float cy, float cz)
    {
        Matrix4f matrix = pose.last().pose();
        Vector3f color = ATMOSPHERE_COLORS.get(planet);
        float peak = ATMOSPHERE_PEAK.get(planet);

        float distC = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        float axisX = cx / distC;
        float axisY = cy / distC;
        float axisZ = cz / distC;

        float planetRadius = PLANET_RADIUS * scale;
        float outerFactor = 1.15F;
        float outerRadius = planetRadius * outerFactor;
        float limbAngle = (float) Math.asin(Math.min(1.0, planetRadius / distC));
        float outerAngle = (float) Math.asin(Math.min(1.0, outerRadius / distC));
        float angleRange = Math.max(0.0001F, outerAngle - limbAngle);
        // Start the glow slightly inside the planet's projected limb so the halo
        // visibly touches the surface instead of leaving a dark gap at the edge.
        float innerAngle = Math.max(0.0F, limbAngle - 0.05F);
        float innerRange = Math.max(0.0001F, limbAngle - innerAngle);

        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < HALO_X.length; i++)
        {
            float wx = cx + HALO_X[i] * outerFactor * scale;
            float wy = cy + HALO_Y[i] * outerFactor * scale;
            float wz = cz + HALO_Z[i] * outerFactor * scale;

            float len = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
            float vx = wx / len;
            float vy = wy / len;
            float vz = wz / len;
            float dot = axisX * vx + axisY * vy + axisZ * vz;
            dot = Math.max(-1.0F, Math.min(1.0F, dot));
            float angle = (float) Math.acos(dot);

            float a = 0.0F;
            if (angle >= innerAngle && angle <= limbAngle)
            {
                // Ramp up from the inner edge to the limb so the glow overlaps
                // the planet's rim and appears glued to the surface.
                float t = (angle - innerAngle) / innerRange;
                a = peak * smoothstep(t) * alpha;
            }
            else if (angle > limbAngle && angle <= outerAngle)
            {
                float t = (angle - limbAngle) / angleRange;
                float fade = (float) Math.pow(1.0F - t, 1.5);
                a = peak * fade * alpha;
            }
            vertexColor(bb, matrix, wx, wy, wz, color.x, color.y, color.z, a);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static Vector3f fixedSunDirection(Planet planet)
    {
        return FIXED_SUN_DIRECTIONS.get(planet);
    }

    private static float smoothstep(float t)
    {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }

    /**
     * Small bodies remain readable without keeping a textured sphere alive.
     * The eight-sided marker is intentionally softer than a square billboard,
     * so distant planets do not look like stray pixels in the starfield.
     */
    private static void renderPlanetPoint(PoseStack pose, Planet planet, float alpha,
                                          float cx, float cy, float cz, float projectedRadius)
    {
        if (alpha <= 0.002F)
            return;
        float size = Math.max(0.30F, Math.min(1.10F, projectedRadius * 0.85F));
        int color = PLANET_POINT_COLORS[planet.ordinal()];
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = pose.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 8; i++)
        {
            double a0 = Math.PI * 2.0 * i / 8.0;
            double a1 = Math.PI * 2.0 * (i + 1) / 8.0;
            vertexColor(bb, matrix, cx, cy, cz, r, g, b, alpha);
            vertexColor(bb, matrix,
                    cx + (float) Math.cos(a0) * size,
                    cy + (float) Math.sin(a0) * size, cz,
                    r, g, b, alpha * 0.72F);
            vertexColor(bb, matrix,
                    cx + (float) Math.cos(a1) * size,
                    cy + (float) Math.sin(a1) * size, cz,
                    r, g, b, alpha * 0.72F);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void renderPlanet(PoseStack pose, Camera cam, Planet planet, float scale, float alpha,
                                     float cx, float cy, float cz, float shipYaw, float shipPitch)
    {
        // Skybox-style: the planet is drawn at a fixed offset in the rotation-only
        // AFTER_SKY frame, so it stays visible through the bridge window at all times.
        drawOrientedPlanetSphere(pose, pose.last().pose(), planet, cx, cy, cz, scale,
                fixedSunDirection(planet), 1.0F, alpha, shipYaw, shipPitch);
    }

    /** Draws a planet with a fixed body-space orientation, transformed by the ship view. */
    private static void drawOrientedPlanetSphere(PoseStack pose, Matrix4f matrix, Planet planet,
                                                  float cx, float cy, float cz, float scale,
                                                  Vector3f worldSun, float brightness, float alpha,
                                                  float shipYaw, float shipPitch)
    {
        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, PLANET_TEXTURES.get(planet));
        RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);

        // The VBO contains body-oriented positions and fixed per-vertex light.
        // Only the ship view changes each frame, so compose it into the model
        // matrix instead of allocating Vec3 objects and recalculating trig for
        // every vertex.
        Matrix4f model = new Matrix4f(RenderSystem.getModelViewMatrix())
                .mul(matrix)
                .translate(cx, cy, cz)
                .rotateX((float) Math.toRadians(-shipPitch))
                .rotateY((float) Math.toRadians(-shipYaw))
                .scale(scale);
        VertexBuffer surface = getPlanetSurfaceBuffer(planet, worldSun);
        surface.bind();
        surface.drawWithShader(model, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.setShaderColor(1,1,1,1); RenderSystem.depthMask(true); RenderSystem.disableBlend();
    }

    private static VertexBuffer getPlanetSurfaceBuffer(Planet planet, Vector3f worldSun)
    {
        VertexBuffer cached = PLANET_SURFACE_BUFFERS.get(planet);
        if (cached != null && !cached.isInvalid())
            return cached;

        Vector3f orientation = BODY_ORIENTATION.getOrDefault(planet, new Vector3f());
        float yaw = (float) Math.toRadians(orientation.y);
        float pitch = (float) Math.toRadians(orientation.x);
        float yawCos = (float) Math.cos(yaw);
        float yawSin = (float) Math.sin(yaw);
        float pitchCos = (float) Math.cos(pitch);
        float pitchSin = (float) Math.sin(pitch);

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < SPHERE_X.length; i++)
        {
            float localX = SPHERE_X[i];
            float localY = SPHERE_Y[i];
            float localZ = SPHERE_Z[i];
            float yawX = localX * yawCos + localZ * yawSin;
            float yawZ = -localX * yawSin + localZ * yawCos;
            float worldX = yawX;
            float worldY = localY * pitchCos - yawZ * pitchSin;
            float worldZ = localY * pitchSin + yawZ * pitchCos;
            addLitSphereVertex(bb, worldX, worldY, worldZ, SPHERE_U[i], SPHERE_V[i], worldSun);
        }

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(bb.buildOrThrow());
        VertexBuffer.unbind();
        PLANET_SURFACE_BUFFERS.put(planet, buffer);
        return buffer;
    }

    /**
     * Draws a textured, sun-lit planet sphere at the given center. Also used by
     * {@link MoltenMoonRenderer} for the overworld moon (with a phase-dependent
     * sun direction and brightness).
     */
    static void drawPlanetSphere(PoseStack pose, Matrix4f matrix, ResourceLocation texture,
                                 float cx, float cy, float cz, float scale,
                                 Vector3f sun, float brightness, float alpha)
    {
        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);

        Matrix4f model = new Matrix4f(RenderSystem.getModelViewMatrix())
                .mul(matrix)
                .translate(cx, cy, cz)
                .scale(scale);
        VertexBuffer surface = getMoonSurfaceBuffer(sun);
        surface.bind();
        surface.drawWithShader(model, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static VertexBuffer getMoonSurfaceBuffer(Vector3f sun)
    {
        boolean lightingChanged = moonSurfaceBuffer == null || moonSurfaceBuffer.isInvalid()
                || Float.compare(moonSurfaceSunX, sun.x) != 0
                || Float.compare(moonSurfaceSunY, sun.y) != 0
                || Float.compare(moonSurfaceSunZ, sun.z) != 0;
        if (!lightingChanged)
            return moonSurfaceBuffer;

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < SPHERE_X.length; i++)
            addLitSphereVertex(bb, SPHERE_X[i], SPHERE_Y[i], SPHERE_Z[i], SPHERE_U[i], SPHERE_V[i], sun);

        if (moonSurfaceBuffer == null || moonSurfaceBuffer.isInvalid())
            moonSurfaceBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        moonSurfaceBuffer.bind();
        moonSurfaceBuffer.upload(bb.buildOrThrow());
        VertexBuffer.unbind();
        moonSurfaceSunX = sun.x;
        moonSurfaceSunY = sun.y;
        moonSurfaceSunZ = sun.z;
        return moonSurfaceBuffer;
    }

    private static void addLitSphereVertex(BufferBuilder bb, float x, float y, float z,
                                           float u, float v, Vector3f sun)
    {
        float dot = x / PLANET_RADIUS * sun.x
                + y / PLANET_RADIUS * sun.y
                + z / PLANET_RADIUS * sun.z;
        float shade = 1.0F - smoothstep((dot + 0.25F) / 0.5F);
        float r = lerp(1.0F, 0.06F, shade);
        float g = lerp(1.0F, 0.08F, shade);
        float b = lerp(1.0F, 0.20F, shade);
        float terminator = Math.max(0.0F, 1.0F - Math.abs(dot) / 0.22F);
        r += (0.90F - r) * terminator * 0.35F;
        g += (0.55F - g) * terminator * 0.25F;
        b += (0.25F - b) * terminator * 0.18F;
        bb.addVertex(x, y, z).setColor(r, g, b, 1.0F).setUv(u, v);
    }

    private static void renderWarpStreaks(PoseStack pose, Camera cam, float partialTick,
                                          SpaceRenderContext space)
    {
        float progress = space.warpProgress();
        FlightPhase flightPhase = space.flightPhase();
        int duration = Math.max(1, space.warpDurationTicks());
        float accelStart = ShipFlightController.TURN_TICKS / (float) duration;
        float hyperspaceStart = (ShipFlightController.TURN_TICKS + ShipFlightController.ACCEL_TICKS) / (float) duration;

        // Entrance choreography: the tunnel builds during the last part of the
        // ship's turn as individual dots; the dots then stretch into streaks
        // toward the vanishing point; only after the turn completes does the
        // streak motion (the "jump") ramp in.
        // Use the last half of ACCELERATE as the pre-jump buildup. The first
        // quarter shows bright points, then they elongate while remaining fixed;
        // actual forward motion starts exactly at HYPERSPACE.
        float introStart = lerp(accelStart, hyperspaceStart, 0.45F);
        float introSpan = Math.max(0.0001F, hyperspaceStart - introStart);
        float introElapsed = Math.max(0.0F, Math.min(1.0F, (progress - introStart) / introSpan));
        double fadeIn = smoothstep(introElapsed / 0.28F);
        double introStretch = smoothstep((introElapsed - 0.22F) / 0.78F);

        // Crossfade with the arriving planet: the tunnel fades out over the tail of the warp.
        float decelStart = (duration - ShipFlightController.DECEL_TICKS - ShipFlightController.ARRIVE_TICKS) / (float) duration;
        float fadeEnd = (duration - ShipFlightController.ARRIVE_TICKS) / (float) duration;
        double tunnelFade = 1.0 - smoothstep((progress - decelStart) / Math.max(0.0001F, fadeEnd - decelStart));
        double tunnelAlpha = tunnelFade * fadeIn;
        if (tunnelAlpha <= 0.001)
            return;

        // Motion ramps in after the turn and keeps cruising until the tunnel
        // crossfades out on arrival.
        double motionIn = (flightPhase == FlightPhase.HYPERSPACE || flightPhase == FlightPhase.DECELERATE)
                ? smoothstep((progress - hyperspaceStart) / Math.max(MOTION_RAMP, 0.015F)) : 0.0;

        // Length: dots stretch with an overshoot for a smoother handoff into the
        // motion phase, then settle at the cruise length.
        double overshoot = STRETCH_OVERSHOOT * Math.max(0.0, introStretch - motionIn);
        double lenScale = Math.max(MIN_LENGTH, introStretch) * (1.0 + overshoot);

        Matrix4f matrix = pose.last().pose();
        // Use the authoritative interpolated flight clock. This remains
        // continuous through pauses and tracks the ship's actual acceleration
        // instead of advancing independently on wall-clock time.
        double animationTicks = progress * duration;

        // Entrance geometry appears and stretches before it travels. A separate
        // clock starts at the hyperspace boundary and eases its velocity from
        // rest, avoiding the old pre-jump motion plus speed multiplier kick.
        double hyperspaceStartTicks = hyperspaceStart * duration;
        double cruiseTicks = Math.max(0.0, animationTicks - hyperspaceStartTicks);
        double accelerationRamp = smoothstep((float) (cruiseTicks
                / Math.max(1.0, duration * MOTION_RAMP)));
        // Reach a substantially faster cruise without changing the zero-speed
        // handoff. The eased multiplier preserves the heavy acceleration feel.
        double motionTicks = cruiseTicks * (0.10 + 1.70 * accelerationRamp);
        double motionLengthScale = lenScale * (1.0 + 0.42 * accelerationRamp);

        // Slow rotation of the whole tunnel; streaks lengthen as the warp progresses.
        double swirl = motionTicks * 0.006;

        // Near arrival the tunnel shifts toward the target star's corona color,
        // linking the jump flash to the stellar identity of the destination.
        float tintAmount = 0.0F;
        Vector3f arrivalTint = null;
        Planet target = space.targetBody();
        if (target != null && progress >= WarpVisualTiming.ARRIVAL_FADE_START)
        {
            arrivalTint = STELLAR_CORONA_COLORS.get(target);
            tintAmount = smoothstep((progress - WarpVisualTiming.ARRIVAL_FADE_START)
                    / (1.0F - WarpVisualTiming.ARRIVAL_FADE_START)) * 0.65F;
        }

        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Distant star layer: thin, slow, dim streaks behind the main tunnel.
        // Drawn first so the bright foreground streaks layer on top.
        for (int i = 0; i < TUNNEL_STAR_COUNT; i++)
        {
            double theta = TUNNEL_THETA[i] + swirl * 0.55;
            double d = TUNNEL_RADIUS[i];
            double rx = Math.cos(theta) * d;
            double ry = Math.sin(theta) * d;

            double period = TUNNEL_PERIOD[i];
            double travel = ((motionTicks * 0.62 + TUNNEL_PHASE[i]) % period) / period;
            double headZ = TUNNEL_NEAR_Z + (1.0 - travel) * (TUNNEL_FAR_Z - TUNNEL_NEAR_Z);
            double tailZ = Math.min(TUNNEL_FAR_Z + 34.0, headZ + 8.0 + 26.0 * motionLengthScale);
            // Keep a star on one world-space ray and vary only depth. Scaling
            // x/y by z here would preserve the same screen coordinate at both
            // ends, collapsing the streak into a point after projection.
            double headX = rx;
            double headY = ry;
            double tailX = rx;
            double tailY = ry;
            double halfW = TUNNEL_WIDTH[i];
            double fade = 0.15 + 0.85 * travel;
            double alpha = 0.36 * fade * (1.0 - progress * 0.16) * tunnelAlpha;

            // Width is applied along the tangential (perpendicular to radial)
            // direction, so every streak points at the vanishing point.
            double len = Math.max(0.0001, d);
            double tx = -ry / len;
            double ty = rx / len;
            float cr = 0.72F, cg = 0.82F, cb = 1.0F;
            if (arrivalTint != null)
            {
                cr = lerp(cr, arrivalTint.x, tintAmount);
                cg = lerp(cg, arrivalTint.y, tintAmount);
                cb = lerp(cb, arrivalTint.z, tintAmount);
            }
            float a = (float) alpha;

            taperedStreak(bb, matrix, headX, headY, headZ, tailX, tailY, tailZ,
                    tx, ty, halfW, halfW * 0.70, cr, cg, cb, a);
        }

        // Main foreground streak tunnel.
        for (int i = 0; i < STREAK_COUNT; i++)
        {
            // radial distribution around the view axis, biased toward the core
            double theta = STREAK_THETA[i] + swirl;
            double d = STREAK_RADIUS[i];
            double rx = Math.cos(theta) * d;
            double ry = Math.sin(theta) * d;

            double period = STREAK_PERIOD[i];
            double travel = ((motionTicks * 0.88 + STREAK_PHASE[i]) % period) / period;
            double headZ = TUNNEL_NEAR_Z + (1.0 - travel) * (TUNNEL_FAR_Z - TUNNEL_NEAR_Z);
            double tailZ = Math.min(TUNNEL_FAR_Z + 48.0, headZ + 14.0 + 42.0 * motionLengthScale);
            double headX = rx;
            double headY = ry;
            double tailX = rx;
            double tailY = ry;

            double halfW = STREAK_WIDTH[i];
            double fade = 0.15 + 0.85 * travel;
            double alpha = 0.66 * fade * (1.0 - progress * 0.18) * tunnelAlpha;

            // Mostly ice blue-white, with cyan and violet accents.
            float cr, cg, cb;
            double tint = ((i * 37) % 100) / 100.0;
            if (tint < 0.12)
            {
                cr = 0.75F; cg = 0.65F; cb = 1.0F;
            }
            else if (tint < 0.30)
            {
                cr = 0.55F; cg = 0.95F; cb = 1.0F;
            }
            else
            {
                cr = 0.85F; cg = 0.95F; cb = 1.0F;
            }
            if (arrivalTint != null)
            {
                cr = lerp(cr, arrivalTint.x, tintAmount);
                cg = lerp(cg, arrivalTint.y, tintAmount);
                cb = lerp(cb, arrivalTint.z, tintAmount);
            }

            // Same tangential width as the star layer: radial streaks, not bars.
            double len = Math.max(0.0001, d);
            double tx = -ry / len;
            double ty = rx / len;
            taperedStreak(bb, matrix, headX, headY, headZ, tailX, tailY, tailZ,
                    tx, ty, halfW, halfW * 0.80, cr, cg, cb, (float) alpha);
            taperedStreak(bb, matrix, headX, headY, headZ, tailX, tailY, tailZ,
                    tx, ty, halfW * 0.22, halfW * 0.12,
                    0.96F, 0.99F, 1.0F, (float) Math.min(1.0, alpha * 1.35));
        }

        // Near layer: fewer, broader streaks with a shorter travel depth. The
        // parallax against the mid layer makes the tunnel read as volume rather
        // than a flat set of radial bars.
        for (int i = 0; i < NEAR_STREAK_COUNT; i++)
        {
            double theta = NEAR_THETA[i] + swirl * 1.25;
            double d = NEAR_RADIUS[i];
            double rx = Math.cos(theta) * d;
            double ry = Math.sin(theta) * d;
            double period = NEAR_PERIOD[i];
            double travel = ((motionTicks * 1.12 + NEAR_PHASE[i]) % period) / period;
            double headZ = TUNNEL_NEAR_Z + (1.0 - travel) * (TUNNEL_FAR_Z - TUNNEL_NEAR_Z);
            double tailZ = Math.min(TUNNEL_FAR_Z + 64.0, headZ + 20.0 + 58.0 * motionLengthScale);
            double tailX = rx;
            double tailY = ry;
            double headX = rx;
            double headY = ry;
            double halfW = NEAR_WIDTH[i];
            double fade = 0.15 + 0.85 * travel;
            double alpha = 0.82 * fade * (1.0 - progress * 0.14) * tunnelAlpha;
            double len = Math.max(0.0001, d);
            double tx = -ry / len;
            double ty = rx / len;
            float cr = 0.72F, cg = 0.92F, cb = 1.0F;
            if (arrivalTint != null)
            {
                cr = lerp(cr, arrivalTint.x, tintAmount);
                cg = lerp(cg, arrivalTint.y, tintAmount);
                cb = lerp(cb, arrivalTint.z, tintAmount);
            }
            taperedStreak(bb, matrix, headX, headY, headZ, tailX, tailY, tailZ,
                    tx, ty, halfW, halfW * 0.72, cr, cg, cb, (float) alpha);
            taperedStreak(bb, matrix, headX, headY, headZ, tailX, tailY, tailZ,
                    tx, ty, halfW * 0.20, halfW * 0.11,
                    1.0F, 1.0F, 1.0F, (float) Math.min(1.0, alpha * 1.45));
        }

        // Edge layer: short, fast particles with a much wider radial spread.
        // These cross the outer screen instead of clustering around the core.
        for (int i = 0; i < EDGE_STREAK_COUNT; i++)
        {
            double theta = EDGE_THETA[i] + swirl * 1.55;
            double d = EDGE_RADIUS[i];
            double rx = Math.cos(theta) * d;
            double ry = Math.sin(theta) * d;
            double period = EDGE_PERIOD[i];
            double travel = ((motionTicks * 1.55 + EDGE_PHASE[i]) % period) / period;
            double headZ = TUNNEL_NEAR_Z + (1.0 - travel) * 96.0;
            double tailZ = Math.min(148.0, headZ + 8.0 + 24.0 * motionLengthScale);
            double len = Math.max(0.0001, d);
            double tx = -ry / len;
            double ty = rx / len;
            double alpha = 0.58 * (0.20 + 0.80 * travel) * (1.0 - progress * 0.16) * tunnelAlpha;
            float cr = 0.48F, cg = 0.82F, cb = 1.0F;
            if (arrivalTint != null)
            {
                cr = lerp(cr, arrivalTint.x, tintAmount * 0.5F);
                cg = lerp(cg, arrivalTint.y, tintAmount * 0.5F);
                cb = lerp(cb, arrivalTint.z, tintAmount * 0.5F);
            }
            taperedStreak(bb, matrix, rx, ry, headZ, rx, ry, tailZ,
                    tx, ty, EDGE_WIDTH[i], EDGE_WIDTH[i] * 0.55, cr, cg, cb, (float) alpha);
            taperedStreak(bb, matrix, rx, ry, headZ, rx, ry, tailZ,
                    tx, ty, EDGE_WIDTH[i] * 0.18, EDGE_WIDTH[i] * 0.09,
                    0.92F, 0.98F, 1.0F, (float) Math.min(1.0, alpha * 1.30));
        }

        // Full surround shell. Unlike the forward tunnel, z spans both sides of
        // the camera, so turning toward a side or rear window still reveals
        // flowing space. The near-camera exclusion prevents sudden white clips.
        for (int i = 0; i < SURROUND_STREAK_COUNT; i++)
        {
            double theta = SURROUND_THETA[i] + swirl * 0.38;
            double radius = SURROUND_RADIUS[i];
            double x = Math.cos(theta) * radius;
            double y = Math.sin(theta) * radius;
            double period = SURROUND_PERIOD[i];
            double travel = ((motionTicks * 0.78 + SURROUND_PHASE[i]) % period) / period;
            double headZ = -170.0 + travel * 340.0;
            if (Math.abs(headZ) < 7.0)
                headZ = Math.copySign(7.0, headZ == 0.0 ? 1.0 : headZ);
            double direction = headZ >= 0.0 ? 1.0 : -1.0;
            double tailZ = headZ + direction * (10.0 + 34.0 * motionLengthScale);
            double alpha = 0.42 * (0.45 + 0.55 * Math.abs(headZ) / 170.0)
                    * (0.35 + 0.65 * smoothstep((float) Math.min(1.0, cruiseTicks / 18.0))) * tunnelAlpha;
            double tx = -Math.sin(theta);
            double ty = Math.cos(theta);
            float cr = 0.62F, cg = 0.86F, cb = 1.0F;
            if (arrivalTint != null)
            {
                cr = lerp(cr, arrivalTint.x, tintAmount * 0.45F);
                cg = lerp(cg, arrivalTint.y, tintAmount * 0.45F);
                cb = lerp(cb, arrivalTint.z, tintAmount * 0.45F);
            }
            taperedStreak(bb, matrix, x, y, headZ, x, y, tailZ,
                    tx, ty, SURROUND_WIDTH[i], SURROUND_WIDTH[i] * 0.62,
                    cr, cg, cb, (float) alpha);
            taperedStreak(bb, matrix, x, y, headZ, x, y, tailZ,
                    tx, ty, SURROUND_WIDTH[i] * 0.18, SURROUND_WIDTH[i] * 0.10,
                    0.90F, 0.97F, 1.0F, (float) Math.min(0.90, alpha * 1.25));
        }

        // Core glow at the vanishing point (+Z is forward): pulses, grows as the
        // warp progresses, and picks up the target planet's color on approach.
        double pulse = 0.18 + 0.04 * Math.sin(animationTicks * 0.16);
        float coreR = 0.35F, coreG = 0.75F, coreB = 1.0F;
        if (arrivalTint != null)
        {
            coreR = lerp(coreR, arrivalTint.x, tintAmount);
            coreG = lerp(coreG, arrivalTint.y, tintAmount);
            coreB = lerp(coreB, arrivalTint.z, tintAmount);
        }
        float coreGrow = 1.0F + progress * 0.9F;
        // The core blooms as the dots stretch toward the vanishing point.
        float coreBloom = 0.4F + 0.6F * (float) introStretch;
        double coreAlpha = tunnelAlpha * introStretch;
        drawRadialGlow(bb, matrix, 44.0F * coreGrow * coreBloom,
                coreR, coreG, coreB, (float) (pulse * coreAlpha));
        drawRadialGlow(bb, matrix, 14.0F * coreGrow * coreBloom,
                1.0F, 1.0F, 1.0F, (float) (pulse * 1.25 * coreAlpha));

        // Localized hyperspace-entry flash. It blooms at the vanishing point
        // for a fraction of a second instead of covering the entire viewport.
        // The short span keeps this distinct from the normal cruising glow.
        double entryT = (progress - hyperspaceStart) / 0.025F;
        double entryUp = smoothstep((float) (entryT / 0.16));
        double entryDown = smoothstep((float) ((entryT - 0.16) / 0.84));
        double entryFlash = entryUp * (1.0 - entryDown);
        if (entryFlash > 0.001)
        {
            double flashAlpha = entryFlash * tunnelAlpha;
            // Full-viewport entry flash. The outer ring is large enough to
            // cover the complete projection at the tunnel depth; its lower
            // opacity preserves the blue-white falloff instead of producing a
            // flat opaque white frame.
            drawRadialGlow(bb, matrix, 920.0F * (1.0F + (float) entryFlash * 0.12F),
                    coreR, coreG, coreB, (float) (flashAlpha * 0.30));
            drawRadialGlow(bb, matrix, 600.0F * (1.0F + (float) entryFlash * 0.16F),
                    0.66F, 0.88F, 1.0F, (float) (flashAlpha * 0.46));
            drawRadialGlow(bb, matrix, 190.0F * (1.0F + (float) entryFlash * 0.22F),
                    1.0F, 1.0F, 1.0F, (float) (flashAlpha * 0.80));
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Multi-ring radial falloff without a texture or visible square boundary. */
    private static void drawRadialGlow(BufferBuilder bb, Matrix4f matrix, float radius,
                                       float r, float g, float b, float alpha)
    {
        final int segments = 64;
        float[] rings = { 0.0F, 0.18F, 0.42F, 0.72F, 1.0F };
        float[] alphas = { alpha, alpha * 0.82F, alpha * 0.42F, alpha * 0.12F, 0.0F };
        for (int ring = 0; ring < rings.length - 1; ring++)
        {
            float inner = radius * rings[ring];
            float outer = radius * rings[ring + 1];
            for (int i = 0; i < segments; i++)
            {
                double a0 = Math.PI * 2.0 * i / segments;
                double a1 = Math.PI * 2.0 * (i + 1) / segments;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                vertexColor(bb, matrix, c0 * inner, s0 * inner, 300.0F, r, g, b, alphas[ring]);
                vertexColor(bb, matrix, c1 * inner, s1 * inner, 300.0F, r, g, b, alphas[ring]);
                vertexColor(bb, matrix, c1 * outer, s1 * outer, 300.0F, r, g, b, alphas[ring + 1]);
                vertexColor(bb, matrix, c0 * inner, s0 * inner, 300.0F, r, g, b, alphas[ring]);
                vertexColor(bb, matrix, c1 * outer, s1 * outer, 300.0F, r, g, b, alphas[ring + 1]);
                vertexColor(bb, matrix, c0 * outer, s0 * outer, 300.0F, r, g, b, alphas[ring + 1]);
            }
        }
    }

    /** Pointed streak geometry avoids the square cap produced by a wide quad. */
    private static void taperedStreak(BufferBuilder bb, Matrix4f matrix,
                                      double headX, double headY, double headZ,
                                      double tailX, double tailY, double tailZ,
                                      double tx, double ty, double halfWidth,
                                      double tailHalfWidth, float r, float g, float b,
                                      float headAlpha)
    {
        float hx = (float) headX, hy = (float) headY, hz = (float) headZ;
        float tailPlusX = (float) (tailX + tx * tailHalfWidth);
        float tailPlusY = (float) (tailY + ty * tailHalfWidth);
        float tailMinusX = (float) (tailX - tx * tailHalfWidth);
        float tailMinusY = (float) (tailY - ty * tailHalfWidth);
        float tailZf = (float) tailZ;
        float shoulderPlusX = (float) (headX + (tailX - headX) * 0.16 + tx * halfWidth * 0.18);
        float shoulderPlusY = (float) (headY + (tailY - headY) * 0.16 + ty * halfWidth * 0.18);
        float shoulderMinusX = (float) (headX + (tailX - headX) * 0.16 - tx * halfWidth * 0.18);
        float shoulderMinusY = (float) (headY + (tailY - headY) * 0.16 - ty * halfWidth * 0.18);
        float shoulderZ = (float) (headZ + (tailZ - headZ) * 0.16);

        // Pointed cap.
        vertexColor(bb, matrix, hx, hy, hz, r, g, b, headAlpha);
        vertexColor(bb, matrix, shoulderPlusX, shoulderPlusY, shoulderZ, r, g, b, headAlpha * 0.72F);
        vertexColor(bb, matrix, shoulderMinusX, shoulderMinusY, shoulderZ, r, g, b, headAlpha * 0.72F);
        // Filled fading ribbon behind the cap.
        vertexColor(bb, matrix, shoulderPlusX, shoulderPlusY, shoulderZ, r, g, b, headAlpha * 0.72F);
        vertexColor(bb, matrix, tailPlusX, tailPlusY, tailZf, r, g, b, headAlpha * 0.035F);
        vertexColor(bb, matrix, tailMinusX, tailMinusY, tailZf, r, g, b, headAlpha * 0.035F);
        vertexColor(bb, matrix, shoulderPlusX, shoulderPlusY, shoulderZ, r, g, b, headAlpha * 0.72F);
        vertexColor(bb, matrix, tailMinusX, tailMinusY, tailZf, r, g, b, headAlpha * 0.035F);
        vertexColor(bb, matrix, shoulderMinusX, shoulderMinusY, shoulderZ, r, g, b, headAlpha * 0.72F);
    }

    private static void vertexColor(BufferBuilder bb, Matrix4f matrix, float x, float y, float z,
                                    float r, float g, float b, float a)
    {
        bb.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }

    private static void vertexColor(BufferBuilder bb, Matrix4f matrix, float x, float y, float z, float[] rgba)
    {
        vertexColor(bb, matrix, x, y, z, rgba[0], rgba[1], rgba[2], rgba[3]);
    }
}
