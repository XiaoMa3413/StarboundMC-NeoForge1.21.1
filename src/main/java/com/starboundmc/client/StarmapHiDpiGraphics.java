package com.starboundmc.client;

import net.minecraft.client.gui.GuiGraphics;

/** High-density primitive layer for star-map chrome drawn in virtual pixels. */
public final class StarmapHiDpiGraphics
{
    private final StarmapUiDensity density;

    public StarmapHiDpiGraphics(StarmapUiDensity density)
    {
        if (density == null)
            throw new IllegalArgumentException("density must not be null");
        this.density = density;
    }

    public StarmapUiDensity density()
    {
        return density;
    }

    /**
     * Enters virtual-pixel space. Closing the scope always restores the pose,
     * which keeps high-density rendering isolated from vanilla GUI drawing.
     */
    public DrawScope begin(GuiGraphics graphics)
    {
        if (graphics == null)
            throw new IllegalArgumentException("graphics must not be null");
        graphics.pose().pushPose();
        graphics.pose().scale(density.inverseScale(), density.inverseScale(), 1.0F);
        return new DrawScope(graphics, density);
    }

    /** Draws a one-virtual-pixel frame: 0.5 logical px when density is 2x. */
    public void drawFineFrame(GuiGraphics graphics, int x, int y,
                              int width, int height, int color)
    {
        if (width <= 0 || height <= 0)
            return;
        try (DrawScope draw = begin(graphics))
        {
            int left = draw.virtual(x);
            int top = draw.virtual(y);
            int right = draw.virtual(x + width);
            int bottom = draw.virtual(y + height);
            draw.fillVirtual(left, top, right, top + 1, color);
            draw.fillVirtual(left, bottom - 1, right, bottom, color);
            draw.fillVirtual(left, top + 1, left + 1, bottom - 1, color);
            draw.fillVirtual(right - 1, top + 1, right, bottom - 1, color);
        }
    }

    public static final class DrawScope implements AutoCloseable
    {
        private final GuiGraphics graphics;
        private final StarmapUiDensity density;
        private boolean closed;

        private DrawScope(GuiGraphics graphics, StarmapUiDensity density)
        {
            this.graphics = graphics;
            this.density = density;
        }

        public int virtual(int logicalCoordinate)
        {
            return density.virtual(logicalCoordinate);
        }

        public void fill(int x, int y, int right, int bottom, int color)
        {
            fillVirtual(virtual(x), virtual(y), virtual(right), virtual(bottom), color);
        }

        public void fillVirtual(int x, int y, int right, int bottom, int color)
        {
            graphics.fill(x, y, right, bottom, color);
        }

        public void fillGradient(int x, int y, int right, int bottom,
                                 int topColor, int bottomColor)
        {
            graphics.fillGradient(virtual(x), virtual(y), virtual(right), virtual(bottom),
                    topColor, bottomColor);
        }

        @Override
        public void close()
        {
            if (closed)
                return;
            closed = true;
            graphics.pose().popPose();
        }
    }
}
