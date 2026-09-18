// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import java.util.function.Consumer;

/** Reused transparent render target, projected as one continuous tessellated surface. */
final class VisorHudProjection implements AutoCloseable {
    private TextureTarget target;
    private static final VisorSurface.Point[][] POINTS = new VisorSurface.Point[25][65];
    private static final float[] FADE = new float[65];
    static {
        for (int col = 0; col < 65; col++) {
            FADE[col] = VisorSurface.fade(col * 2);
            for (int row = 0; row < 25; row++) POINTS[row][col] = VisorSurface.project(col * 2, row * 2);
        }
    }

    void draw(GuiGraphics destination, float x, float y, Consumer<GuiGraphics> content) {
        var mc = Minecraft.getInstance();
        int scale = Math.clamp((int) Math.ceil(mc.getWindow().getGuiScale()), 2, 6);
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var shader = RenderSystem.getShader();
        int sampler = RenderSystem.getShaderTexture(0);
        float[] color = RenderSystem.getShaderColor().clone();
        float[] clearColor = new float[4]; GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        int[] colorMask = new int[4]; GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorMask);
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        int equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        var modelView = RenderSystem.getModelViewStack();
        try {
            // flush() itself changes depth and render-type state. Capture the caller's
            // state first, but never let its queued geometry enter our texture.
            destination.flush();
            modelView.pushMatrix();
            try {
                RenderSystem.disableScissor();
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
                if (target == null) {
                    target = new TextureTarget(VisorSurface.WIDTH * scale, VisorSurface.HEIGHT * scale, false, Minecraft.ON_OSX);
                    target.setClearColor(0, 0, 0, 0);
                    target.setFilterMode(GL11.GL_LINEAR);
                } else if (target.width != VisorSurface.WIDTH * scale) {
                    target.resize(VisorSurface.WIDTH * scale, VisorSurface.HEIGHT * scale, Minecraft.ON_OSX);
                    target.setFilterMode(GL11.GL_LINEAR);
                }
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);
                modelView.identity(); RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, VisorSurface.WIDTH,
                        VisorSurface.HEIGHT, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                var canvas = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
                content.accept(canvas);
                canvas.flush();
            } finally {
                modelView.popMatrix(); RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(projection, sorting);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
                RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
                RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
                RenderSystem.colorMask(colorMask[0] != 0, colorMask[1] != 0, colorMask[2] != 0, colorMask[3] != 0);
                if (scissor) GlStateManager._enableScissorTest();
                else RenderSystem.disableScissor();
            }
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
            RenderSystem.enableBlend();
            // GUI rendering into transparent black produces premultiplied color.
            // ONE avoids multiplying by alpha twice (dark fringes around glyphs).
            RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // Vanilla position_tex_color discards alpha < 0.1, clipping our faint
            // glow and the dim end of warning pulses after the edge fade.
            RenderSystem.setShader(LDLibShaders::getGuiTexture);
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
            RenderSystem.setShaderColor(1, 1, 1, 1);
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            var pose = destination.pose().last().pose();
            // Two-pixel cells deform inside individual glyphs and the warning triangle.
            for (int row = 0; row < VisorSurface.HEIGHT; row += 2) {
                for (int col = 0; col < VisorSurface.WIDTH; col += 2) {
                    vertex(buffer, pose, x, y, col, row);
                    vertex(buffer, pose, x, y, col, row + 2);
                    vertex(buffer, pose, x, y, col + 2, row + 2);
                    vertex(buffer, pose, x, y, col + 2, row);
                }
            }
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

    private static void vertex(BufferBuilder buffer, Matrix4f pose, float x, float y, float u, float v) {
        var point = POINTS[(int) v / 2][(int) u / 2];
        float fade = FADE[(int) u / 2];
        buffer.addVertex(pose, x + point.x(), y + point.y(), 0)
                .setUv(u / VisorSurface.WIDTH, 1 - v / VisorSurface.HEIGHT)
                .setColor(fade, fade, fade, fade);
    }

    @Override public void close() {
        if (target != null) {
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
}
