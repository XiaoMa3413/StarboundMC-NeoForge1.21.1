package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starboundmc.client.compat.stellarview.StellarViewStarfield;
import com.starboundmc.client.space.GalaxyEnvironmentBlend;
import com.starboundmc.client.space.SpaceCoordinateFrame;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.warp.ShipFlightController;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

/** Draws the existing space dome and deterministic 2,000-star background shell. */
public final class SpaceBackgroundRenderer
{
    private static final int STAR_COUNT = 2000;
    private static final float STAR_DISTANCE = 200.0F;
    private static final float STAR_SIZE_SCALE = 0.85F;

    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final float[] STAR_Z = new float[STAR_COUNT];
    private static final float[] STAR_SIZE = new float[STAR_COUNT];
    private static final float[] STAR_BRIGHT = new float[STAR_COUNT];
    private static final float[] STAR_R = new float[STAR_COUNT];
    private static final float[] STAR_G = new float[STAR_COUNT];
    private static final float[] STAR_B = new float[STAR_COUNT];
    private static final float[] STAR_TWINKLE = new float[STAR_COUNT];

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
    }

    private SpaceBackgroundRenderer()
    {
    }

    static void renderStarField(PoseStack pose, ClientLevel level, Camera camera, float partialTick,
                                SpaceRenderContext space,
                                SpaceCoordinateFrame frame, GalaxyEnvironmentBlend environment)
    {
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
            // During the jump the moving tunnel is the environment. Keep a
            // restrained floor of the static shell so first-person peripheral
            // vision retains orientation and depth instead of becoming a flat
            // blue void through the middle of a long jump.
            float shellFade = enter * (1.0F - exit);
            starAlpha = 1.0F - 0.88F * shellFade;
        }
        if (starAlpha > 0.01F)
        {
            // Match the established star orientation: stable camera + visual roll,
            // then inverse ship pitch/yaw. The coordinate provider independently
            // anchors the field to the virtual ship position, never the player.
            Matrix4f starModelView = new Matrix4f(pose.last().pose())
                    .rotateX((float) Math.toRadians(-space.pitch()))
                    .rotateY((float) Math.toRadians(-space.yaw()));
            if (StellarViewStarfield.render(level, camera, partialTick, starModelView,
                    RenderSystem.getProjectionMatrix(), space, starAlpha))
                return;
        }

        renderStarField(pose, frame, starAlpha, starConvergence,
                environment.skyTintColor(), environment.skyTintAmount());
    }

    static void renderSpaceDome(PoseStack pose)
    {
        Matrix4f matrix = pose.last().pose();
        try
        {
            FogRenderer.setupNoFog();
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

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
        }
        finally
        {
            SpaceRenderPassState.restoreDefaults();
        }
    }

    static void renderStarField(PoseStack pose, SpaceCoordinateFrame frame,
                                float alpha, float convergence,
                                int tintColor, float tintAmount)
    {
        if (alpha <= 0.01F)
            return;

        Matrix4f matrix = pose.last().pose();
        long now = System.currentTimeMillis();
        float tintR = ((tintColor >> 16) & 0xFF) / 255.0F;
        float tintG = ((tintColor >> 8) & 0xFF) / 255.0F;
        float tintB = (tintColor & 0xFF) / 255.0F;
        Vector3f rotated = new Vector3f();

        try
        {
            FogRenderer.setupNoFog();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            BufferBuilder bb = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i < STAR_COUNT; i++)
            {
                // Hyperspace entrance: stars in the forward hemisphere collapse
                // toward the flight axis before the tunnel takes over. Renormalize
                // after squeezing so the star shell stays at a stable distance.
                frame.toBackgroundDirection(STAR_X[i], STAR_Y[i], STAR_Z[i], rotated);
                float x1 = rotated.x;
                float y2 = rotated.y;
                float z2 = rotated.z;
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
        }
        finally
        {
            SpaceRenderPassState.restoreDefaults();
        }
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
        return new float[] { lerp(0.010F, 0.028F, t), lerp(0.014F, 0.038F, t),
                lerp(0.035F, 0.095F, t), 1.0F };
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

    private static void vertexColor(BufferBuilder bb, Matrix4f matrix, float x, float y, float z,
                                    float r, float g, float b, float a)
    {
        bb.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }

    private static void vertexColor(BufferBuilder bb, Matrix4f matrix, float x, float y, float z,
                                    float[] rgba)
    {
        vertexColor(bb, matrix, x, y, z, rgba[0], rgba[1], rgba[2], rgba[3]);
    }
}
