package com.starboundmc.client.starmap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Size-aware texture cache for the static starfield and calibration grid. */
final class StarmapStarfieldCache {
    private static final long SEED = 0x5EEDL;
    private static final int GRID = 0x243A6373;
    private static int textureSequence;

    private ResourceLocation location;
    private int cachedWidth;
    private int cachedHeight;

    void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        ensureTexture(safeWidth, safeHeight);
        graphics.blit(location, x, y, safeWidth, safeHeight,
                0, 0, safeWidth, safeHeight, safeWidth, safeHeight);
    }

    void release() {
        if (location != null)
            Minecraft.getInstance().getTextureManager().release(location);
        location = null;
        cachedWidth = 0;
        cachedHeight = 0;
    }

    private void ensureTexture(int width, int height) {
        if (location != null && cachedWidth == width && cachedHeight == height)
            return;
        release();

        NativeImage image = render(width, height);
        DynamicTexture texture = new DynamicTexture(image);
        location = Minecraft.getInstance().getTextureManager().register(
                "starmap_redraw/starfield_" + textureSequence++, texture);
        texture.upload();
        cachedWidth = width;
        cachedHeight = height;
    }

    static NativeImage render(int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        NativeImage image = new NativeImage(safeWidth, safeHeight, true);
        for (Star star : pattern(safeWidth, safeHeight))
            fill(image, star.x(), star.y(), star.size(), star.size(), star.argb());

        int gridStep = Math.max(32, Math.min(safeWidth, safeHeight) / 5);
        for (int x = gridStep; x < safeWidth; x += gridStep)
            fill(image, x, 12, 1, safeHeight - 24, GRID);
        for (int y = gridStep; y < safeHeight; y += gridStep)
            fill(image, 12, y, safeWidth - 24, 1, GRID);
        return image;
    }

    static List<Star> pattern(int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        Random random = new Random(SEED);
        int count = starCount(safeWidth, safeHeight);
        List<Star> stars = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(safeWidth);
            int y = random.nextInt(safeHeight);
            int roll = random.nextInt(12);
            int size = roll < 3 ? 2 : 1;
            int color = roll == 0 ? 0xFFB9D7E5
                    : roll < 4 ? 0xFF789BB0 : 0xFF526B7C;
            stars.add(new Star(x, y, size, color));
        }
        return List.copyOf(stars);
    }

    static int starCount(int width, int height) {
        long area = (long) Math.max(1, width) * Math.max(1, height);
        return (int) Math.max(120L, area / 6500L);
    }

    private static void fill(NativeImage image, int x, int y,
                             int width, int height, int argb) {
        if (width <= 0 || height <= 0)
            return;
        int minimumX = Math.max(0, x);
        int minimumY = Math.max(0, y);
        int maximumX = Math.min(image.getWidth(), x + width);
        int maximumY = Math.min(image.getHeight(), y + height);
        int abgr = argbToAbgr(argb);
        for (int py = minimumY; py < maximumY; py++) {
            for (int px = minimumX; px < maximumX; px++)
                image.setPixelRGBA(px, py, abgr);
        }
    }

    private static int argbToAbgr(int argb) {
        return argb & 0xFF00FF00
                | (argb & 0x00FF0000) >>> 16
                | (argb & 0x000000FF) << 16;
    }

    record Star(int x, int y, int size, int argb) {}
}
