package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.starboundmc.client.space.AtmosphereSolarOptics;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

/** One 256 KiB optical-depth lookup shared by every atmosphere species. */
final class AtmosphereSolarLut {
    private static float[] columns;
    private static int texture;
    private static long uploads;
    private AtmosphereSolarLut() {}

    static void bind(ShaderInstance shader) {
        RenderSystem.assertOnRenderThread();
        if (texture == 0) {
            if (columns == null) columns = AtmosphereSolarOptics.generate();
            var data = MemoryUtil.memAllocFloat(columns.length);
            int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            UnpackState unpack = UnpackState.capture();
            int pendingTexture = 0;
            try {
                data.put(columns).flip();
                pendingTexture = GlStateManager._genTexture();
                GlStateManager._bindTexture(pendingTexture);
                // NativeImage leaves atlas row length and subimage offsets behind. Without
                // resetting them, the driver can read beyond this tightly packed CPU buffer.
                GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
                GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, Float.BYTES);
                GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
                GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
                GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
                GlStateManager._pixelStore(GL11.GL_UNPACK_SWAP_BYTES, GL11.GL_FALSE);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RG32F,AtmosphereSolarOptics.WIDTH,
                        AtmosphereSolarOptics.HEIGHT,0,GL30.GL_RG,GL11.GL_FLOAT,data);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
                texture = pendingTexture;
                uploads++;
            } finally {
                if (texture == 0 && pendingTexture != 0) GlStateManager._deleteTexture(pendingTexture);
                MemoryUtil.memFree(data);
                unpack.restore();
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

    /** Queried only on creation/reload, never per atmosphere draw. */
    private record UnpackState(int alignment, int rowLength, int skipPixels, int skipRows,
                               int swapBytes, int buffer) {
        static UnpackState capture() {
            return new UnpackState(GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT),
                    GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH),
                    GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS),
                    GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS),
                    GL11.glGetInteger(GL11.GL_UNPACK_SWAP_BYTES),
                    GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING));
        }

        void restore() {
            GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, alignment);
            GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SWAP_BYTES, swapBytes);
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, buffer);
        }
    }
}
