package com.starboundmc.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Own astronomical depth/color. Only resolved color is copied to the incoming world target. */
final class SpaceSceneTarget {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static TextureTarget target;
    private static TextureTarget externalStars;
    private static ShaderInstance composite;
    private static ShaderInstance externalCopy;
    private static VertexBuffer triangle;
    private static boolean failed, active, linear;
    private static float distanceScale = 1;
    private static int drawFramebuffer, readFramebuffer;
    private static final int[] viewport = new int[4];
    private static long allocations, composites;
    private static long externalCopies;

    private SpaceSceneTarget() {}

    static void register(RegisterShadersEvent event) {
        release();
        composite = externalCopy = null;
        failed = false;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "space_composite"),
                    DefaultVertexFormat.POSITION), shader -> composite = shader);
        } catch (java.io.IOException | RuntimeException failure) {
            LOGGER.warn("Space composite unavailable; using direct pipeline", failure);
        }
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "space_external_stars"),
                    DefaultVertexFormat.POSITION), shader -> externalCopy = shader);
        } catch (java.io.IOException | RuntimeException failure) {
            LOGGER.warn("External star color adapter unavailable; using display-space compatibility", failure);
        }
    }

    static boolean begin() {
        active = false;
        if (failed || composite == null || PlanetSurfaceShader.current() == null
                || !SpaceStellarRenderer.ready() || !SpaceRingShader.ready()
                || StarfieldClientConfig.SPACE_PIPELINE_MODE.get() == StarfieldClientConfig.PipelineMode.DIRECT)
            return false;
        drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        // The external backend renders into a display-space target and is decoded on import.
        linear = StarfieldClientConfig.SPACE_BACKGROUND_MODE.get() == StarfieldClientConfig.BackgroundMode.PROCEDURAL
                && GpuSpaceBackground.ready()
                && (externalCopy != null || !externalRequested());
        try {
            if (target == null || target.width != viewport[2] || target.height != viewport[3]) {
                if (target != null) target.destroyBuffers();
                target = new TextureTarget(viewport[2], viewport[3], true, false);
                GlStateManager._bindTexture(target.getColorTextureId());
                GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, target.width, target.height,
                        0, GL11.GL_RGBA, GL11.GL_FLOAT, null);
                GlStateManager._bindTexture(target.getDepthTextureId());
                GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F,
                        target.width, target.height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null);
                GlStateManager._bindTexture(0);
                target.bindWrite(true);
                target.checkStatus();
                target.setClearColor(0, 0, 0, 1);
                allocations++;
            }
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            target.clear(false);
            target.bindWrite(true);
            active = true;
            distanceScale = 1;
            return true;
        } catch (RuntimeException failure) {
            failed = true;
            if (target != null) target.destroyBuffers();
            target = null;
            LOGGER.warn("Space target unavailable; using direct pipeline until resource reload", failure);
            restoreDestination();
            return false;
        }
    }

    static boolean active() { return active; }
    static boolean linear() { return active && linear; }
    static void distanceScale(float scale) { distanceScale = scale; }

    static void configure(ShaderInstance shader) {
        shader.safeGetUniform("DistanceScale").set(active ? distanceScale : 0);
        shader.safeGetUniform("LinearColor").set(linear() ? 1F : 0F);
    }

    private static boolean externalRequested() {
        return StarfieldClientConfig.stellarViewStarsEnabled() && ModList.get().isLoaded("stellarview");
    }

    static boolean beginExternalStars() {
        if (!linear() || externalCopy == null || !externalRequested()) return false;
        if (externalStars == null || externalStars.width != target.width || externalStars.height != target.height) {
            if (externalStars != null) externalStars.destroyBuffers();
            externalStars = new TextureTarget(target.width, target.height, false, false);
            externalStars.setClearColor(0, 0, 0, 0);
        }
        RenderSystem.disableScissor(); RenderSystem.colorMask(true, true, true, true);
        externalStars.clear(false);
        externalStars.bindWrite(true);
        return true;
    }

    static void finishExternalStars() {
        target.bindWrite(true);
        drawFullscreen(externalCopy, externalStars.getColorTextureId(), true);
        externalCopies++;
    }

    private static void drawFullscreen(ShaderInstance shader, int texture, boolean additive) {
        if (triangle == null || triangle.isInvalid()) {
            if (triangle != null) triangle.close();
            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                    DefaultVertexFormat.POSITION);
            builder.addVertex(-1, -1, 0); builder.addVertex(3, -1, 0); builder.addVertex(-1, 3, 0);
            triangle = new VertexBuffer(VertexBuffer.Usage.STATIC);
            triangle.bind(); triangle.upload(builder.buildOrThrow());
        }
        ShaderInstance previous = RenderSystem.getShader();
        try {
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            shader.setSampler("Sampler0", texture);
            shader.apply();
            // Reapply the pass blend after ShaderInstance's cached BlendMode, which may
            // have been changed by the external renderer. The full-screen shader has no matrices.
            if (additive) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            } else RenderSystem.disableBlend();
            triangle.bind(); triangle.draw();
        } finally {
            shader.clear(); VertexBuffer.unbind(); RenderSystem.setShader(() -> previous);
            SpaceRenderPassState.restoreDefaults();
        }
    }

    static void finish() {
        if (!active) return;
        try {
            restoreDestination();
            composite.safeGetUniform("LinearColor").set(linear ? 1F : 0F);
            composite.safeGetUniform("Exposure").set(StarfieldClientConfig.SPACE_EXPOSURE.get().floatValue());
            drawFullscreen(composite, target.getColorTextureId(), false);
            composites++;
        } finally {
            active = false;
            restoreDestination();
        }
    }

    private static void restoreDestination() {
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    static void release() {
        active = false;
        if (target != null) target.destroyBuffers();
        target = null;
        if (externalStars != null) externalStars.destroyBuffers();
        externalStars = null;
        if (triangle != null) triangle.close();
        triangle = null;
    }

    static void disableAfterFailure(RuntimeException failure) {
        active = false;
        failed = true;
        LOGGER.warn("Isolated space pipeline failed; using direct pipeline until resource reload", failure);
    }

    static void abort() { active = false; }

    static String diagnostics() {
        return "sceneTarget=" + (target != null) + " linearHDR=" + linear + " targetFailed=" + failed
                + " targetAllocations=" + allocations + " composites=" + composites
                + " externalCopies=" + externalCopies;
    }
}
