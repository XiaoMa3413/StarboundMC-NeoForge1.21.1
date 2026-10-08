package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starboundmc.client.StarmapUniverse;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.space.WarpRenderVisibility;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.world.starmap.StellarVisualProfile;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Owns the existing hyperspace streak, tunnel, and arrival flash passes. */
public final class WarpRenderer
{
    private static final float MOTION_RAMP = 0.10F;
    private static final double MIN_LENGTH = 0.04;
    private static final float STRETCH_OVERSHOOT = 1.2F;
    private static final double TUNNEL_NEAR_Z = 8.0;
    private static final double TUNNEL_FAR_Z = 260.0;
    private static final int STREAK_COUNT = 220;
    private static final int NEAR_STREAK_COUNT = 120;
    private static final int TUNNEL_STAR_COUNT = 420;
    private static final int EDGE_STREAK_COUNT = 96;
    private static final int SURROUND_STREAK_COUNT = 260;
    private static final Map<String, Vector3f> STELLAR_CORONA_COLORS = new HashMap<>();

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
        for (var system : StarmapUniverse.allSystems())
        {
            int color = system.stellarVisual().getCoronaColor();
            STELLAR_CORONA_COLORS.put(system.systemId(), new Vector3f(
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F));
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

    private WarpRenderer()
    {
    }

    static void render(PoseStack pose, Camera camera, float partialTick, SpaceRenderContext space)
    {
        if (WarpRenderVisibility.shouldRenderStreaks(space.flightPhase(), space.warpDurationTicks()))
            renderWarpStreaks(pose, camera, partialTick, space);
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

        // A slow intensity breath keeps the long hyperspace middle alive
        // without introducing a visible camera shake or changing travel speed.
        double cruiseBreath = 0.94 + 0.06 * Math.sin(cruiseTicks * 0.16);
        tunnelAlpha *= cruiseBreath;

        // Slow rotation of the whole tunnel; streaks lengthen as the warp progresses.
        double swirl = motionTicks * 0.006;

        // Near arrival the tunnel shifts toward the target star's corona color,
        // linking the jump flash to the stellar identity of the destination.
        float tintAmount = 0.0F;
        Vector3f arrivalTint = null;
        String targetSystemId = StarmapUniverse.systemIdOfEntry(space.targetBodyId());
        if (targetSystemId != null && progress >= WarpVisualTiming.ARRIVAL_FADE_START)
        {
            arrivalTint = STELLAR_CORONA_COLORS.get(targetSystemId);
            tintAmount = smoothstep((progress - WarpVisualTiming.ARRIVAL_FADE_START)
                    / (1.0F - WarpVisualTiming.ARRIVAL_FADE_START)) * 0.65F;
        }

        FogRenderer.setupNoFog();
        try
        {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

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
        }
        finally
        {
            SpaceRenderPassState.restoreDefaults();
        }
    }
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
    private static float smoothstep(float t)
    {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }
    private static float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }
}
