package com.starboundmc.client;

/**
 * Projection between the fixed star-map authoring canvas and a local UI
 * rectangle. Screen offsets remain the responsibility of the owning screen.
 *
 * <p>The current viewport always shows the full authoring canvas. Keeping the
 * conversion behind this object allows a later implementation to add pan or
 * zoom without changing every marker, overlay and hit-test caller.</p>
 */
public final class StarmapViewport
{
    private final int baseWidth;
    private final int baseHeight;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final float scale;

    private StarmapViewport(int baseWidth, int baseHeight,
                            int x, int y, int width, int height, float scale)
    {
        if (baseWidth <= 0 || baseHeight <= 0)
            throw new IllegalArgumentException("Authoring canvas dimensions must be positive");
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Viewport dimensions must be positive");
        if (!Float.isFinite(scale) || scale <= 0.0F)
            throw new IllegalArgumentException("Viewport scale must be finite and positive");
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scale = scale;
    }

    public static StarmapViewport fixed(int baseWidth, int baseHeight,
                                        int x, int y, int width, int height, float scale)
    {
        return new StarmapViewport(baseWidth, baseHeight, x, y, width, height, scale);
    }

    public int baseWidth() { return baseWidth; }
    public int baseHeight() { return baseHeight; }
    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }
    public int right() { return x + width; }
    public int bottom() { return y + height; }
    public float scale() { return scale; }

    public int centerX()
    {
        return projectX(baseWidth / 2);
    }

    public int centerY()
    {
        return projectY(baseHeight / 2);
    }

    /** Projects an authoring-canvas pixel centre into local UI coordinates. */
    public int projectX(int baseX)
    {
        return x + StarmapGeometry.projectPixelCenter(baseX, width, baseWidth);
    }

    /** Projects an authoring-canvas pixel centre into local UI coordinates. */
    public int projectY(int baseY)
    {
        return y + StarmapGeometry.projectPixelCenter(baseY, height, baseHeight);
    }

    /** Converts a local UI x coordinate back into continuous authoring space. */
    public double unprojectX(double localX)
    {
        return ((localX - x + 0.5D) / width) * baseWidth - 0.5D;
    }

    /** Converts a local UI y coordinate back into continuous authoring space. */
    public double unprojectY(double localY)
    {
        return ((localY - y + 0.5D) / height) * baseHeight - 0.5D;
    }

    /** Scales a distance authored in base pixels exactly as the current UI did. */
    public int projectSize(int baseSize)
    {
        return Math.max(1, Math.round(baseSize * scale));
    }

    public boolean contains(double localX, double localY)
    {
        return localX >= x && localX < right() && localY >= y && localY < bottom();
    }
}
