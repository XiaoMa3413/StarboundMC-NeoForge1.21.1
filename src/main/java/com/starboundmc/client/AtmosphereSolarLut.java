package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.starboundmc.client.space.AtmosphereSolarOptics;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

/** One 256 KiB optical-depth lookup shared by every atmosphere species. */
final class AtmosphereSolarLut {
    private static float[] columns;
    private static int texture;
    private static long uploads;
    private AtmosphereSolarLut() {}

    static void bind(ShaderInstance shader) {
        if (texture == 0) {
            if (columns == null) columns = AtmosphereSolarOptics.generate();
            var data = MemoryUtil.memAllocFloat(columns.length);
            int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            try {
                data.put(columns).flip();
                texture = GlStateManager._genTexture();
                GlStateManager._bindTexture(texture);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RG32F,AtmosphereSolarOptics.WIDTH,
                        AtmosphereSolarOptics.HEIGHT,0,GL30.GL_RG,GL11.GL_FLOAT,data);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
                uploads++;
            } catch (RuntimeException failure) {
                release(); throw failure;
            } finally {
                MemoryUtil.memFree(data);
                GlStateManager._bindTexture(previous);
            }
        }
        shader.setSampler("SolarOpticalDepth",texture);
    }

    static void release() {
        if (texture != 0) GlStateManager._deleteTexture(texture);
        texture = 0;
    }

    static String diagnostics() { return "solarLut=" + (texture != 0) + " solarLutUploads=" + uploads; }
}
