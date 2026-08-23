package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starboundmc.client.space.StellarLod;
import com.starboundmc.world.starmap.StellarVisualProfile;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

/** Shared, allocation-light stellar sky renderer used in space and on worlds. */
public final class StellarRenderer
{
    /** Safely inside the default 768-unit far plane, including the largest corona. */
    public static final float SHIP_SKY_DISTANCE = 320.0F;

    private static final int DISC_SEGMENTS = 64;
    private static VertexBuffer discBuffer;
    private static VertexBuffer glowBuffer;
    private static VertexBuffer rayBuffer;
    private static VertexBuffer radiationBuffer;
    private static VertexBuffer particleBuffer;

    private StellarRenderer()
    {
    }

    /**
     * Renders one profile in a direction relative to the active sky pose. The
     * virtual distance never reaches the projection matrix: only direction is
     * retained and the configured apparent radius is placed on a safe shell.
     */
    public static void render(PoseStack pose, StellarVisualProfile profile, Vec3 direction,
                              float shellDistance, float alpha, float animationTicks,
                              float apparentScale)
    {
        if (direction == null)
            return;
        render(pose, profile, direction.x, direction.y, direction.z, shellDistance,
                alpha, animationTicks, apparentScale, 1.0F, 1.0F, StellarLod.FULL);
    }

    public static void render(PoseStack pose, StellarVisualProfile profile,
                              double directionX, double directionY, double directionZ,
                              float shellDistance, float alpha, float animationTicks,
                              float apparentScale, float coronaDetail, float effectDetail,
                              StellarLod lod)
    {
        lod = lod == null ? StellarLod.FULL : lod;
        // Ship-space points must go through StellarPointBatchRenderer. Keeping
        // a second legacy point path here would reintroduce mismatched size and
        // brightness whenever a future caller bypasses the batch.
        if (lod == StellarLod.POINT)
            return;
        if (profile == null || alpha <= 0.002F
                || profile.getApparentRadius() * apparentScale < 0.15F)
            return;

        double lengthSqr = directionX * directionX + directionY * directionY + directionZ * directionZ;
        if (lengthSqr < 1.0E-8)
            return;
        double invLength = 1.0 / Math.sqrt(lengthSqr);
        float dx = (float) (directionX * invLength);
        float dy = (float) (directionY * invLength);
        float dz = (float) (directionZ * invLength);
        float cx = dx * shellDistance;
        float cy = dy * shellDistance;
        float cz = dz * shellDistance;

        ensureBuffers();
        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float radiation = profile.getRadiationStrength();
        coronaDetail = Math.max(0.0F, Math.min(1.0F, coronaDetail));
        effectDetail = Math.max(0.0F, Math.min(1.0F, effectDetail));
        float pulsePhase = animationTicks * profile.getPulseSpeed();
        float pulse = 1.0F + 0.025F * (float) Math.sin(pulsePhase);
        if (radiation > 0.0F)
            pulse += radiation * 0.035F * (float) Math.sin(pulsePhase * 2.37F + 1.4F);
        float radius = profile.getApparentRadius() * apparentScale * pulse;
        float nearField = Math.max(0.0F, Math.min(1.0F, (radius - 28.0F) / 44.0F));

        // Keep the simplified disc at least as wide as the batch point core so
        // the point/simplified crossfade never looks like a temporary shrink.
        if (lod == StellarLod.SIMPLIFIED)
            radius = Math.max(radius, 1.45F);
        if (lod == StellarLod.SIMPLIFIED)
        {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            drawLayer(glowBuffer, pose, cx, cy, cz, dx, dy, dz,
                    radius * 1.65F, 0.0F, profile.getCoronaColor(),
                    alpha * (0.12F + coronaDetail * 0.18F));
            RenderSystem.defaultBlendFunc();
            drawLayer(discBuffer, pose, cx, cy, cz, dx, dy, dz,
                    radius, 0.0F, profile.getSurfaceColor(), alpha);
            restoreRenderState();
            return;
        }

        // Additive energy layers are behind the opaque photosphere.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        if (radiation > 0.0F && effectDetail > 0.08F)
        {
            float phaseA = fractional(animationTicks * 0.0065F);
            float phaseB = fractional(phaseA + 0.5F);
            drawLayer(radiationBuffer, pose, cx, cy, cz, dx, dy, dz,
                    radius * (1.65F + phaseA * 2.25F), animationTicks * 0.0022F,
                    profile.getCoronaColor(), alpha * radiation * effectDetail * (1.0F - phaseA) * 0.34F);
            drawLayer(radiationBuffer, pose, cx, cy, cz, dx, dy, dz,
                    radius * (1.65F + phaseB * 2.25F), -animationTicks * 0.0017F,
                    profile.getCoronaColor(), alpha * radiation * effectDetail * (1.0F - phaseB) * 0.25F);
            drawLayer(particleBuffer, pose, cx, cy, cz, dx, dy, dz,
                    radius * 2.85F, -animationTicks * 0.0031F,
                    profile.getCoronaColor(), alpha * radiation * effectDetail * 0.42F);
        }

        float flare = profile.getFlareStrength();
        if (flare > 0.0F && effectDetail > 0.08F)
        {
            drawLayer(rayBuffer, pose, cx, cy, cz, dx, dy, dz,
                    radius * (2.30F + flare * 0.75F), animationTicks * 0.0018F,
                    profile.getCoronaColor(), alpha * flare * effectDetail * 0.62F);
            drawLayer(rayBuffer, pose, cx, cy, cz, dx, dy, dz,
                    radius * (1.85F + flare * 0.50F), -animationTicks * 0.0026F,
                    profile.getCoreColor(), alpha * flare * effectDetail * 0.25F);
        }

        drawLayer(glowBuffer, pose, cx, cy, cz, dx, dy, dz,
                radius * profile.getGlowScale(), 0.0F,
                profile.getCoronaColor(), alpha * (0.035F + coronaDetail * 0.235F
                        + radiation * effectDetail * 0.10F + nearField * coronaDetail * 0.10F));
        drawLayer(glowBuffer, pose, cx, cy, cz, dx, dy, dz,
                radius * 1.48F, 0.0F,
                profile.getSurfaceColor(), alpha * (0.08F + coronaDetail * 0.28F));

        // The surface uses normal alpha blending so background stars do not
        // shine through its solid disc. A small additive core prevents flatness.
        RenderSystem.defaultBlendFunc();
        drawLayer(discBuffer, pose, cx, cy, cz, dx, dy, dz,
                radius, 0.0F, profile.getSurfaceColor(), alpha);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        drawLayer(glowBuffer, pose, cx, cy, cz, dx, dy, dz,
                radius * 0.72F, 0.0F, profile.getCoreColor(), alpha * 0.52F);

        restoreRenderState();
    }

    private static void restoreRenderState()
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static float fractional(float value)
    {
        return value - (float) Math.floor(value);
    }

    private static void drawLayer(VertexBuffer buffer, PoseStack pose,
                                  float cx, float cy, float cz,
                                  float dx, float dy, float dz,
                                  float scale, float rotation,
                                  int color, float alpha)
    {
        if (alpha <= 0.001F || scale <= 0.001F)
            return;

        float yaw = (float) Math.atan2(dx, dz);
        float pitch = -(float) Math.asin(Math.max(-1.0F, Math.min(1.0F, dy)));
        Matrix4f model = new Matrix4f(RenderSystem.getModelViewMatrix())
                .mul(pose.last().pose())
                .translate(cx, cy, cz)
                .rotateY(yaw)
                .rotateX(pitch)
                .rotateZ(rotation)
                .scale(scale);

        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, alpha);
        buffer.bind();
        buffer.drawWithShader(model, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
    }

    private static void ensureBuffers()
    {
        if (discBuffer == null || discBuffer.isInvalid())
            discBuffer = upload(StellarRenderer::buildDisc);
        if (glowBuffer == null || glowBuffer.isInvalid())
            glowBuffer = upload(StellarRenderer::buildGlow);
        if (rayBuffer == null || rayBuffer.isInvalid())
            rayBuffer = upload(StellarRenderer::buildRays);
        if (radiationBuffer == null || radiationBuffer.isInvalid())
            radiationBuffer = upload(StellarRenderer::buildRadiationArcs);
        if (particleBuffer == null || particleBuffer.isInvalid())
            particleBuffer = upload(StellarRenderer::buildParticles);
    }

    private interface GeometryBuilder
    {
        void build(BufferBuilder builder);
    }

    private static VertexBuffer upload(GeometryBuilder geometry)
    {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        geometry.build(bb);
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(bb.end());
        VertexBuffer.unbind();
        return buffer;
    }

    private static void buildDisc(BufferBuilder bb)
    {
        for (int i = 0; i < DISC_SEGMENTS; i++)
        {
            double a0 = Math.PI * 2.0 * i / DISC_SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / DISC_SEGMENTS;
            vertex(bb, 0.0F, 0.0F, 0.0F, 1.0F);
            vertex(bb, (float) Math.cos(a0), (float) Math.sin(a0), 0.0F, 1.0F);
            vertex(bb, (float) Math.cos(a1), (float) Math.sin(a1), 0.0F, 1.0F);
        }
    }

    private static void buildGlow(BufferBuilder bb)
    {
        float[] radii = { 0.0F, 0.20F, 0.48F, 0.76F, 1.0F };
        float[] alpha = { 1.0F, 0.78F, 0.36F, 0.10F, 0.0F };
        for (int ring = 0; ring < radii.length - 1; ring++)
            addRing(bb, radii[ring], radii[ring + 1], alpha[ring], alpha[ring + 1], false);
    }

    private static void buildRays(BufferBuilder bb)
    {
        for (int i = 0; i < 16; i++)
        {
            double angle = Math.PI * 2.0 * i / 16.0;
            double halfWidth = 0.022 + (i % 3) * 0.006;
            float inner = 0.38F;
            float outer = 0.74F + (i % 5) * 0.065F;
            float ix0 = inner * (float) Math.cos(angle - halfWidth);
            float iy0 = inner * (float) Math.sin(angle - halfWidth);
            float ix1 = inner * (float) Math.cos(angle + halfWidth);
            float iy1 = inner * (float) Math.sin(angle + halfWidth);
            float ox = outer * (float) Math.cos(angle);
            float oy = outer * (float) Math.sin(angle);
            vertex(bb, ix0, iy0, 0.0F, 0.42F);
            vertex(bb, ox, oy, 0.0F, 0.02F);
            vertex(bb, ix1, iy1, 0.0F, 0.42F);
        }
    }

    private static void buildRadiationArcs(BufferBuilder bb)
    {
        addRing(bb, 0.78F, 0.89F, 0.0F, 0.72F, true);
        addRing(bb, 0.89F, 1.0F, 0.72F, 0.0F, true);
    }

    private static void addRing(BufferBuilder bb, float inner, float outer,
                                float innerAlpha, float outerAlpha, boolean gaps)
    {
        for (int i = 0; i < DISC_SEGMENTS; i++)
        {
            if (gaps && ((i + 2) % 13 < 3 || (i + 7) % 29 < 2))
                continue;
            double a0 = Math.PI * 2.0 * i / DISC_SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / DISC_SEGMENTS;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            vertex(bb, c0 * inner, s0 * inner, 0.0F, innerAlpha);
            vertex(bb, c1 * inner, s1 * inner, 0.0F, innerAlpha);
            vertex(bb, c1 * outer, s1 * outer, 0.0F, outerAlpha);
            vertex(bb, c0 * inner, s0 * inner, 0.0F, innerAlpha);
            vertex(bb, c1 * outer, s1 * outer, 0.0F, outerAlpha);
            vertex(bb, c0 * outer, s0 * outer, 0.0F, outerAlpha);
        }
    }

    private static void buildParticles(BufferBuilder bb)
    {
        Random random = new Random(0x5A17B0A4L);
        for (int i = 0; i < 26; i++)
        {
            double angle = random.nextDouble() * Math.PI * 2.0;
            float radius = 0.48F + random.nextFloat() * 0.47F;
            float x = radius * (float) Math.cos(angle);
            float y = radius * (float) Math.sin(angle);
            float size = 0.012F + random.nextFloat() * 0.025F;
            float a = 0.25F + random.nextFloat() * 0.55F;
            vertex(bb, x, y + size, 0.0F, a);
            vertex(bb, x + size, y, 0.0F, a * 0.35F);
            vertex(bb, x, y - size, 0.0F, a);
            vertex(bb, x, y + size, 0.0F, a);
            vertex(bb, x, y - size, 0.0F, a);
            vertex(bb, x - size, y, 0.0F, a * 0.35F);
        }
    }

    private static void vertex(BufferBuilder bb, float x, float y, float z, float alpha)
    {
        bb.vertex(x, y, z).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
    }
}
