package com.starboundmc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.shaders.FogShape;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

/** Restores the common opaque state expected between the space render passes. */
final class SpaceRenderPassState
{
    private SpaceRenderPassState()
    {
    }

    static void restoreDefaults()
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Captured once at the event boundary; internal passes retain their inexpensive defaults. */
    static Snapshot capture() { return new Snapshot(); }

    static final class Snapshot {
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final boolean srgb = GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB);
        private final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int rgbEquation = GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_RGB);
        private final int alphaEquation = GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_ALPHA);
        private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int program = GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);
        private final int vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        private final int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        // Surface and the optional query background each use at most five units.
        private final int[] boundTextures = new int[5];
        private final int[] viewport = new int[4], scissorBox = new int[4], colorMask = new int[4];
        private final float[] clearColor = new float[4], shaderColor = RenderSystem.getShaderColor().clone();
        private final double clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        private final float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
        private final FogShape fogShape = RenderSystem.getShaderFogShape();
        private final float[] fogColor = RenderSystem.getShaderFogColor().clone();
        private final ShaderInstance shader = RenderSystem.getShader();
        private final int[] textures = {RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1),
                RenderSystem.getShaderTexture(2)};

        private Snapshot() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
            GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorMask);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
            for (int i = 0; i < boundTextures.length; i++) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
                boundTextures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            GlStateManager._activeTexture(activeTexture);
        }

        void restore() {
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(depthWrite); RenderSystem.depthFunc(depthFunc);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            org.lwjgl.opengl.GL20.glBlendEquationSeparate(rgbEquation, alphaEquation);
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            if (!scissor) RenderSystem.disableScissor();
            if (srgb) GL11.glEnable(GL30.GL_FRAMEBUFFER_SRGB); else GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            RenderSystem.colorMask(colorMask[0] != 0, colorMask[1] != 0, colorMask[2] != 0, colorMask[3] != 0);
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            RenderSystem.clearDepth(clearDepth);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
            RenderSystem.setShaderFogStart(fogStart); RenderSystem.setShaderFogEnd(fogEnd);
            RenderSystem.setShaderFogShape(fogShape);
            RenderSystem.setShaderFogColor(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            for (int i = 0; i < textures.length; i++) RenderSystem.setShaderTexture(i, textures[i]);
            for (int i = 0; i < boundTextures.length; i++) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
                GlStateManager._bindTexture(boundTextures[i]);
            }
            GlStateManager._activeTexture(activeTexture);
            GlStateManager._glUseProgram(program);
            GlStateManager._glBindVertexArray(vertexArray);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        }
    }
}
