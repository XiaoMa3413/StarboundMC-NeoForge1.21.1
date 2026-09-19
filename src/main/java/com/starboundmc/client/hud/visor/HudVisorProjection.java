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
 * Renders one compact HUD component into its own texture, then applies shared low-intensity edge
 * optics at component level. A single quad preserves glyph geometry instead of bending every
 * two-pixel cell inside the text.
 */
public final class HudVisorProjection implements AutoCloseable {
    private static final float GLOW_SPREAD = .45F;
    private static final float GLOW_ALPHA = .10F;

    private final int contentWidth;
    private final int contentHeight;
    private TextureTarget target;

    public HudVisorProjection(int contentWidth, int contentHeight) {
        if (contentWidth <= 0 || contentHeight <= 0)
            throw new IllegalArgumentException("Visor targets must have positive dimensions");
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
    }

    public int contentWidth() { return contentWidth; }
    public int contentHeight() { return contentHeight; }

    public void draw(GuiGraphics destination, float x, float y, float displayWidth,
                     float displayHeight, Consumer<GuiGraphics> content) {
        draw(destination, x, y, displayWidth, displayHeight, 1F, content);
    }

    public void draw(GuiGraphics destination, float x, float y, float displayWidth,
                     float displayHeight, float opacity, Consumer<GuiGraphics> content) {
        if (opacity <= .001F || displayWidth <= 0 || displayHeight <= 0)
            return;

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
                ensureTarget(scale);
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);
                modelView.identity();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, contentWidth,
                        contentHeight, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                var canvas = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
                content.accept(canvas);
                canvas.flush();
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
            RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(LDLibShaders::getGuiTexture);
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
            RenderSystem.setShaderColor(1, 1, 1, 1);
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            var pose = destination.pose().last().pose();
            quad(buffer, pose, destination, x - GLOW_SPREAD, y - GLOW_SPREAD,
                    displayWidth + GLOW_SPREAD * 2, displayHeight + GLOW_SPREAD * 2,
                    opacity * GLOW_ALPHA);
            quad(buffer, pose, destination, x, y, displayWidth, displayHeight, opacity);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
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

    private void quad(BufferBuilder buffer, Matrix4f pose, GuiGraphics destination,
                      float x, float y, float displayWidth, float displayHeight, float opacity) {
        vertex(buffer, pose, destination, x, y, displayWidth, displayHeight, 0, 0, opacity);
        vertex(buffer, pose, destination, x, y, displayWidth, displayHeight,
                0, contentHeight, opacity);
        vertex(buffer, pose, destination, x, y, displayWidth, displayHeight,
                contentWidth, contentHeight, opacity);
        vertex(buffer, pose, destination, x, y, displayWidth, displayHeight,
                contentWidth, 0, opacity);
    }

    private void ensureTarget(int scale) {
        int width = contentWidth * scale;
        int height = contentHeight * scale;
        if (target == null) {
            target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
            target.setFilterMode(GL11.GL_LINEAR);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
        }
    }

    private void vertex(BufferBuilder buffer, Matrix4f pose, GuiGraphics destination,
                        float x, float y, float displayWidth, float displayHeight,
                        float u, float v, float opacity) {
        float guiX = x + u * displayWidth / contentWidth;
        float guiY = y + v * displayHeight / contentHeight;
        var point = HudVisorSurface.projectGui(guiX, guiY,
                destination.guiWidth(), destination.guiHeight());
        float fade = HudVisorSurface.edgeFadeGui(guiX, guiY,
                destination.guiWidth(), destination.guiHeight()) * Math.clamp(opacity, 0F, 1F);
        buffer.addVertex(pose, point.x(), point.y(), 0)
                .setUv(u / contentWidth, 1F - v / contentHeight)
                .setColor(fade, fade, fade, fade);
    }

    @Override
    public void close() {
        if (target == null)
            return;
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try {
            target.destroyBuffers();
            target = null;
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
        }
    }
}
