// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

/**
 * Shared, low-intensity visor optics expressed in normalized GUI coordinates.
 * Input and output use {@code (-1,-1)} at the top-left and {@code (1,1)} at the bottom-right.
 */
public final class HudVisorSurface {
    private static final float HORIZONTAL_COMPRESSION = 0.015F;
    private static final float VERTICAL_COMPRESSION = 0.003F;
    private static final float HORIZONTAL_FADE = 0.16F;
    private static final float VERTICAL_FADE = 0.04F;

    private HudVisorSurface() { }

    public record Point(float x, float y) { }

    /**
     * Applies only a slight, axis-aligned peripheral compression. There is deliberately no
     * cross-axis bow: a horizontal text baseline stays horizontal and glyphs are not tilted to
     * prove that the HUD occupies a curved surface.
     */
    public static Point project(float normalizedX, float normalizedY) {
        float x2 = normalizedX * normalizedX;
        float y2 = normalizedY * normalizedY;
        float x = normalizedX * (1F - HORIZONTAL_COMPRESSION * x2);
        float y = normalizedY * (1F - VERTICAL_COMPRESSION * y2);
        return new Point(x, y);
    }

    public static Point projectGui(float guiX, float guiY, float guiWidth, float guiHeight) {
        if (guiWidth <= 0 || guiHeight <= 0)
            return new Point(guiX, guiY);
        float normalizedX = guiX * 2F / guiWidth - 1F;
        float normalizedY = guiY * 2F / guiHeight - 1F;
        Point projected = project(normalizedX, normalizedY);
        return new Point((projected.x + 1F) * guiWidth * .5F,
                (projected.y + 1F) * guiHeight * .5F);
    }

    /** Mild global edge fade; it never makes warnings illegible. */
    public static float edgeFade(float normalizedX, float normalizedY) {
        float horizontal = smoothstep(.70F, 1F, Math.abs(normalizedX));
        float vertical = smoothstep(.82F, 1F, Math.abs(normalizedY));
        return (1F - HORIZONTAL_FADE * horizontal) * (1F - VERTICAL_FADE * vertical);
    }

    public static float edgeFadeGui(float guiX, float guiY, float guiWidth, float guiHeight) {
        if (guiWidth <= 0 || guiHeight <= 0)
            return 1F;
        return edgeFade(guiX * 2F / guiWidth - 1F, guiY * 2F / guiHeight - 1F);
    }

    private static float smoothstep(float low, float high, float value) {
        float t = Math.clamp((value - low) / (high - low), 0F, 1F);
        return t * t * (3F - 2F * t);
    }
}
