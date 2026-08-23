package com.starboundmc.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Supersampled reusable sprites for live star-map overlays. */
public final class StarmapOverlaySprites
{
    private static final int LOGICAL_SIZE = 39;
    private static final int SS = 4;
    private static final int TEXTURE_SIZE = LOGICAL_SIZE * SS;
    private static final double RING_RADIUS = 17.5D;
    private static ResourceLocation ringLocation;
    private static ResourceLocation diskLocation;

    private StarmapOverlaySprites()
    {
    }

    public static void drawRing(GuiGraphics graphics, int centerX, int centerY,
                                int radius, int argb)
    {
        if (radius <= 0)
            return;
        int size = logicalSize(radius);
        drawRingAt(graphics, centerX - size / 2, centerY - size / 2, size, argb);
    }

    public static void drawDisk(GuiGraphics graphics, int centerX, int centerY,
                                int radius, int argb)
    {
        if (radius <= 0)
            return;
        int size = logicalSize(radius);
        drawDiskAt(graphics, centerX - size / 2, centerY - size / 2, size, argb);
    }

    static int logicalSize(int radius)
    {
        int size = Math.max(3, (int) Math.round(radius * LOGICAL_SIZE / RING_RADIUS));
        return (size & 1) == 0 ? size + 1 : size;
    }

    static void drawRingAt(GuiGraphics graphics, int x, int y,
                           int size, int argb)
    {
        drawTinted(graphics, ring(), x, y, size, argb);
    }

    static void drawDiskAt(GuiGraphics graphics, int x, int y,
                           int size, int argb)
    {
        drawTinted(graphics, disk(), x, y, size, argb);
    }

    private static ResourceLocation ring()
    {
        if (ringLocation != null)
            return ringLocation;
        NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);
        double center = TEXTURE_SIZE / 2.0D;
        double radius = RING_RADIUS * SS;
        double halfStroke = 0.55D * SS;
        double antialias = 0.9D * SS;
        for (int y = 0; y < TEXTURE_SIZE; y++)
        {
            for (int x = 0; x < TEXTURE_SIZE; x++)
            {
                double distance = Math.hypot(x + 0.5D - center, y + 0.5D - center);
                double edgeDistance = Math.abs(distance - radius) - halfStroke;
                if (edgeDistance <= antialias)
                {
                    int alpha = edgeDistance <= 0.0D ? 255
                            : (int) (255 * (1.0D - edgeDistance / antialias));
                    image.setPixelRGBA(x, y, abgr(alpha, 255, 255, 255));
                }
            }
        }

        ringLocation = upload("starmap_overlay_ring", image);
        return ringLocation;
    }

    private static ResourceLocation disk()
    {
        if (diskLocation != null)
            return diskLocation;
        NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);
        double center = TEXTURE_SIZE / 2.0D;
        double radius = RING_RADIUS * SS;
        double antialias = 0.9D * SS;
        for (int y = 0; y < TEXTURE_SIZE; y++)
        {
            for (int x = 0; x < TEXTURE_SIZE; x++)
            {
                double distance = Math.hypot(x + 0.5D - center, y + 0.5D - center);
                double edgeDistance = distance - radius;
                if (edgeDistance <= antialias)
                {
                    int alpha = edgeDistance <= 0.0D ? 255
                            : (int) (255 * (1.0D - edgeDistance / antialias));
                    image.setPixelRGBA(x, y, abgr(alpha, 255, 255, 255));
                }
            }
        }
        diskLocation = upload("starmap_overlay_disk", image);
        return diskLocation;
    }

    private static void drawTinted(GuiGraphics graphics, ResourceLocation location,
                                   int x, int y, int size, int argb)
    {
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, alpha);
        graphics.blit(location, x, y, size, size,
                0, 0, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation upload(String name, NativeImage image)
    {
        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation location = Minecraft.getInstance().getTextureManager()
                .register(name, texture);
        texture.upload();
        RenderSystem.bindTexture(texture.getId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        return location;
    }

    private static int abgr(int alpha, int red, int green, int blue)
    {
        return (alpha << 24) | (blue << 16) | (green << 8) | red;
    }
}
