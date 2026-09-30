package com.starboundmc.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Small HDR pyramid. Upsampling adds only a different mip, never samples its destination. */
final class SpaceBloom {
    private static TextureTarget[] levels = new TextureTarget[0];
    private static ShaderInstance filter;
    private static boolean failed;
    private static long draws, allocations;

    private SpaceBloom() {}

    static void register(RegisterShadersEvent event) {
        release(); filter = null; failed = false;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "space_bloom"),
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION), shader -> filter = shader);
        } catch (java.io.IOException | RuntimeException exception) {
            LogUtils.getLogger().warn("Space bloom unavailable; retaining HDR without bloom", exception);
        }
    }

    static int render(TextureTarget source) {
        SpaceVisualQuality quality = StarfieldClientConfig.SPACE_VISUAL_QUALITY.get();
        int count = quality.bloomLevels();
        if (count == 0 || StarfieldClientConfig.SPACE_BLOOM_STRENGTH.get() == 0) {
            release(); return 0;
        }
        if (filter == null || failed)
            return 0;
        try {
            int width = Math.max(1, source.width / quality.bloomDownsample());
            int height = Math.max(1, source.height / quality.bloomDownsample());
            if (levels.length != count || levels[0].width != width || levels[0].height != height) {
                release();
                levels = new TextureTarget[count];
                for (int i = 0; i < count; i++) {
                    levels[i] = new TextureTarget(Math.max(1, width >> i), Math.max(1, height >> i), false, false);
                    GlStateManager._bindTexture(levels[i].getColorTextureId());
                    GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F,
                            levels[i].width, levels[i].height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, null);
                    levels[i].setFilterMode(GL11.GL_LINEAR);
                    levels[i].checkStatus();
                    allocations++;
                }
                GlStateManager._bindTexture(0);
            }
            TextureTarget input = source;
            for (int i = 0; i < count; i++) {
                filter.safeGetUniform("Prefilter").set(i == 0 ? 1F : 0F);
                filter.safeGetUniform("Gain").set(1F);
                filter.safeGetUniform("TexelSize").set(1F / input.width, 1F / input.height);
                levels[i].bindWrite(true);
                SpaceSceneTarget.drawFullscreen(filter, input.getColorTextureId(), false);
                input = levels[i]; draws++;
            }
            for (int i = count - 2; i >= 0; i--) {
                input = levels[i + 1];
                filter.safeGetUniform("Prefilter").set(0F);
                filter.safeGetUniform("Gain").set(.65F);
                filter.safeGetUniform("TexelSize").set(1F / input.width, 1F / input.height);
                levels[i].bindWrite(true);
                SpaceSceneTarget.drawFullscreen(filter, input.getColorTextureId(), true);
                draws++;
            }
            return levels[0].getColorTextureId();
        } catch (RuntimeException exception) {
            failed = true; release();
            LogUtils.getLogger().warn("Space bloom failed; retaining HDR without bloom until reload", exception);
            return 0;
        }
    }

    static void release() {
        for (TextureTarget level : levels) if (level != null) level.destroyBuffers();
        levels = new TextureTarget[0];
    }

    static String diagnostics() {
        return "bloomLevels=" + levels.length + " bloomFailed=" + failed
                + " bloomDraws=" + draws + " bloomAllocations=" + allocations;
    }
}
