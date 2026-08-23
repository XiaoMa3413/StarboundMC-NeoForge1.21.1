package com.starboundmc.client;

import net.minecraft.client.gui.GuiGraphics;

/** Draws static star-map chrome without owning screen state or input geometry. */
public final class StarmapChromeRenderer
{
    private final StarmapHiDpiGraphics hiDpi;

    public StarmapChromeRenderer(StarmapHiDpiGraphics hiDpi)
    {
        if (hiDpi == null)
            throw new IllegalArgumentException("hiDpi must not be null");
        this.hiDpi = hiDpi;
    }

    /** Draws the background, panel hierarchy, static frames and fuel gauge. */
    public void renderBase(GuiGraphics graphics, int screenWidth, int screenHeight,
                           int panelX, int panelY, int panelWidth, int panelHeight,
                           StarmapLayout layout, float fuelRatio)
    {
        StarmapLayout.Bounds canvas = layout.canvas();
        int canvasX = panelX + canvas.x();
        int canvasY = panelY + canvas.y();
        int headerHeight = layout.detail().y();
        int footerY = panelY + layout.detail().bottom();

        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            draw.fillGradient(0, 0, screenWidth, screenHeight,
                    StarmapVisualTheme.BACKGROUND_TOP,
                    StarmapVisualTheme.BACKGROUND_BOTTOM);
            drawMainShell(draw, panelX, panelY, panelWidth, panelHeight);
            drawHeaderSection(draw, panelX, panelY, panelWidth, headerHeight);
            drawFooterSection(draw, panelX, footerY,
                    panelWidth, panelY + panelHeight - footerY);
            drawDisplayBezel(draw, canvasX, canvasY, canvas.width(), canvas.height());

            if (!layout.compact())
            {
                StarmapLayout.Bounds detail = layout.detail();
                drawDetailPanel(draw, panelX + detail.x(), panelY + detail.y(),
                        detail.width(), detail.height(), false);
            }

            drawFuelInstrument(draw, canvasX, panelY + 16,
                    canvas.width(), 4, fuelRatio);
            drawTitlePlate(draw, panelX, panelY, panelWidth, headerHeight);
        }
    }

    /** Drawn after the canvas texture so its fine corner marks remain visible. */
    public void renderCanvasOverlay(GuiGraphics graphics, int x, int y, int width, int height)
    {
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            StarmapTerminalPrimitives.drawCornerBrackets(draw,
                    x - 1, y - 1, width + 2, height + 2,
                    StarmapVisualTheme.ACCENT_DIM);
            drawCanvasCalibration(draw, x, y, width, height);
        }
    }

    private static void drawCanvasCalibration(StarmapHiDpiGraphics.DrawScope draw,
                                              int x, int y, int width, int height)
    {
        if (width < 48 || height < 40)
            return;
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        int longMark = draw.virtual(StarmapVisualTheme.CANVAS_CALIBRATION_LONG);
        int shortMark = draw.virtual(StarmapVisualTheme.CANVAS_CALIBRATION_SHORT);
        int left = draw.virtual(x);
        int top = draw.virtual(y);
        int right = draw.virtual(x + width);
        int bottom = draw.virtual(y + height);
        int centerX = draw.virtual(x + width / 2);
        int centerY = draw.virtual(y + height / 2);
        int quarterX = draw.virtual(x + width / 4);
        int threeQuarterX = draw.virtual(x + width * 3 / 4);
        int quarterY = draw.virtual(y + height / 4);
        int threeQuarterY = draw.virtual(y + height * 3 / 4);
        int color = StarmapVisualTheme.CANVAS_CALIBRATION;

        draw.fillVirtual(centerX, top, centerX + stroke, top + longMark, color);
        draw.fillVirtual(centerX, bottom - longMark, centerX + stroke, bottom, color);
        draw.fillVirtual(left, centerY, left + longMark, centerY + stroke, color);
        draw.fillVirtual(right - longMark, centerY, right, centerY + stroke, color);

        draw.fillVirtual(quarterX, top, quarterX + stroke, top + shortMark, color);
        draw.fillVirtual(threeQuarterX, top,
                threeQuarterX + stroke, top + shortMark, color);
        draw.fillVirtual(quarterX, bottom - shortMark,
                quarterX + stroke, bottom, color);
        draw.fillVirtual(threeQuarterX, bottom - shortMark,
                threeQuarterX + stroke, bottom, color);
        draw.fillVirtual(left, quarterY, left + shortMark, quarterY + stroke, color);
        draw.fillVirtual(left, threeQuarterY,
                left + shortMark, threeQuarterY + stroke, color);
        draw.fillVirtual(right - shortMark, quarterY,
                right, quarterY + stroke, color);
        draw.fillVirtual(right - shortMark, threeQuarterY,
                right, threeQuarterY + stroke, color);
    }

    /** Draws the opaque compact-layout drawer in its existing elevated render pass. */
    public void renderDetailPanel(GuiGraphics graphics, int x, int y,
                                  int width, int height, boolean opaque)
    {
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            drawDetailPanel(draw, x, y, width, height, opaque);
        }
    }

    private static void drawDetailPanel(StarmapHiDpiGraphics.DrawScope draw,
                                        int x, int y, int width, int height, boolean opaque)
    {
        int detailColor = opaque ? StarmapVisualTheme.DETAIL_SURFACE_OPAQUE
                : StarmapVisualTheme.DETAIL_SURFACE;
        StarmapTerminalPrimitives.drawInsetSlot(draw,
                x, y, width, height, detailColor,
                StarmapVisualTheme.SHELL_HIGHLIGHT, StarmapVisualTheme.SHELL_SHADOW);
        StarmapTerminalPrimitives.drawThreeLayerFrame(draw, x, y, width, height,
                StarmapVisualTheme.SHELL_EDGE, StarmapVisualTheme.SHELL_RAISED,
                StarmapVisualTheme.SHELL_SHADOW);
        draw.fill(x + 1, y + 1,
                x + 1 + StarmapVisualTheme.DETAIL_ACCENT_WIDTH, y + height - 1,
                StarmapVisualTheme.ACCENT_DIM);
    }

    private static void drawMainShell(StarmapHiDpiGraphics.DrawScope draw,
                                      int x, int y, int width, int height)
    {
        StarmapTerminalPrimitives.drawSteppedPanel(draw,
                x, y, width, height,
                StarmapVisualTheme.SHELL_SHADOW, StarmapVisualTheme.SHELL_SHADOW);
        if (width <= 2 || height <= 2)
            return;
        StarmapTerminalPrimitives.drawSteppedPanel(draw,
                x + 1, y + 1, width - 2, height - 2,
                StarmapVisualTheme.SHELL_RAISED, StarmapVisualTheme.SHELL_HIGHLIGHT);
        int inset = StarmapVisualTheme.SHELL_INSET;
        if (width <= inset * 2 || height <= inset * 2)
            return;
        StarmapTerminalPrimitives.drawSteppedPanel(draw,
                x + inset, y + inset, width - inset * 2, height - inset * 2,
                StarmapVisualTheme.SHELL_SURFACE, StarmapVisualTheme.SHELL_SEAM);
    }

    private static void drawHeaderSection(StarmapHiDpiGraphics.DrawScope draw,
                                          int x, int y, int width, int height)
    {
        int cut = StarmapVisualTheme.CUT_CORNER_SIZE;
        if (width <= cut * 2 || height <= 6)
            return;
        StarmapTerminalPrimitives.drawSteppedPanel(draw,
                x + 3, y + 3, width - 6, height - 5,
                StarmapVisualTheme.HEADER_SURFACE, StarmapVisualTheme.SHELL_SEAM);
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        int lineY = draw.virtual(y + height - 2);
        draw.fillVirtual(draw.virtual(x + cut), lineY,
                draw.virtual(x + width - cut),
                lineY + stroke, StarmapVisualTheme.SHELL_HIGHLIGHT);
    }

    private static void drawFooterSection(StarmapHiDpiGraphics.DrawScope draw,
                                          int x, int y, int width, int height)
    {
        int cut = StarmapVisualTheme.CUT_CORNER_SIZE;
        if (width <= cut * 2 || height <= 2)
            return;
        int right = x + width;
        draw.fill(x + cut, y, right - cut, y + height - 2,
                StarmapVisualTheme.SHELL_RAISED);
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        int top = draw.virtual(y);
        draw.fillVirtual(draw.virtual(x + cut), top, draw.virtual(right - cut),
                top + stroke, StarmapVisualTheme.SHELL_HIGHLIGHT);

        int markWidth = Math.min(48, Math.max(0, width / 6));
        if (markWidth >= 8 && height >= 8)
        {
            StarmapTerminalPrimitives.drawTicks(draw,
                    x + 12, y + 3, markWidth, 8, StarmapVisualTheme.MECHANICAL_MARK);
            StarmapTerminalPrimitives.drawSegmentedRail(draw,
                    right - 12 - markWidth, y + 5, markWidth,
                    StarmapVisualTheme.MECHANICAL_MARK);
        }
        StarmapTerminalPrimitives.drawFastener(draw,
                x + 5, y + Math.max(2, height - 5), StarmapVisualTheme.MECHANICAL_MARK);
        StarmapTerminalPrimitives.drawFastener(draw,
                right - 6, y + Math.max(2, height - 5), StarmapVisualTheme.MECHANICAL_MARK);
    }

    private static void drawDisplayBezel(StarmapHiDpiGraphics.DrawScope draw,
                                         int x, int y, int width, int height)
    {
        int bezel = StarmapVisualTheme.DISPLAY_BEZEL_WIDTH;
        StarmapTerminalPrimitives.drawInsetSlot(draw,
                x - bezel, y - bezel, width + bezel * 2, height + bezel * 2,
                StarmapVisualTheme.DISPLAY_BEZEL,
                StarmapVisualTheme.DISPLAY_EDGE, StarmapVisualTheme.SHELL_SHADOW);
        StarmapTerminalPrimitives.drawThreeLayerFrame(draw,
                x - bezel + 1, y - bezel + 1,
                width + (bezel - 1) * 2, height + (bezel - 1) * 2,
                StarmapVisualTheme.DISPLAY_EDGE, StarmapVisualTheme.FRAME_INNER,
                StarmapVisualTheme.SHELL_SHADOW);
        StarmapTerminalPrimitives.drawTwoToneFrame(draw,
                x - 1, y - 1, width + 2, height + 2,
                StarmapVisualTheme.ACCENT_DIM, StarmapVisualTheme.FRAME_INNER);
    }

    private static void drawTitlePlate(StarmapHiDpiGraphics.DrawScope draw,
                                       int x, int y, int width, int headerHeight)
    {
        int availableWidth = width - 10;
        if (availableWidth < 28 || headerHeight < 18)
            return;
        int plateWidth = Math.min(availableWidth,
                Math.min(StarmapVisualTheme.TITLE_PLATE_MAX_WIDTH,
                        Math.max(StarmapVisualTheme.TITLE_PLATE_MIN_WIDTH, width / 4)));
        StarmapTerminalPrimitives.drawSteppedPanel(draw,
                x + 5, y + 3, plateWidth, StarmapVisualTheme.TITLE_PLATE_HEIGHT,
                StarmapVisualTheme.SHELL_RAISED, StarmapVisualTheme.SHELL_HIGHLIGHT);
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        draw.fillVirtual(draw.virtual(x + 6), draw.virtual(y + 5),
                draw.virtual(x + 6) + stroke,
                draw.virtual(y + StarmapVisualTheme.TITLE_PLATE_HEIGHT - 3),
                StarmapVisualTheme.ACCENT);
    }

    private static void drawFuelInstrument(StarmapHiDpiGraphics.DrawScope draw,
                                           int x, int y, int width, int height, float ratio)
    {
        int padding = StarmapVisualTheme.FUEL_SLOT_PADDING;
        StarmapTerminalPrimitives.drawInsetSlot(draw,
                x - padding, y - padding, width + padding * 2, height + padding * 2,
                StarmapVisualTheme.DISPLAY_BEZEL,
                StarmapVisualTheme.SHELL_EDGE, StarmapVisualTheme.SHELL_SHADOW);
        drawFuelBar(draw, x, y, width, height, ratio);
    }

    private static void drawFuelBar(StarmapHiDpiGraphics.DrawScope draw,
                                    int x, int y, int width, int height, float ratio)
    {
        int left = draw.virtual(x);
        int top = draw.virtual(y);
        int right = draw.virtual(x + width);
        int bottom = draw.virtual(y + height);
        int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
        int innerWidth = Math.max(0, right - left - stroke * 2);
        int fillWidth = Math.max(0, Math.min(innerWidth,
                (int) (innerWidth * Math.max(0.0F, Math.min(1.0F, ratio)))));

        draw.fillVirtual(left, top, right, bottom, StarmapVisualTheme.FUEL_TRACK);
        draw.fillVirtual(left + stroke, top + stroke,
                left + stroke + fillWidth, bottom - stroke, StarmapVisualTheme.STATUS_FUEL);
        draw.fillVirtual(left, top, right, top + stroke, StarmapVisualTheme.FUEL_EDGE);
        draw.fillVirtual(left, bottom - stroke, right, bottom, StarmapVisualTheme.FUEL_EDGE);

        int segment = draw.virtual(StarmapVisualTheme.FUEL_SEGMENT_LENGTH);
        for (int marker = left + stroke + segment;
             marker < right - stroke; marker += segment)
        {
            draw.fillVirtual(marker, top + stroke,
                    marker + stroke, bottom - stroke, StarmapVisualTheme.FUEL_SEPARATOR);
        }
    }
}
