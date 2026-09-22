// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.function.Consumer;

/**
 * Supersampled artwork projected onto its cached section of the screen-wide reference curve.
 * A lower-resolution halo is prepared with nine flat samples, then uses the same mesh as the core.
 */
public final class HudVisorProjection implements AutoCloseable {
    private static final float GLOW_RADIUS = .7F;
    private static final float GLOW_ALPHA = .08F;
    private static final int PADDING = 2;
    private static final int MESH_STEP = 2;
    private static final boolean GLOW_ENABLED = !Boolean.getBoolean("starboundmc.debug.hudVisorNoGlow");

    private final int contentWidth;
    private final int contentHeight;
    private final HudVisorGeometry.Profile profile;
    private final boolean centerAnchor;
    private TextureTarget target;
    private TextureTarget haloTarget;
    private VertexBuffer mesh;
    private HudVisorGeometry.Placement meshPlacement;

    public HudVisorProjection(int contentWidth, int contentHeight) {
        this(contentWidth, contentHeight, HudVisorGeometry.Profile.FLAT);
    }

    public HudVisorProjection(int contentWidth, int contentHeight, HudVisorGeometry.Profile profile) {
        if (contentWidth <= 0 || contentHeight <= 0)
            throw new IllegalArgumentException("Visor targets must have positive dimensions");
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        this.centerAnchor = profile == HudVisorGeometry.Profile.SURVIVAL;
        this.profile = HudVisorGeometry.comparison(profile,
                System.getProperty("starboundmc.debug.hudVisorProfile", "screen"));
    }

    public int contentWidth() { return contentWidth; }
    public int contentHeight() { return contentHeight; }
    public HudVisorGeometry.Profile profile() { return profile; }

    public void draw(GuiGraphics destination, float x, float y, float displayWidth,
                     float displayHeight, Consumer<GuiGraphics> content) {
        draw(destination, x, y, displayWidth, displayHeight, 1F, content);
    }

    public void draw(GuiGraphics destination, float x, float y, float displayWidth,
                     float displayHeight, float opacity, Consumer<GuiGraphics> content) {
        draw(destination, x, y, displayWidth, displayHeight, opacity, GLOW_ALPHA, content);
    }

    public void draw(GuiGraphics destination, float x, float y, float displayWidth,
                     float displayHeight, float opacity, float glowStrength, Consumer<GuiGraphics> content) {
        if (!Float.isFinite(opacity) || opacity <= .001F || displayWidth <= 0 || displayHeight <= 0)
            return;
        opacity = Math.clamp(opacity, 0F, 1F);

        var mc = Minecraft.getInstance();
        int scale = Math.clamp((int) Math.ceil(mc.getWindow().getGuiScale() * 2), 4, 12);
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var shader = RenderSystem.getShader();
        int sampler = RenderSystem.getShaderTexture(0);
        float[] color = RenderSystem.getShaderColor().clone();
        float[] clearColor = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        int[] colorMask = new int[4];
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorMask);
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        int equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        var modelView = RenderSystem.getModelViewStack();
        try {
            destination.flush();
            modelView.pushMatrix();
            try {
                RenderSystem.disableScissor();
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
                ensureTargets(scale);
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);
                modelView.identity();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-PADDING, contentWidth + PADDING,
                        contentHeight + PADDING, -PADDING, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                var canvas = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
                content.accept(canvas);
                HudVisorCalibrationGrid.render(canvas, contentWidth, contentHeight);
                canvas.flush();
                if (GLOW_ENABLED)
                    prepareHalo(displayWidth / contentWidth, displayHeight / contentHeight);
            } finally {
                modelView.popMatrix();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(projection, sorting);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
                RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
                RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
                RenderSystem.colorMask(colorMask[0] != 0, colorMask[1] != 0,
                        colorMask[2] != 0, colorMask[3] != 0);
                if (scissor) GlStateManager._enableScissorTest();
                else RenderSystem.disableScissor();
            }

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.setShader(LDLibShaders::getGuiTexture);
            float centerX = centerAnchor ? x : x + displayWidth / 2;
            float referenceY = centerAnchor ? y : y + displayHeight / 2;
            ensureMesh(new HudVisorGeometry.Placement(destination.guiWidth(), destination.guiHeight(),
                    centerX, displayWidth / contentWidth, displayHeight / contentHeight));
            var model = new Matrix4f(RenderSystem.getModelViewMatrix())
                    .mul(destination.pose().last().pose()).translate(centerX, referenceY, 0);
            if (GLOW_ENABLED) {
                float glow = opacity * (Float.isFinite(glowStrength)
                        ? Math.clamp(glowStrength, 0F, .16F) : GLOW_ALPHA);
                RenderSystem.setShaderColor(glow, glow, glow, glow);
                RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE,
                        GL11.GL_ZERO, GL11.GL_ONE);
                drawMesh(model, haloTarget);
            }
            // Premultiplied RGB and alpha must both follow the component's fade.
            // Uniform opacity preserves small fades that would quantize away in vertex colors.
            RenderSystem.setShaderColor(opacity, opacity, opacity, opacity);
            RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawMesh(model, target);
        } finally {
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, sampler);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GL20.glBlendEquationSeparate(equationRgb, equationAlpha);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.depthMask(depthMask);
        }
    }

    private void prepareHalo(float scaleX, float scaleY) {
        haloTarget.clear(Minecraft.ON_OSX);
        haloTarget.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
        RenderSystem.setShader(LDLibShaders::getGuiTexture);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int row = -1; row <= 1; row++) {
            for (int column = -1; column <= 1; column++) {
                // Approximate [1,2,1] x [1,2,1]; normalized exactly after 8-bit quantization.
                int weight = row == 0 && column == 0 ? 63 : row == 0 || column == 0 ? 32 : 16;
                float dx = column * GLOW_RADIUS / scaleX;
                float dy = row * GLOW_RADIUS / scaleY;
                sourceVertex(buffer, -PADDING, -PADDING, dx, dy, weight);
                sourceVertex(buffer, -PADDING, contentHeight + PADDING, dx, dy, weight);
                sourceVertex(buffer, contentWidth + PADDING, contentHeight + PADDING, dx, dy, weight);
                sourceVertex(buffer, contentWidth + PADDING, -PADDING, dx, dy, weight);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private void sourceVertex(BufferBuilder buffer, float u, float v, float dx, float dy, int weight) {
        buffer.addVertex(u + dx, v + dy, 0)
                .setUv((u + PADDING) / (contentWidth + 2F * PADDING),
                        1 - (v + PADDING) / (contentHeight + 2F * PADDING))
                .setColor(weight, weight, weight, weight);
    }

    private void drawMesh(Matrix4f model, TextureTarget texture) {
        RenderSystem.setShaderTexture(0, texture.getColorTextureId());
        mesh.bind();
        try {
            mesh.drawWithShader(model, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        } finally {
            VertexBuffer.unbind();
        }
    }

    private void ensureMesh(HudVisorGeometry.Placement placement) {
        if (mesh != null && !mesh.isInvalid() && placement.equals(meshPlacement)) return;
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean flat = profile == HudVisorGeometry.Profile.FLAT;
        int stepX = flat ? contentWidth + 2 * PADDING : MESH_STEP;
        int stepY = flat ? contentHeight + 2 * PADDING : MESH_STEP;
        for (int u = -PADDING; u < contentWidth + PADDING; u += stepX) {
            int right = Math.min(u + stepX, contentWidth + PADDING);
            for (int v = -PADDING; v < contentHeight + PADDING; v += stepY) {
                int bottom = Math.min(v + stepY, contentHeight + PADDING);
                meshVertex(buffer, u, v, placement);
                meshVertex(buffer, u, bottom, placement);
                meshVertex(buffer, right, bottom, placement);
                meshVertex(buffer, right, v, placement);
            }
        }
        if (mesh == null || mesh.isInvalid()) mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
        mesh.bind();
        try {
            mesh.upload(buffer.buildOrThrow());
            meshPlacement = placement;
        } finally {
            VertexBuffer.unbind();
        }
    }

    private void meshVertex(BufferBuilder buffer, float u, float v, HudVisorGeometry.Placement placement) {
        var point = HudVisorGeometry.project(profile, u, v, contentWidth, contentHeight, placement);
        float fade = HudVisorGeometry.opacity(profile, u);
        buffer.addVertex(point.x(), point.y(), 0)
                .setUv((u + PADDING) / (contentWidth + 2F * PADDING),
                        1 - (v + PADDING) / (contentHeight + 2F * PADDING))
                .setColor(fade, fade, fade, fade);
    }

    private void ensureTargets(int scale) {
        target = ensureTarget(target, scale);
        if (GLOW_ENABLED)
            haloTarget = ensureTarget(haloTarget, Math.max(2, scale / 2));
    }

    private TextureTarget ensureTarget(TextureTarget target, int scale) {
        int width = (contentWidth + 2 * PADDING) * scale;
        int height = (contentHeight + 2 * PADDING) * scale;
        if (target == null) {
            target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
            target.setFilterMode(GL11.GL_LINEAR);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
        }
        return target;
    }

    @Override
    public void close() {
        if (mesh != null) {
            mesh.close();
            mesh = null;
        }
        meshPlacement = null;
        if (target == null && haloTarget == null) return;
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try {
            if (target != null) target.destroyBuffers();
            target = null;
            if (haloTarget != null) haloTarget.destroyBuffers();
            haloTarget = null;
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
        }
    }
}
