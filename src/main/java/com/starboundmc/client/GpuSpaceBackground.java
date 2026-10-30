package com.starboundmc.client;

import com.starboundmc.client.space.BackgroundStarRenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.GalaxyEnvironmentBlend;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.slf4j.Logger;

/** Fullscreen direction background; cinematic star geometry is owned by BackgroundStarRenderer. */
final class GpuSpaceBackground {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static ShaderInstance backgroundShader;
    private static VertexBuffer triangle;
    private static boolean backgroundFailed;
    private static long uploads, backgroundDraws;

    private GpuSpaceBackground() {}

    static boolean ready() {
        return backgroundShader != null && !backgroundFailed && BackgroundStarRenderer.ready();
    }

    static void register(RegisterShadersEvent event) {
        releaseGeometry();
        backgroundShader = null;
        backgroundFailed = false;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), id("space_background"),
                    DefaultVertexFormat.POSITION), shader -> backgroundShader = shader);
        } catch (java.io.IOException | RuntimeException failure) {
            LOGGER.warn("Space direction background unavailable; using minimal background", failure);
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
    }

    static String diagnostics() {
        return "backgroundShader=" + (backgroundShader != null) + " backgroundFailed=" + backgroundFailed
                + " backgroundUploads=" + uploads + " backgroundDraws=" + backgroundDraws
                + " " + BackgroundStarRenderer.diagnostics();
    }
}
