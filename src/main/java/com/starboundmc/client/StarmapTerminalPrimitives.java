package com.starboundmc.client;

/** Pixel-aligned terminal shapes shared by star-map renderers. */
public final class StarmapTerminalPrimitives
{
    private StarmapTerminalPrimitives() {}

    /** The two-tone frame used by the current visual baseline. */
    public static void drawTwoToneFrame(StarmapHiDpiGraphics.DrawScope draw,
                                        int x, int y, int width, int height,
                                        int outerColor, int innerColor)
    {
        if (width < 2 || height < 2)
            return;
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        int left = draw.virtual(x);
        int top = draw.virtual(y);
        int right = draw.virtual(x + width);
        int bottom = draw.virtual(y + height);
        drawFrameStroke(draw, left, top, right, bottom, stroke, outerColor);
        if (right - left < stroke * 4 || bottom - top < stroke * 4)
            return;
        drawFrameStroke(draw, left + stroke, top + stroke,
                right - stroke, bottom - stroke, stroke, innerColor);
    }

    /** The corner brackets used by the current visual baseline. */
    public static void drawCornerBrackets(StarmapHiDpiGraphics.DrawScope draw,
                                          int x, int y, int width, int height, int color)
    {
        if (width < StarmapVisualTheme.CORNER_BRACKET_MIN_SIZE
                || height < StarmapVisualTheme.CORNER_BRACKET_MIN_SIZE)
            return;
        int length = Math.min(StarmapVisualTheme.CORNER_BRACKET_MAX_LENGTH,
                Math.max(StarmapVisualTheme.CORNER_BRACKET_MIN_LENGTH,
                        Math.min(width, height)
                                / StarmapVisualTheme.CORNER_BRACKET_LENGTH_DIVISOR));
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        int left = draw.virtual(x);
        int top = draw.virtual(y);
        int right = draw.virtual(x + width);
        int bottom = draw.virtual(y + height);
        int virtualLength = draw.virtual(length);
        draw.fillVirtual(left, top, left + virtualLength, top + stroke, color);
        draw.fillVirtual(left, top, left + stroke, top + virtualLength, color);
        draw.fillVirtual(right - virtualLength, top, right, top + stroke, color);
        draw.fillVirtual(right - stroke, top, right, top + virtualLength, color);
        draw.fillVirtual(left, bottom - stroke, left + virtualLength, bottom, color);
        draw.fillVirtual(left, bottom - virtualLength, left + stroke, bottom, color);
        draw.fillVirtual(right - virtualLength, bottom - stroke, right, bottom, color);
        draw.fillVirtual(right - stroke, bottom - virtualLength, right, bottom, color);
    }

    /** Three-layer mechanical frame shared by the shell, display and detail panel. */
    public static void drawThreeLayerFrame(StarmapHiDpiGraphics.DrawScope draw,
                                           int x, int y, int width, int height,
                                           int highlight, int surface, int shadow)
    {
        if (width <= 0 || height <= 0)
            return;
        int layer = StarmapVisualTheme.FRAME_LAYER_WIDTH;
        int left = draw.virtual(x);
        int top = draw.virtual(y);
        int right = draw.virtual(x + width);
        int bottom = draw.virtual(y + height);
        drawFrameStroke(draw, left, top, right, bottom, layer, shadow);
        if (right - left <= layer * 2 || bottom - top <= layer * 2)
            return;
        drawFrameStroke(draw, left + layer, top + layer,
                right - layer, bottom - layer, layer, surface);
        if (right - left <= layer * 4 || bottom - top <= layer * 4)
            return;
        drawFrameStroke(draw, left + layer * 2, top + layer * 2,
                right - layer * 2, bottom - layer * 2, layer, highlight);
    }

    /** Filled rectangular inset with a bright top/left edge and dark bottom/right edge. */
    public static void drawInsetSlot(StarmapHiDpiGraphics.DrawScope draw,
                                     int x, int y, int width, int height,
                                     int fillColor, int lightEdge, int darkEdge)
    {
        if (width <= 0 || height <= 0)
            return;
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        int left = draw.virtual(x);
        int top = draw.virtual(y);
        int right = draw.virtual(x + width);
        int bottom = draw.virtual(y + height);
        draw.fillVirtual(left, top, right, bottom, fillColor);
        draw.fillVirtual(left, top, right, top + stroke, darkEdge);
        draw.fillVirtual(left, top, left + stroke, bottom, darkEdge);
        draw.fillVirtual(left, bottom - stroke, right, bottom, lightEdge);
        draw.fillVirtual(right - stroke, top, right, bottom, lightEdge);
    }

    /** Filled stepped-corner panel used by shell plates, cards and key caps. */
    public static void drawSteppedPanel(StarmapHiDpiGraphics.DrawScope draw,
                                        int x, int y, int width, int height,
                                        int fillColor, int edgeColor)
    {
        if (width <= 0 || height <= 0)
            return;
        int cut = Math.min(draw.virtual(StarmapVisualTheme.CUT_CORNER_SIZE),
                Math.min(draw.virtual(width), draw.virtual(height)) / 2);
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        int left = draw.virtual(x);
        int top = draw.virtual(y);
        int right = draw.virtual(x + width);
        int bottom = draw.virtual(y + height);
        draw.fillVirtual(left + cut, top, right - cut, bottom, fillColor);
        draw.fillVirtual(left, top + cut, right, bottom - cut, fillColor);
        draw.fillVirtual(left + cut, top, right - cut, top + stroke, edgeColor);
        draw.fillVirtual(left + cut, bottom - stroke, right - cut, bottom, edgeColor);
        draw.fillVirtual(left, top + cut, left + stroke, bottom - cut, edgeColor);
        draw.fillVirtual(right - stroke, top + cut, right, bottom - cut, edgeColor);
        drawDiagonalStep(draw, left, top, cut, stroke, edgeColor, 1, 1);
        drawDiagonalStep(draw, right - stroke, top, cut, stroke, edgeColor, -1, 1);
        drawDiagonalStep(draw, left, bottom - stroke, cut, stroke, edgeColor, 1, -1);
        drawDiagonalStep(draw, right - stroke, bottom - stroke,
                cut, stroke, edgeColor, -1, -1);
    }

    /** Narrow filled label cap with a stepped leading edge. */
    public static void drawLabelCap(StarmapHiDpiGraphics.DrawScope draw,
                                    int x, int y, int width, int color)
    {
        int height = StarmapVisualTheme.LABEL_CAP_HEIGHT;
        int cut = Math.min(StarmapVisualTheme.CUT_CORNER_SIZE, height / 2);
        if (width <= cut || height <= 0)
            return;
        draw.fill(x + cut, y, x + width, y + height, color);
        draw.fill(x, y + cut, x + cut, y + height - cut, color);
        for (int offset = 0; offset < cut; offset++)
        {
            draw.fillVirtual(draw.virtual(x + offset), draw.virtual(y + cut - offset - 1),
                    draw.virtual(x + offset + 1), draw.virtual(y + height - cut + offset + 1),
                    color);
        }
    }

    /** Evenly spaced short calibration ticks on a horizontal rail. */
    public static void drawTicks(StarmapHiDpiGraphics.DrawScope draw,
                                 int x, int y, int width, int spacing, int color)
    {
        if (width <= 0 || spacing <= 0)
            return;
        int tickHeight = StarmapVisualTheme.TICK_LENGTH;
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        for (int offset = 0; offset <= width; offset += spacing)
        {
            int virtualX = draw.virtual(x + offset);
            int top = draw.virtual(y);
            draw.fillVirtual(virtualX, top,
                    virtualX + stroke, draw.virtual(y + tickHeight), color);
        }
    }

    /** One small square mounting point. */
    public static void drawFastener(StarmapHiDpiGraphics.DrawScope draw,
                                    int x, int y, int color)
    {
        int size = StarmapVisualTheme.FASTENER_SIZE;
        draw.fill(x, y, x + size, y + size, color);
    }

    /** Segmented horizontal guide rail for low-priority mechanical detail. */
    public static void drawSegmentedRail(StarmapHiDpiGraphics.DrawScope draw,
                                         int x, int y, int width, int color)
    {
        int segment = draw.virtual(StarmapVisualTheme.RAIL_SEGMENT_LENGTH);
        int advance = segment + draw.virtual(StarmapVisualTheme.RAIL_SEGMENT_GAP);
        int virtualWidth = draw.virtual(width);
        int left = draw.virtual(x);
        int top = draw.virtual(y);
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        for (int offset = 0; offset < virtualWidth; offset += advance)
        {
            draw.fillVirtual(left + offset, top,
                    left + Math.min(virtualWidth, offset + segment), top + stroke, color);
        }
    }

    /** Raised, stepped key cap with a recessed base and optional status rail. */
    public static void drawMechanicalButton(StarmapHiDpiGraphics.DrawScope draw,
                                            int x, int y, int width, int height,
                                            int fillColor, int lightEdge, int darkEdge,
                                            int statusColor, boolean pressed)
    {
        if (width <= 0 || height <= 0)
            return;
        drawSteppedPanel(draw, x, y, width, height,
                StarmapVisualTheme.SHELL_SHADOW, StarmapVisualTheme.SHELL_SHADOW);

        int pressOffset = pressed ? StarmapVisualTheme.BUTTON_PRESS_OFFSET : 0;
        int faceY = y + pressOffset;
        int faceHeight = Math.max(1, height - pressOffset);
        drawSteppedPanel(draw, x, faceY, width, faceHeight, fillColor, darkEdge);

        int cut = Math.min(draw.virtual(StarmapVisualTheme.CUT_CORNER_SIZE),
                Math.min(draw.virtual(width), draw.virtual(faceHeight)) / 2);
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        int left = draw.virtual(x);
        int top = draw.virtual(faceY);
        int right = draw.virtual(x + width);
        int bottom = draw.virtual(faceY + faceHeight);
        if (right - left <= cut * 2 || bottom - top <= stroke * 2)
            return;

        draw.fillVirtual(left + cut, top, right - cut, top + stroke, lightEdge);
        draw.fillVirtual(left, top + cut, left + stroke, bottom - cut, lightEdge);
        draw.fillVirtual(left + cut, bottom - stroke, right - cut, bottom, darkEdge);
        draw.fillVirtual(right - stroke, top + cut, right, bottom - cut, darkEdge);
        if (statusColor != 0 && bottom - top >= stroke * 4)
        {
            int railInset = cut + draw.virtual(2);
            if (right - left > railInset * 2)
            {
                draw.fillVirtual(left + railInset, bottom - stroke * 2,
                        right - railInset, bottom - stroke, statusColor);
            }
        }
    }

    private static void drawFrameStroke(StarmapHiDpiGraphics.DrawScope draw,
                                        int left, int top, int right, int bottom,
                                        int stroke, int color)
    {
        draw.fillVirtual(left, top, right, top + stroke, color);
        draw.fillVirtual(left, bottom - stroke, right, bottom, color);
        draw.fillVirtual(left, top, left + stroke, bottom, color);
        draw.fillVirtual(right - stroke, top, right, bottom, color);
    }

    private static void drawDiagonalStep(StarmapHiDpiGraphics.DrawScope draw,
                                         int startX, int startY, int length, int stroke,
                                         int color, int directionX, int directionY)
    {
        for (int offset = 0; offset < length; offset++)
        {
            int x = startX + directionX * offset;
            int y = startY + directionY * offset;
            draw.fillVirtual(x, y, x + stroke, y + stroke, color);
        }
    }
}
