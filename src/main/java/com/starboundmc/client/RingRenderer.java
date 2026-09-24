package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starboundmc.world.GasGiantGeometry;
import com.starboundmc.world.universe.BodySpaceVisualProfile;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Renders the existing gas-giant ring geometry and its far/near passes. */
public final class RingRenderer
{
    private static final int RING_SEGMENTS = 144;
    private static final float RING_INNER = GasGiantGeometry.RING_INNER_RADII;
    private static final float RING_OUTER = GasGiantGeometry.RING_OUTER_RADII;
    private static final float RING_ALPHA = 0.90F;
    private static final float RING_TINT_R = 1.00F;
    private static final float RING_TINT_G = 0.97F;
    private static final float RING_TINT_B = 0.92F;
    private static final float[] RING_VX = new float[RING_SEGMENTS * 4];
    private static final float[] RING_VY = new float[RING_SEGMENTS * 4];
    private static final float[] RING_VZ = new float[RING_SEGMENTS * 4];
    private static final float[] RING_VU = new float[RING_SEGMENTS * 4];
    private static final float[] RING_MID_X = new float[RING_SEGMENTS];
    private static final float[] RING_MID_Y = new float[RING_SEGMENTS];
    private static final float[] RING_MID_Z = new float[RING_SEGMENTS];

    static
    {
        buildRingBand();
    }

    private RingRenderer()
    {
    }

    static boolean hasRings(BodySpaceVisualProfile profile)
    {
        return profile != null && profile.hasRings();
    }

    private static void buildRingBand()
    {
        for (int seg = 0; seg < RING_SEGMENTS; seg++)
        {
            double a0 = Math.PI * 2.0 * seg / RING_SEGMENTS;
            double a1 = Math.PI * 2.0 * (seg + 1) / RING_SEGMENTS;
            float cos0 = (float) Math.cos(a0), sin0 = (float) Math.sin(a0);
            float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);

            // Corner order: inner@a0 (u0), outer@a0 (u1), outer@a1 (u1), inner@a1 (u0).
            for (int corner = 0; corner < 4; corner++)
            {
                float radius = (corner == 0 || corner == 3) ? RING_INNER : RING_OUTER;
                float angleCos = (corner <= 1) ? cos0 : cos1;
                float angleSin = (corner <= 1) ? sin0 : sin1;
                float u = (corner == 1 || corner == 2) ? 1.0F : 0.0F;
                int idx = seg * 4 + corner;
                RING_VX[idx] = angleCos * radius * PlanetRenderer.PLANET_RADIUS;
                RING_VY[idx] = 0.0F;
                RING_VZ[idx] = angleSin * radius * PlanetRenderer.PLANET_RADIUS;
                RING_VU[idx] = u;
            }
            RING_MID_X[seg] = 0.25F * (RING_VX[seg * 4] + RING_VX[seg * 4 + 1]
                    + RING_VX[seg * 4 + 2] + RING_VX[seg * 4 + 3]);
            RING_MID_Y[seg] = 0.0F;
            RING_MID_Z[seg] = 0.25F * (RING_VZ[seg * 4] + RING_VZ[seg * 4 + 1]
                    + RING_VZ[seg * 4 + 2] + RING_VZ[seg * 4 + 3]);
        }
    }

    static void drawPlanetRings(PoseStack pose, BodySpaceVisualProfile profile,
                                float cx, float cy, float cz,
                                float scale, float shipYaw, float shipPitch,
                                float alpha, boolean nearPass)
    {
        // Match the disk's orientation-minus-spin so ring and surface tilt
        // together: the baked ring is equatorial, so the body frame has to be
        // composed here, before the ship-view rotation.
        String ringTexture = profile.ringTexture().orElse(null);
        if (ringTexture == null)
            return;
        Matrix4f model = new Matrix4f()
                .translate(cx, cy, cz)
                .rotateX((float) Math.toRadians(-shipPitch))
                .rotateY((float) Math.toRadians(-shipYaw))
                .mul(bodyOrientation(profile))
                .scale(scale);
        drawRingPass(pose, model, ResourceLocation.parse(ringTexture), alpha, nearPass);
    }

    /** Draws one half of the baked band in the caller's body frame. */
    static void drawRingPass(PoseStack pose, Matrix4f model, ResourceLocation texture,
                             float alpha, boolean nearPass)
    {
        if (alpha <= 0.002F || texture == null)
            return;

        try
        {
            FogRenderer.setupNoFog();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * RING_ALPHA);

            Matrix4f full = new Matrix4f(pose.last().pose()).mul(model);
            Vector3f centre = full.transformPosition(new Vector3f());
            Vector3f centroid = new Vector3f();
            BufferBuilder bb = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int drawn = 0;
            for (int seg = 0; seg < RING_SEGMENTS; seg++)
            {
                full.transformPosition(RING_MID_X[seg], RING_MID_Y[seg], RING_MID_Z[seg], centroid);
                boolean near = centroid.lengthSquared() < centre.lengthSquared();
                if (near != nearPass)
                    continue;
                drawn++;
                for (int corner = 0; corner < 4; corner++)
                {
                    int idx = seg * 4 + corner;
                    bb.addVertex(full, RING_VX[idx], RING_VY[idx], RING_VZ[idx])
                            .setUv(RING_VU[idx], 0.5F)
                            .setColor(RING_TINT_R, RING_TINT_G, RING_TINT_B, 1.0F);
                }
            }
            if (drawn > 0)
                BufferUploader.drawWithShader(bb.buildOrThrow());
        }
        finally
        {
            SpaceRenderPassState.restoreDefaults();
        }
    }

    /** Shared body orientation, composed identically by the surface and ring. */
    static Matrix4f bodyOrientation(BodySpaceVisualProfile profile)
    {
        return new Matrix4f()
                .rotateX((float) Math.toRadians(profile.orientationTilt()))
                .rotateY((float) Math.toRadians(profile.orientationYaw()));
    }
}
