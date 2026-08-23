package com.starboundmc.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Supersampled sprite textures for the star-map ship: a hull (isosceles
 * triangle with glow, dark rim, center band and nose highlight) and an engine
 * flame cone. Both are rendered 4x and drawn via {@code GuiGraphics.blit}
 * under a rotated PoseStack, so the GPU's bilinear filtering keeps them crisp
 * at any GUI scale — same approach as {@link StarMapCanvas}.
 *
 * <p>The flame texture is drawn with its height scaled by current speed
 * (blit target height), so acceleration is visible without re-rendering.</p>
 */
public class StarShipSprite
{
    /** Supersampling factor (texture pixels per GUI-logical pixel). */
    public static final int SS = 4;

    /** Hull texture logical size (center = hull center; hull is 14x6 inside). */
    private static final int BODY_W = 26;
    private static final int BODY_H = 30;
    private static final double HULL_H = 14.0;
    private static final double HULL_W = 6.0;

    /** Flame texture logical size (cone, narrow end = engine nozzle). */
    private static final int FLAME_W = 12;
    private static final int FLAME_H = 20;

    private static ResourceLocation bodyLocation = null;
    private static ResourceLocation flameLocation = null;

    public static int bodyWidth()
    {
        return BODY_W;
    }

    public static int bodyHeight()
    {
        return BODY_H;
    }

    public static int flameWidth()
    {
        return FLAME_W;
    }

    public static int flameHeight()
    {
        return FLAME_H;
    }

    /** Hull height in GUI-logical pixels (half = distance from hull center to the base). */
    public static double hullHeight()
    {
        return HULL_H;
    }

    public static ResourceLocation body()
    {
        if (bodyLocation == null)
        {
            bodyLocation = upload("ship_body", renderBody());
        }
        return bodyLocation;
    }

    public static ResourceLocation flame()
    {
        if (flameLocation == null)
        {
            flameLocation = upload("ship_flame", renderFlame());
        }
        return flameLocation;
    }

    private static ResourceLocation upload(String name, NativeImage image)
    {
        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation location = Minecraft.getInstance().getTextureManager()
                .register("starmap_" + name, texture);
        texture.upload();
        RenderSystem.bindTexture(texture.getId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        return location;
    }

    // ---- pixel helpers (NativeImage stores 0xAABBGGRR) ----

    private static int abgr(int a, int r, int g, int b)
    {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static void blendPixel(NativeImage img, int x, int y, int a, int r, int g, int b)
    {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight())
            return;
        int cur = img.getPixelRGBA(x, y);
        int ca = (cur >>> 24) & 0xFF;
        int cr = cur & 0xFF;
        int cg = (cur >>> 8) & 0xFF;
        int cb = (cur >>> 16) & 0xFF;
        int na = a + ca * (255 - a) / 255;
        int nr = (r * a + cr * (255 - a)) / 255;
        int ng = (g * a + cg * (255 - a)) / 255;
        int nb = (b * a + cb * (255 - a)) / 255;
        img.setPixelRGBA(x, y, abgr(na, nr, ng, nb));
    }

    /** Distance from a point to the nearest edge of the triangle (0 on the edge, + outside). */
    private static double distToTriangle(double px, double py,
                                         double x1, double y1, double x2, double y2, double x3, double y3)
    {
        double d1 = distToSegment(px, py, x1, y1, x2, y2);
        double d2 = distToSegment(px, py, x2, y2, x3, y3);
        double d3 = distToSegment(px, py, x3, y3, x1, y1);
        return Math.min(d1, Math.min(d2, d3));
    }

    private static double distToSegment(double px, double py, double x1, double y1, double x2, double y2)
    {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        double t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / len2));
        double ex = x1 + t * dx;
        double ey = y1 + t * dy;
        return Math.hypot(px - ex, py - ey);
    }

    private static boolean insideTriangle(double px, double py,
                                          double x1, double y1, double x2, double y2, double x3, double y3)
    {
        double d1 = (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2);
        double d2 = (px - x3) * (y2 - y3) - (x2 - x3) * (py - y3);
        double d3 = (px - x1) * (y3 - y1) - (x3 - x1) * (py - y1);
        boolean neg = d1 < 0 || d2 < 0 || d3 < 0;
        boolean pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    // ---- hull ----

    private static NativeImage renderBody()
    {
        int w = BODY_W * SS;
        int h = BODY_H * SS;
        NativeImage img = new NativeImage(w, h, true);
        double cx = BODY_W / 2.0 * SS;
        double cy = BODY_H / 2.0 * SS;
        double hh = HULL_H / 2.0 * SS;
        double hw = HULL_W * SS;

        // Triangle vertices (nose up).
        double tx = cx, ty = cy - hh;
        double bx1 = cx - hw, by1 = cy + hh;
        double bx2 = cx + hw, by2 = cy + hh;

        double aa = SS;              // edge anti-aliasing width
        double rimW = 1.6 * SS;      // dark rim depth inside the edge
        double glowR = 5.5 * SS;     // outer glow reach

        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                double px = x + 0.5;
                double py = y + 0.5;
                double d = distToTriangle(px, py, tx, ty, bx1, by1, bx2, by2);
                boolean inside = insideTriangle(px, py, tx, ty, bx1, by1, bx2, by2);

                if (inside)
                {
                    // Dark rim close to the edge, hull color further in.
                    double rimT = Math.min(1.0, d / rimW);
                    int r = (int) (0x40 + (0x10 - 0x40) * rimT);
                    int g = (int) (0xE0 + (0x32 - 0xE0) * rimT);
                    int b = (int) (0xC0 + (0x3E - 0xC0) * rimT);
                    // Center band + nose highlight (brighten toward the nose).
                    double up = (cy - py) / hh; // 1 at nose, -1 at base
                    if (up > -0.25 && up < 0.9)
                    {
                        double band = 1.0 - Math.abs(up - 0.35) / 0.75;
                        r = (int) (r + (0x6A - r) * band * 0.8);
                        g = (int) (g + (0xD0 - g) * band * 0.8);
                        b = (int) (b + (0xE8 - b) * band * 0.8);
                    }
                    if (up > 0.55)
                    {
                        double nose = (up - 0.55) / 0.45;
                        r = (int) (r + (0x9A - r) * nose);
                        g = (int) (g + (0xF0 - g) * nose);
                        b = (int) (b + (0xF8 - b) * nose);
                    }
                    img.setPixelRGBA(x, y, abgr(255, r, g, b));
                }
                else if (d < aa)
                {
                    int a = (int) (255 * (1.0 - d / aa));
                    img.setPixelRGBA(x, y, abgr(a, 0x40, 0xE0, 0xC0));
                }
                else if (d < glowR)
                {
                    int a = (int) (50 * (1.0 - (d - aa) / (glowR - aa)));
                    blendPixel(img, x, y, a, 0x40, 0xE0, 0xC0);
                }
            }
        }
        return img;
    }

    // ---- flame ----

    private static NativeImage renderFlame()
    {
        int w = FLAME_W * SS;
        int h = FLAME_H * SS;
        NativeImage img = new NativeImage(w, h, true);
        double cx = FLAME_W / 2.0 * SS;
        double hw = FLAME_W / 2.0 * SS;

        // Cone: narrow end (nozzle) at the top, wide end at the bottom.
        double tx = cx, ty = 0.0;
        double bx1 = cx - hw, by1 = h;
        double bx2 = cx + hw, by2 = h;
        double aa = SS;

        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                double px = x + 0.5;
                double py = y + 0.5;
                double d = distToTriangle(px, py, tx, ty, bx1, by1, bx2, by2);
                if (insideTriangle(px, py, tx, ty, bx1, by1, bx2, by2))
                {
                    double t = py / h; // 0 at nozzle, 1 at tail
                    // Bright yellow core near the nozzle, orange toward the tail.
                    int r = (int) (0xF0 + (0xE0 - 0xF0) * t);
                    int g = (int) (0xB0 + (0x80 - 0xB0) * t);
                    int b = (int) (0x60 + (0x40 - 0x60) * t);
                    img.setPixelRGBA(x, y, abgr(235, r, g, b));
                }
                else if (d < aa)
                {
                    int a = (int) (200 * (1.0 - d / aa));
                    blendPixel(img, x, y, a, 0xE8, 0x90, 0x50);
                }
            }
        }
        return img;
    }
}
