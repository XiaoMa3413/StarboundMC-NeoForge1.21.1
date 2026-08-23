package com.starboundmc.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarmapBodyType;
import com.starboundmc.world.starmap.StarmapBodyVisual;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** Generates size-aware, supersampled static scenery for the responsive star map. */
public final class StarMapCanvas
{
    public static final int SS = 2;
    private static final int VISUAL_VERSION = 3;
    private static final int MAX_TEXTURE_SIZE = 2048;
    private static final int MAX_CACHE_ENTRIES = 6;
    private static final StarmapBodyVisual LOCKED_BODY_VISUAL = StarmapBodyVisual.builder(
                    StarmapBodyType.GENERIC, 0xFF707070, 1, 0x4C4F434BL)
            .secondaryColor(0xFF505050)
            .surfaceDetail(0.35F)
            .build();
    private static final Map<CacheKey, CanvasTexture> CACHE =
            new LinkedHashMap<>(8, 0.75F, true);
    private static final Map<String, Optional<NativeImage>> BODY_SPRITES = new HashMap<>();
    private static int textureSequence;

    private StarMapCanvas()
    {
    }

    public static CanvasTexture galaxy(int canvasWidth, int canvasHeight)
    {
        return getOrCreate(new CacheKey("galaxy", canvasWidth, canvasHeight,
                VISUAL_VERSION), null, null);
    }

    public static CanvasTexture get(StarSystem system, int canvasWidth, int canvasHeight)
    {
        return getOrCreate(new CacheKey(system.getSystemId(), canvasWidth, canvasHeight,
                VISUAL_VERSION), system, null);
    }

    public static CanvasTexture focus(StarSystem system, PlanetEntry entry,
                                      int canvasWidth, int canvasHeight)
    {
        return getOrCreate(new CacheKey(system.getSystemId() + "_focus_"
                        + entry.getEntryId().replace(':', '_'),
                canvasWidth, canvasHeight, VISUAL_VERSION), system, entry);
    }

    private static CanvasTexture getOrCreate(CacheKey key, StarSystem system, PlanetEntry focus)
    {
        CanvasTexture cached = CACHE.get(key);
        if (cached != null)
            return cached;

        int destinationWidth = Math.max(1, key.width());
        int destinationHeight = Math.max(1, key.height());
        int[] textureSize = textureSizeFor(destinationWidth, destinationHeight);
        int textureWidth = textureSize[0];
        int textureHeight = textureSize[1];
        NativeImage image = new NativeImage(textureWidth, textureHeight, true);
        CanvasMetrics metrics = new CanvasMetrics(destinationWidth, destinationHeight,
                textureWidth, textureHeight);
        drawBackground(image, metrics);
        if (system == null)
        {
            for (StarSystem candidate : StarSystems.all())
            {
                int[] position = StarmapGeometry.galaxyPosition(candidate);
                drawDimStar(image, metrics.sourceX(position[0]), metrics.sourceY(position[1]),
                        candidate.getStarColor(), metrics.sourceRadius(
                                Math.max(10, candidate.getStarGlowSize() / 2)));
            }
        }
        else if (focus != null)
        {
            drawFocusScenery(image, system, focus, metrics);
        }
        else
        {
            drawScenery(image, system, metrics);
        }

        DynamicTexture texture = new DynamicTexture(image);
        String textureName = "starmap_canvas/" + key.id() + "_v"
                + key.visualVersion() + "_" + textureSequence++;
        ResourceLocation location = Minecraft.getInstance().getTextureManager()
                .register(textureName, texture);
        texture.upload();
        RenderSystem.bindTexture(texture.getId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        CanvasTexture result = new CanvasTexture(location, textureWidth, textureHeight);
        CACHE.put(key, result);
        trimCache();
        return result;
    }

    static int[] textureSizeFor(int requestedWidth, int requestedHeight)
    {
        int destinationWidth = Math.max(1, requestedWidth);
        int destinationHeight = Math.max(1, requestedHeight);
        double minimumQuality = Math.max(SS, Math.max(
                StarmapGeometry.BASE_WIDTH * (double) SS / destinationWidth,
                StarmapGeometry.BASE_HEIGHT * (double) SS / destinationHeight));
        double quality = Math.min(minimumQuality, MAX_TEXTURE_SIZE
                / (double) Math.max(destinationWidth, destinationHeight));
        int textureWidth = Math.max(1, (int) Math.round(destinationWidth * quality));
        int textureHeight = Math.max(1, (int) Math.round(destinationHeight * quality));
        return new int[] { Math.min(MAX_TEXTURE_SIZE, textureWidth),
                Math.min(MAX_TEXTURE_SIZE, textureHeight) };
    }

    static int visualVersion()
    {
        return VISUAL_VERSION;
    }

    private static void trimCache()
    {
        Iterator<Map.Entry<CacheKey, CanvasTexture>> iterator = CACHE.entrySet().iterator();
        while (CACHE.size() > MAX_CACHE_ENTRIES && iterator.hasNext())
        {
            CanvasTexture stale = iterator.next().getValue();
            iterator.remove();
            Minecraft.getInstance().getTextureManager().release(stale.location());
        }
    }

    private static int abgr(int a, int r, int g, int b)
    {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static void blendPixel(NativeImage image, int x, int y, int a, int r, int g, int b)
    {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight() || a <= 0)
            return;
        int sourceAlpha = Math.min(255, a);
        int current = image.getPixelRGBA(x, y);
        int currentAlpha = (current >>> 24) & 0xFF;
        int currentRed = current & 0xFF;
        int currentGreen = (current >>> 8) & 0xFF;
        int currentBlue = (current >>> 16) & 0xFF;
        int nextAlpha = sourceAlpha + currentAlpha * (255 - sourceAlpha) / 255;
        int nextRed = (r * sourceAlpha + currentRed * (255 - sourceAlpha)) / 255;
        int nextGreen = (g * sourceAlpha + currentGreen * (255 - sourceAlpha)) / 255;
        int nextBlue = (b * sourceAlpha + currentBlue * (255 - sourceAlpha)) / 255;
        image.setPixelRGBA(x, y, abgr(nextAlpha, nextRed, nextGreen, nextBlue));
    }

    private static void drawBackground(NativeImage image, CanvasMetrics metrics)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y++)
        {
            float t = height <= 1 ? 0.0F : y / (float) (height - 1);
            int r = interpolateColorComponent(
                    StarmapVisualTheme.DISPLAY_TOP,
                    StarmapVisualTheme.DISPLAY_BOTTOM, 16, t);
            int g = interpolateColorComponent(
                    StarmapVisualTheme.DISPLAY_TOP,
                    StarmapVisualTheme.DISPLAY_BOTTOM, 8, t);
            int b = interpolateColorComponent(
                    StarmapVisualTheme.DISPLAY_TOP,
                    StarmapVisualTheme.DISPLAY_BOTTOM, 0, t);
            for (int x = 0; x < width; x++)
                image.setPixelRGBA(x, y, abgr(255, r, g, b));
        }

        Random random = new Random(0x5EED1234L);
        int inset = Math.max(2, (int) Math.ceil(metrics.sourceRadius(2)));
        int starRgb = StarmapVisualTheme.CANVAS_STAR_RGB;
        int starRed = starRgb >> 16 & 0xFF;
        int starGreen = starRgb >> 8 & 0xFF;
        int starBlue = starRgb & 0xFF;
        for (int i = 0; i < StarmapVisualTheme.CANVAS_BACKGROUND_STAR_COUNT; i++)
        {
            int sx = inset + random.nextInt(Math.max(1, width - inset * 2));
            int sy = inset + random.nextInt(Math.max(1, height - inset * 2));
            int alpha = StarmapVisualTheme.CANVAS_STAR_ALPHA_MIN
                    + random.nextInt(StarmapVisualTheme.CANVAS_STAR_ALPHA_MAX
                            - StarmapVisualTheme.CANVAS_STAR_ALPHA_MIN + 1);
            double radius = metrics.sourceRadius(random.nextDouble() < 0.18D ? 0.9D : 0.45D);
            drawSoftDot(image, sx, sy, radius, alpha, starRed, starGreen, starBlue);
        }
    }

    private static void drawDimStar(NativeImage image, double cx, double cy, int color, double glow)
    {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        forEachPixel(cx, cy, glow, image, (x, y, distance) -> {
            double t = distance / glow;
            blendPixel(image, x, y, (int) (0x38 * (1.0D - t) * (1.0D - t)),
                    red, green, blue);
            if (distance <= glow / 4.0D)
            {
                int alpha = (int) (0x88 * (1.0D - distance / (glow / 4.0D + 0.5D)));
                blendPixel(image, x, y, alpha, red, green, blue);
            }
        });
    }

    private static void drawScenery(NativeImage image, StarSystem system, CanvasMetrics metrics)
    {
        double cx = metrics.sourceX(StarmapGeometry.BASE_WIDTH / 2);
        double cy = metrics.sourceY(StarmapGeometry.BASE_HEIGHT / 2);
        int starColor = system.getStarColor();
        int starRed = (starColor >> 16) & 0xFF;
        int starGreen = (starColor >> 8) & 0xFF;
        int starBlue = starColor & 0xFF;

        if (system.getRadiationRadius() > 0)
        {
            double radius = metrics.sourceRadius(system.getRadiationRadius());
            double feather = Math.max(1.0D, metrics.sourceRadius(2.0D));
            forEachPixel(cx, cy, radius + feather, image, (x, y, distance) -> {
                if (distance < radius)
                    blendPixel(image, x, y, 14, 0xC0, 0x40, 0x30);
                else
                    blendPixel(image, x, y,
                            (int) (0x88 * (1.0D - (distance - radius) / feather)),
                            0xE0, 0x60, 0x40);
            });
        }

        for (PlanetEntry entry : system.getEntries())
        {
            if (entry.isMoon())
                continue;
            double radius = metrics.sourceRadius(entry.getOrbitRadius());
            drawOrbitRing(image, cx, cy, radius, metrics,
                    StarmapVisualTheme.ORBIT_MAJOR_RGB,
                    StarmapVisualTheme.ORBIT_MAJOR_ALPHA);
        }
        for (PlanetEntry entry : system.getEntries())
        {
            if (!entry.isMoon())
                continue;
            int[] parentPosition = StarmapGeometry.moonOrbitCenter(entry);
            if (parentPosition != null)
            {
                drawOrbitRing(image, metrics.sourceX(parentPosition[0]),
                        metrics.sourceY(parentPosition[1]),
                        metrics.sourceRadius(entry.getOrbitRadius()), metrics,
                        StarmapVisualTheme.ORBIT_MINOR_RGB,
                        StarmapVisualTheme.ORBIT_MINOR_ALPHA);
            }
        }

        double glow = metrics.sourceRadius(system.getStarGlowSize());
        double core = Math.max(2.0D, glow / 4.0D);
        forEachPixel(cx, cy, glow, image, (x, y, distance) -> {
            double t = distance / glow;
            blendPixel(image, x, y, (int) (0x60 * (1.0D - t) * (1.0D - t)),
                    255, 255, 255);
            if (distance <= core)
            {
                int alpha = (int) (0xE0 * (1.0D - distance / (core + 0.5D)));
                blendPixel(image, x, y, alpha, starRed, starGreen, starBlue);
            }
        });

        for (PlanetEntry entry : system.getEntries())
        {
            int[] position = StarmapGeometry.bodyPosition(entry);
            drawBodyDisc(image, metrics, entry, position[0], position[1],
                    StarmapGeometry.overviewDiameter(entry),
                    cx - metrics.sourceX(position[0]),
                    cy - metrics.sourceY(position[1]), false);
        }
    }

    private static void drawFocusScenery(NativeImage image, StarSystem system,
                                          PlanetEntry focus, CanvasMetrics metrics)
    {
        int centerX = StarmapGeometry.BASE_WIDTH / 2;
        int centerY = StarmapGeometry.BASE_HEIGHT / 2;
        int[] originalPosition = StarmapGeometry.bodyPosition(focus);
        double lightDirectionX = centerX - originalPosition[0];
        double lightDirectionY = centerY - originalPosition[1];
        Iterable<StarmapFocusGeometry.Placement> placements =
                StarmapFocusGeometry.placements(system, focus);
        for (StarmapFocusGeometry.Placement placement : placements)
        {
            if (placement.orbitRadius() > 0)
            {
                drawOrbitRing(image, metrics.sourceX(centerX), metrics.sourceY(centerY),
                        metrics.sourceRadius(placement.orbitRadius()), metrics,
                        StarmapVisualTheme.ORBIT_MINOR_RGB,
                        StarmapVisualTheme.ORBIT_MINOR_ALPHA);
            }
        }
        for (StarmapFocusGeometry.Placement placement : placements)
        {
            drawBodyDisc(image, metrics, placement.entry(), placement.x(), placement.y(),
                    placement.diameter(), lightDirectionX, lightDirectionY,
                    placement.orbitRadius() == 0);
        }
    }

    private static void drawOrbitRing(NativeImage image, double cx, double cy,
                                      double radius, CanvasMetrics metrics,
                                      int rgb, int maximumAlpha)
    {
        double band = Math.max(0.75D, metrics.sourceRadius(0.7D));
        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        int extent = (int) Math.ceil(radius + band);
        for (int y = Math.max(0, (int) Math.floor(cy) - extent);
             y <= Math.min(image.getHeight() - 1, (int) Math.ceil(cy) + extent); y++)
        {
            for (int x = Math.max(0, (int) Math.floor(cx) - extent);
                 x <= Math.min(image.getWidth() - 1, (int) Math.ceil(cx) + extent); x++)
            {
                double distance = Math.abs(Math.hypot(x - cx, y - cy) - radius);
                if (distance < band)
                    blendPixel(image, x, y,
                            (int) (maximumAlpha * (1.0D - distance / band)),
                            red, green, blue);
            }
        }
    }

    private static void drawBodyDisc(NativeImage image, CanvasMetrics metrics, PlanetEntry entry,
                                     int baseX, int baseY, int diameter,
                                     double lightDirectionX, double lightDirectionY,
                                     boolean preferFocusSprite)
    {
        double px = metrics.sourceX(baseX);
        double py = metrics.sourceY(baseY);
        double radius = Math.max(1.0D, metrics.sourceRadius(diameter / 2.0D));
        StarmapBodyVisual visual = entry.isReachable() ? entry.getVisual() : LOCKED_BODY_VISUAL;
        NativeImage sprite = entry.isReachable()
                ? bodySprite(visual, preferFocusSprite) : null;
        double feather = Math.max(1.0D, metrics.sourceRadius(1.0D));
        forEachPixel(px, py, radius + feather, image, (x, y, distance) -> {
            double localX = (x - px) / radius;
            double localY = (y - py) / radius;
            int sourceAlpha = 255;
            int color;
            if (sprite != null)
            {
                int sampleX = clamp((int) Math.round((localX + 1.0D) * 0.5D
                        * (sprite.getWidth() - 1)), 0, sprite.getWidth() - 1);
                int sampleY = clamp((int) Math.round((localY + 1.0D) * 0.5D
                        * (sprite.getHeight() - 1)), 0, sprite.getHeight() - 1);
                int sample = sprite.getPixelRGBA(sampleX, sampleY);
                sourceAlpha = sample >>> 24 & 0xFF;
                if (sourceAlpha == 0)
                    return;
                int argb = sourceAlpha << 24
                        | (sample & 0xFF) << 16
                        | (sample >>> 8 & 0xFF) << 8
                        | (sample >>> 16 & 0xFF);
                color = StarmapBodyShading.shadeTexture(argb, localX, localY,
                        lightDirectionX, lightDirectionY);
            }
            else
            {
                color = StarmapBodyShading.shade(visual, localX, localY,
                        lightDirectionX, lightDirectionY);
            }
            int red = color >> 16 & 0xFF;
            int green = color >> 8 & 0xFF;
            int blue = color & 0xFF;
            if (distance <= radius && sourceAlpha == 255)
                image.setPixelRGBA(x, y, abgr(255, red, green, blue));
            else
            {
                double edgeAlpha = distance <= radius ? 1.0D
                        : 1.0D - (distance - radius) / feather;
                blendPixel(image, x, y,
                        (int) (sourceAlpha * edgeAlpha),
                        red, green, blue);
            }
        });
    }

    private static NativeImage bodySprite(StarmapBodyVisual visual, boolean preferFocusSprite)
    {
        String textureId = preferFocusSprite && visual.getFocusTextureId() != null
                ? visual.getFocusTextureId() : visual.getTextureId();
        if (textureId == null)
            return null;
        return BODY_SPRITES.computeIfAbsent(textureId, StarMapCanvas::loadBodySprite)
                .orElse(null);
    }

    private static Optional<NativeImage> loadBodySprite(String textureId)
    {
        try
        {
            ResourceLocation location = ResourceLocation.tryParse(textureId);
            if (location == null)
                return Optional.empty();
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
                    .getResource(location);
            if (resource.isEmpty())
                return Optional.empty();
            try (InputStream stream = resource.get().open())
            {
                return Optional.of(NativeImage.read(stream));
            }
        }
        catch (IOException | RuntimeException ignored)
        {
            return Optional.empty();
        }
    }

    private static int clamp(int value, int minimum, int maximum)
    {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static int interpolateColorComponent(int fromColor, int toColor,
                                                 int shift, float amount)
    {
        int from = fromColor >> shift & 0xFF;
        int to = toColor >> shift & 0xFF;
        return Math.round(from + (to - from) * amount);
    }

    private static void drawSoftDot(NativeImage image, double cx, double cy, double radius,
                                    int alpha, int red, int green, int blue)
    {
        forEachPixel(cx, cy, radius, image, (x, y, distance) ->
                blendPixel(image, x, y,
                        (int) (alpha * (1.0D - distance / (radius + 0.5D))),
                        red, green, blue));
    }

    private static void forEachPixel(double cx, double cy, double radius, NativeImage image,
                                     PixelConsumer consumer)
    {
        int minX = Math.max(0, (int) Math.floor(cx - radius));
        int maxX = Math.min(image.getWidth() - 1, (int) Math.ceil(cx + radius));
        int minY = Math.max(0, (int) Math.floor(cy - radius));
        int maxY = Math.min(image.getHeight() - 1, (int) Math.ceil(cy + radius));
        for (int y = minY; y <= maxY; y++)
        {
            for (int x = minX; x <= maxX; x++)
            {
                double distance = Math.hypot(x - cx, y - cy);
                if (distance <= radius)
                    consumer.accept(x, y, distance);
            }
        }
    }

    public record CanvasTexture(ResourceLocation location, int width, int height) {}

    private record CacheKey(String id, int width, int height, int visualVersion) {}

    private record CanvasMetrics(int destinationWidth, int destinationHeight,
                                 int textureWidth, int textureHeight)
    {
        double sourceX(int baseX)
        {
            int destinationX = StarmapGeometry.projectPixelCenter(
                    baseX, destinationWidth, StarmapGeometry.BASE_WIDTH);
            return (destinationX + 0.5D) * textureWidth / destinationWidth - 0.5D;
        }

        double sourceY(int baseY)
        {
            int destinationY = StarmapGeometry.projectPixelCenter(
                    baseY, destinationHeight, StarmapGeometry.BASE_HEIGHT);
            return (destinationY + 0.5D) * textureHeight / destinationHeight - 0.5D;
        }

        double sourceRadius(double baseRadius)
        {
            double destinationScale = Math.min(destinationWidth / (double) StarmapGeometry.BASE_WIDTH,
                    destinationHeight / (double) StarmapGeometry.BASE_HEIGHT);
            double textureScale = Math.min(textureWidth / (double) destinationWidth,
                    textureHeight / (double) destinationHeight);
            return baseRadius * destinationScale * textureScale;
        }
    }

    @FunctionalInterface
    private interface PixelConsumer
    {
        void accept(int x, int y, double distance);
    }
}
