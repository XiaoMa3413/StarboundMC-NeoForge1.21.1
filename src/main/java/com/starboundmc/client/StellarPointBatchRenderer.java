package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starboundmc.client.space.StarSystemResolver;
import com.starboundmc.world.starmap.StellarVisualProfile;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** Draws all LOD-2 stellar points in one allocation-free buffer submission. */
public final class StellarPointBatchRenderer
{
    private static final int POINT_SEGMENTS = 12;
    private static final float POINT_HALO_RADIUS = 3.0F;
    private static final float POINT_CORE_RADIUS = 1.45F;
    private static final float[] GLOW_RADII = { 0.0F, 0.20F, 0.48F, 0.76F, 1.0F };
    private static final float[] GLOW_ALPHA = { 1.0F, 0.78F, 0.36F, 0.10F, 0.0F };
    private static final float[] POINT_COS = new float[POINT_SEGMENTS + 1];
    private static final float[] POINT_SIN = new float[POINT_SEGMENTS + 1];

    static
    {
        for (int i = 0; i <= POINT_SEGMENTS; i++)
        {
            double angle = Math.PI * 2.0 * i / POINT_SEGMENTS;
            POINT_COS[i] = (float) Math.cos(angle);
            POINT_SIN[i] = (float) Math.sin(angle);
        }
    }

    private StellarPointBatchRenderer()
    {
    }

    public static void render(PoseStack pose, StarSystemResolver.ResolvedStarField stars,
                              double yawCos, double yawSin,
                              double pitchCos, double pitchSin)
    {
        if (!hasPoints(stars))
            return;

        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Matrix4f matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < stars.count(); i++)
        {
            StarSystemResolver.VisibleStar star = stars.star(i);
            if (star.pointLodWeight() <= 0.002F)
                continue;

            double viewX = star.relativeX() * yawCos + star.relativeZ() * yawSin;
            double yawZ = -star.relativeX() * yawSin + star.relativeZ() * yawCos;
            double viewY = star.relativeY() * pitchCos - yawZ * pitchSin;
            double viewZ = star.relativeY() * pitchSin + yawZ * pitchCos;
            addPoint(buffer, matrix, star, viewX, viewY, viewZ);
        }
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static boolean hasPoints(StarSystemResolver.ResolvedStarField stars)
    {
        for (int i = 0; i < stars.count(); i++)
            if (stars.star(i).pointLodWeight() > 0.002F)
                return true;
        return false;
    }

    private static void addPoint(BufferBuilder buffer, Matrix4f matrix,
                                 StarSystemResolver.VisibleStar star,
                                 double viewX, double viewY, double viewZ)
    {
        double lengthSqr = viewX * viewX + viewY * viewY + viewZ * viewZ;
        if (lengthSqr < 1.0E-8)
            return;
        double inverseLength = 1.0 / Math.sqrt(lengthSqr);
        float dx = (float) (viewX * inverseLength);
        float dy = (float) (viewY * inverseLength);
        float dz = (float) (viewZ * inverseLength);
        float cx = dx * StellarRenderer.SHIP_SKY_DISTANCE;
        float cy = dy * StellarRenderer.SHIP_SKY_DISTANCE;
        float cz = dz * StellarRenderer.SHIP_SKY_DISTANCE;

        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        float rightX, rightY, rightZ;
        float upX, upY, upZ;
        if (horizontal > 1.0E-5F)
        {
            rightX = dz / horizontal;
            rightY = 0.0F;
            rightZ = -dx / horizontal;
            upX = -dy * dx / horizontal;
            upY = horizontal;
            upZ = -dy * dz / horizontal;
        }
        else
        {
            rightX = 1.0F;
            rightY = 0.0F;
            rightZ = 0.0F;
            upX = 0.0F;
            upY = 0.0F;
            upZ = dy >= 0.0F ? -1.0F : 1.0F;
        }

        StellarVisualProfile profile = star.system().getStellarVisual();
        float radius = POINT_HALO_RADIUS;
        int color = profile.getCoreColor();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float pointWeight = star.pointLodWeight();
        float brightness = star.stellarBrightness();
        float alpha = brightness * pointWeight;

        for (int segment = 0; segment < POINT_SEGMENTS; segment++)
        {
            // Match StellarRenderer's four-ring glow profile so batching does
            // not make the same LOD-2 point perceptually dimmer.
            pointVertex(buffer, matrix, cx, cy, cz, red, green, blue, alpha);
            radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                    upX, upY, upZ, radius * GLOW_RADII[1],
                    POINT_COS[segment], POINT_SIN[segment], red, green, blue,
                    alpha * GLOW_ALPHA[1]);
            radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                    upX, upY, upZ, radius * GLOW_RADII[1],
                    POINT_COS[segment + 1], POINT_SIN[segment + 1], red, green, blue,
                    alpha * GLOW_ALPHA[1]);

            for (int ring = 1; ring < GLOW_RADII.length - 1; ring++)
            {
                float innerRadius = radius * GLOW_RADII[ring];
                float outerRadius = radius * GLOW_RADII[ring + 1];
                float innerAlpha = alpha * GLOW_ALPHA[ring];
                float outerAlpha = alpha * GLOW_ALPHA[ring + 1];
                radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                        upX, upY, upZ, innerRadius, POINT_COS[segment], POINT_SIN[segment],
                        red, green, blue, innerAlpha);
                radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                        upX, upY, upZ, innerRadius, POINT_COS[segment + 1], POINT_SIN[segment + 1],
                        red, green, blue, innerAlpha);
                radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                        upX, upY, upZ, outerRadius, POINT_COS[segment + 1], POINT_SIN[segment + 1],
                        red, green, blue, outerAlpha);
                radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                        upX, upY, upZ, innerRadius, POINT_COS[segment], POINT_SIN[segment],
                        red, green, blue, innerAlpha);
                radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                        upX, upY, upZ, outerRadius, POINT_COS[segment + 1], POINT_SIN[segment + 1],
                        red, green, blue, outerAlpha);
                radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                        upX, upY, upZ, outerRadius, POINT_COS[segment], POINT_SIN[segment],
                        red, green, blue, outerAlpha);
            }
        }

        float coreRadius = POINT_CORE_RADIUS;
        float coreAlpha = brightness * pointWeight;
        float coreRed = 0.68F + red * 0.32F;
        float coreGreen = 0.68F + green * 0.32F;
        float coreBlue = 0.68F + blue * 0.32F;
        addRays(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, radius, coreRadius,
                coreRed, coreGreen, coreBlue, alpha * 0.30F);
        addCore(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, coreRadius,
                coreRed, coreGreen, coreBlue, coreAlpha);
    }

    private static void addCore(BufferBuilder buffer, Matrix4f matrix,
                                float cx, float cy, float cz,
                                float rightX, float rightY, float rightZ,
                                float upX, float upY, float upZ,
                                float radius, float red, float green, float blue, float alpha)
    {
        for (int segment = 0; segment < POINT_SEGMENTS; segment++)
        {
            pointVertex(buffer, matrix, cx, cy, cz, red, green, blue, alpha);
            radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                    upX, upY, upZ, radius, POINT_COS[segment], POINT_SIN[segment],
                    red, green, blue, alpha * 0.72F);
            radialVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                    upX, upY, upZ, radius, POINT_COS[segment + 1], POINT_SIN[segment + 1],
                    red, green, blue, alpha * 0.72F);
        }
    }

    private static void addRays(BufferBuilder buffer, Matrix4f matrix,
                                float cx, float cy, float cz,
                                float rightX, float rightY, float rightZ,
                                float upX, float upY, float upZ,
                                float radius, float coreRadius,
                                float red, float green, float blue, float alpha)
    {
        float length = radius * 1.45F;
        float base = coreRadius * 0.35F;
        float halfWidth = Math.max(0.055F, radius * 0.045F);
        rayTriangle(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, length, 0.0F, base, halfWidth,
                base, -halfWidth, red, green, blue, alpha);
        rayTriangle(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, -length, 0.0F, -base, -halfWidth,
                -base, halfWidth, red, green, blue, alpha);
        rayTriangle(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, 0.0F, length, -halfWidth, base,
                halfWidth, base, red, green, blue, alpha);
        rayTriangle(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, 0.0F, -length, halfWidth, -base,
                -halfWidth, -base, red, green, blue, alpha);
    }

    private static void rayTriangle(BufferBuilder buffer, Matrix4f matrix,
                                    float cx, float cy, float cz,
                                    float rightX, float rightY, float rightZ,
                                    float upX, float upY, float upZ,
                                    float tipX, float tipY,
                                    float base1X, float base1Y,
                                    float base2X, float base2Y,
                                    float red, float green, float blue, float alpha)
    {
        basisVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, tipX, tipY, red, green, blue, 0.0F);
        basisVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, base1X, base1Y, red, green, blue, alpha);
        basisVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, base2X, base2Y, red, green, blue, alpha);
    }

    private static void radialVertex(BufferBuilder buffer, Matrix4f matrix,
                                     float cx, float cy, float cz,
                                     float rightX, float rightY, float rightZ,
                                     float upX, float upY, float upZ,
                                     float radius, float cosine, float sine,
                                     float red, float green, float blue, float alpha)
    {
        float horizontal = radius * cosine;
        float vertical = radius * sine;
        basisVertex(buffer, matrix, cx, cy, cz, rightX, rightY, rightZ,
                upX, upY, upZ, horizontal, vertical, red, green, blue, alpha);
    }

    private static void basisVertex(BufferBuilder buffer, Matrix4f matrix,
                                    float cx, float cy, float cz,
                                    float rightX, float rightY, float rightZ,
                                    float upX, float upY, float upZ,
                                    float horizontal, float vertical,
                                    float red, float green, float blue, float alpha)
    {
        pointVertex(buffer, matrix,
                cx + rightX * horizontal + upX * vertical,
                cy + rightY * horizontal + upY * vertical,
                cz + rightZ * horizontal + upZ * vertical,
                red, green, blue, alpha);
    }

    private static void pointVertex(BufferBuilder buffer, Matrix4f matrix,
                                    float x, float y, float z,
                                    float red, float green, float blue, float alpha)
    {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
    }
}
