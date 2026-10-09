package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.BackgroundStarCatalog;
import com.starboundmc.client.space.StarPhotometry;
import com.starboundmc.client.space.StarfieldOptics;
import com.starboundmc.client.space.GalaxyEnvironmentBlend;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/** Fullscreen direction field and static expanded star quads; GLSL 150, no extra dependency. */
final class GpuSpaceBackground {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Integer, VertexBuffer> STAR_BUFFERS = new HashMap<>();
    private static ShaderInstance backgroundShader, starShader;
    private static VertexBuffer triangle;
    private static boolean backgroundFailed, starsFailed;
    private static long uploads, backgroundDraws, starDraws;
    private static float opticalZoom = 1;

    private GpuSpaceBackground() {}

    static boolean ready() {
        return backgroundShader != null && starShader != null && !backgroundFailed && !starsFailed;
    }

    static ShaderInstance starShader() { return starShader; }

    static void register(RegisterShadersEvent event) {
        releaseGeometry();
        backgroundShader = starShader = null;
        backgroundFailed = starsFailed = false;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), id("space_background"),
                    DefaultVertexFormat.POSITION), shader -> backgroundShader = shader);
        } catch (java.io.IOException | RuntimeException failure) {
            LOGGER.warn("Space direction background unavailable; using minimal background", failure);
        }
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), id("background_stars"),
                    DefaultVertexFormat.POSITION_TEX_COLOR), shader -> starShader = shader);
        } catch (java.io.IOException | RuntimeException failure) {
            LOGGER.warn("GPU background stars unavailable; using CPU stars", failure);
        }
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, name);
    }

    static boolean renderBackground(Matrix4f skyModelView, Matrix4f projection,
                                    GalaxyEnvironmentBlend environment) {
        if (backgroundShader == null || backgroundFailed) return false;
        ShaderInstance previous = RenderSystem.getShader();
        try {
            if (triangle == null || triangle.isInvalid()) {
                if (triangle != null) triangle.close();
                BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                        DefaultVertexFormat.POSITION);
                builder.addVertex(-1, -1, 0);
                builder.addVertex(3, -1, 0);
                builder.addVertex(-1, 3, 0);
                triangle = upload(builder);
            }
            FogRenderer.setupNoFog();
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
            RenderSystem.disableCull(); RenderSystem.disableBlend();
            RenderSystem.setShader(() -> backgroundShader);
            backgroundShader.safeGetUniform("InverseViewProjection").set(
                    new Matrix4f(projection).mul(skyModelView).invert());
            setTint(backgroundShader, environment.skyTintColor(), environment.skyTintAmount());
            backgroundShader.safeGetUniform("LinearColor").set(SpaceSceneTarget.linear() ? 1F : 0F);
            backgroundShader.safeGetUniform("FieldDetail").set(
                    StarfieldClientConfig.SPACE_VISUAL_QUALITY.get() == SpaceVisualQuality.PERFORMANCE ? 0F : 1F);
            triangle.bind();
            triangle.drawWithShader(new Matrix4f(), new Matrix4f(), backgroundShader);
            backgroundDraws++;
            return true;
        } catch (RuntimeException failure) {
            backgroundFailed = true;
            LOGGER.warn("Space direction background failed; using minimal background until resource reload", failure);
            return false;
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShader(() -> previous);
            SpaceRenderPassState.restoreDefaults();
        }
    }

    static boolean renderStars(Matrix4f modelView, Matrix4f projection, Vector3f convergenceAxis,
                               float alpha, float convergence, int tint, float tintAmount, float phase) {
        if (starShader == null || starsFailed) return false;
        ShaderInstance previous = RenderSystem.getShader();
        try {
            int count = StarfieldClientConfig.SPACE_VISUAL_QUALITY.get().backgroundStarBudget();
            VertexBuffer stars = STAR_BUFFERS.get(count);
            if (stars == null || stars.isInvalid()) {
                if (stars != null) stars.close();
                BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_TEX_COLOR);
                for (var star : BackgroundStarCatalog.generate(count))
                    for (int corner = 0; corner < 4; corner++)
                        builder.addVertex(star.x(), star.y(), star.z()).setUv(star.sigmaPixels(), star.flux())
                                .setColor(StarPhotometry.display(star.red()),StarPhotometry.display(star.green()),
                                        StarPhotometry.display(star.blue()),star.dustFraction());
                stars = upload(builder);
                STAR_BUFFERS.put(count, stars);
            }
            FogRenderer.setupNoFog();
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
            RenderSystem.disableCull(); RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(() -> starShader);
            starShader.safeGetUniform("ConvergenceAxis").set(convergenceAxis.x, convergenceAxis.y, convergenceAxis.z);
            starShader.safeGetUniform("Convergence").set(convergence);
            starShader.safeGetUniform("StarAlpha").set(alpha);
            setTint(starShader, tint, tintAmount);
            starShader.safeGetUniform("LinearColor").set(SpaceSceneTarget.linear() ? 1F : 0F);
            starShader.safeGetUniform("FieldDetail").set(
                    StarfieldClientConfig.SPACE_VISUAL_QUALITY.get() == SpaceVisualQuality.PERFORMANCE ? 0F : 1F);
            starShader.safeGetUniform("ViewportSize").set((float) SpaceSceneTarget.width(), (float) SpaceSceneTarget.height());
            opticalZoom = StarfieldOptics.magnification(projection, Minecraft.getInstance().options.fov().get());
            starShader.safeGetUniform("OpticalZoom").set(opticalZoom);
            stars.bind();
            stars.drawWithShader(modelView, projection, starShader);
            starDraws++;
            return true;
        } catch (RuntimeException failure) {
            starsFailed = true;
            LOGGER.warn("GPU background stars failed; using minimal background until resource reload", failure);
            return false;
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShader(() -> previous);
            SpaceRenderPassState.restoreDefaults();
        }
    }

    private static void setTint(ShaderInstance shader, int tint, float amount) {
        shader.safeGetUniform("TintColor").set(((tint >> 16) & 255) / 255F,
                ((tint >> 8) & 255) / 255F, (tint & 255) / 255F);
        shader.safeGetUniform("TintAmount").set(amount);
    }

    private static VertexBuffer upload(BufferBuilder builder) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            buffer.bind(); buffer.upload(builder.buildOrThrow());
            uploads++;
            return buffer;
        } catch (RuntimeException failure) {
            buffer.close(); throw failure;
        } finally { VertexBuffer.unbind(); }
    }

    static void releaseGeometry() {
        if (triangle != null) triangle.close();
        triangle = null;
        STAR_BUFFERS.values().forEach(VertexBuffer::close);
        STAR_BUFFERS.clear();
    }

    static String diagnostics() {
        return "backgroundShader=" + (backgroundShader != null) + " starShader=" + (starShader != null)
                + " backgroundFailed=" + backgroundFailed + " starsFailed=" + starsFailed
                + " uploads=" + uploads + " backgroundDraws=" + backgroundDraws
                + " starDraws=" + starDraws + " cachedStarBudgets=" + STAR_BUFFERS.keySet()
                + " opticalZoom=" + opticalZoom;
    }
}
