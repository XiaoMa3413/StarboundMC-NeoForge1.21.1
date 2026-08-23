package com.starboundmc.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Draws immutable detail content without reading navigation or selection state. */
public final class StarmapDetailRenderer
{
    private final Font font;
    private final StarmapLayout layout;
    private final StarmapHiDpiGraphics hiDpi;
    private String cachedDescriptionText;
    private int cachedDescriptionWidth = -1;
    private List<String> cachedDescriptionLines = List.of();

    public StarmapDetailRenderer(Font font, StarmapLayout layout,
                                 StarmapHiDpiGraphics hiDpi)
    {
        if (font == null)
            throw new IllegalArgumentException("font must not be null");
        if (layout == null)
            throw new IllegalArgumentException("layout must not be null");
        if (hiDpi == null)
            throw new IllegalArgumentException("hiDpi must not be null");
        this.font = font;
        this.layout = layout;
        this.hiDpi = hiDpi;
    }

    public void render(GuiGraphics graphics, StarmapDetailContent content)
    {
        if (content == null)
            return;
        if (content.subtitle().getString().isEmpty()
                && content.description().getString().isEmpty()
                && content.sections().isEmpty())
        {
            renderSelectionHint(graphics, content.title());
            return;
        }

        int descriptionStart = renderHeader(graphics, content.title(), content.subtitle());
        int sectionHeight = totalSectionHeight(content.sections());
        int contentBottom = layout.actionButton().y()
                - StarmapVisualTheme.DETAIL_ACTION_CLEARANCE;
        int sectionStart = Math.max(descriptionStart, contentBottom - sectionHeight);
        renderDescription(graphics, content.description(),
                descriptionStart, sectionStart - StarmapVisualTheme.SECTION_GAP);
        renderSections(graphics, content.sections(), sectionStart, contentBottom);
    }

    private void renderSelectionHint(GuiGraphics graphics, Component hint)
    {
        StarmapLayout.Bounds detail = layout.detail();
        int x = detail.x() + StarmapVisualTheme.DETAIL_CONTENT_INSET;
        int y = detail.y() + StarmapVisualTheme.DETAIL_CONTENT_INSET;
        int width = detail.width() - StarmapVisualTheme.DETAIL_CONTENT_INSET * 2;
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            StarmapTerminalPrimitives.drawInsetSlot(draw,
                    x, y, width, StarmapVisualTheme.DETAIL_HINT_HEIGHT,
                    StarmapVisualTheme.DETAIL_SECTION_SURFACE,
                    StarmapVisualTheme.DETAIL_SECTION_EDGE,
                    StarmapVisualTheme.SHELL_SHADOW);
            StarmapTerminalPrimitives.drawSegmentedRail(draw,
                    x + 3, y + StarmapVisualTheme.DETAIL_HINT_HEIGHT - 3,
                    Math.max(0, width - 6), StarmapVisualTheme.ACCENT_DIM);
        }
        graphics.drawString(font, fitText(hint.getString(), Math.max(1, width - 8)),
                x + 4, y + 5, StarmapVisualTheme.TEXT_BODY, false);
    }

    private int renderHeader(GuiGraphics graphics, Component title, Component subtitle)
    {
        StarmapLayout.Bounds detail = layout.detail();
        int inset = StarmapVisualTheme.DETAIL_CONTENT_INSET;
        int x = detail.x() + inset;
        int y = detail.y() + 2;
        int width = detail.width() - inset * 2;
        int height = StarmapVisualTheme.DETAIL_HEADER_HEIGHT;
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            StarmapTerminalPrimitives.drawSteppedPanel(draw,
                    x, y, width, height,
                    StarmapVisualTheme.DETAIL_HEADER_OVERLAY,
                    StarmapVisualTheme.DETAIL_SECTION_EDGE);
            int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
            draw.fillVirtual(draw.virtual(x + 2), draw.virtual(y + 3),
                    draw.virtual(x + 2) + stroke, draw.virtual(y + height - 3),
                    StarmapVisualTheme.ACCENT);
            StarmapTerminalPrimitives.drawSegmentedRail(draw,
                    x + 5, y + height - 3, Math.max(0, width - 10),
                    StarmapVisualTheme.DETAIL_DIVIDER);
        }

        int textX = x + 5;
        int availableWidth = width - (layout.compact() ? 27 : 10);
        graphics.drawString(font, fitText(title.getString(), Math.max(1, availableWidth)),
                textX, y + 4, StarmapVisualTheme.TEXT_PRIMARY, true);
        graphics.drawString(font, fitText(subtitle.getString(), Math.max(1, width - 10)),
                textX, y + 15, StarmapVisualTheme.TEXT_SECONDARY, false);
        return y + height + StarmapVisualTheme.SECTION_GAP;
    }

    private void renderDescription(GuiGraphics graphics, Component description,
                                   int startY, int bottomY)
    {
        String text = description.getString();
        int availableHeight = bottomY - startY;
        int lineHeight = StarmapVisualTheme.DETAIL_DESCRIPTION_LINE_HEIGHT;
        int maximumLines = Math.max(0, (availableHeight - 3) / lineHeight);
        if (text.isEmpty() || maximumLines == 0)
            return;

        StarmapLayout.Bounds detail = layout.detail();
        int inset = StarmapVisualTheme.DETAIL_CONTENT_INSET;
        int x = detail.x() + inset;
        int width = detail.width() - inset * 2;
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            StarmapTerminalPrimitives.drawInsetSlot(draw,
                    x, startY, width, availableHeight,
                    StarmapVisualTheme.DETAIL_DESCRIPTION_SURFACE,
                    StarmapVisualTheme.DETAIL_SECTION_EDGE,
                    StarmapVisualTheme.SHELL_SHADOW);
        }

        int textX = x + 4;
        int textWidth = Math.max(1, width - 8);
        List<String> lines = wrappedDescription(text, textWidth);
        int visibleLines = Math.min(maximumLines, lines.size());
        for (int i = 0; i < visibleLines; i++)
        {
            String line = lines.get(i);
            if (i == visibleLines - 1 && visibleLines < lines.size())
                line = fitText(line + "…", textWidth);
            graphics.drawString(font, line, textX, startY + 3 + i * lineHeight,
                    StarmapVisualTheme.TEXT_BODY, false);
        }
    }

    private void renderSections(GuiGraphics graphics,
                                List<StarmapDetailSection> sections,
                                int startY, int contentBottom)
    {
        if (sections.isEmpty() || startY >= contentBottom)
            return;
        int y = startY;
        for (StarmapDetailSection section : sections)
        {
            int height = sectionHeight(section);
            if (y + height > contentBottom)
                break;
            renderSection(graphics, section, y, height);
            y += height + StarmapVisualTheme.SECTION_GAP;
        }
    }

    private void renderSection(GuiGraphics graphics, StarmapDetailSection section,
                               int y, int height)
    {
        StarmapLayout.Bounds detail = layout.detail();
        int inset = StarmapVisualTheme.DETAIL_CONTENT_INSET;
        int x = detail.x() + inset;
        int width = detail.width() - inset * 2;
        Component label = section.label().orElse(null);
        int labelWidth = label == null ? 0 : sectionLabelWidth(label, width);
        int toneColor = toneColor(section.lines().get(0).tone());

        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            StarmapTerminalPrimitives.drawInsetSlot(draw,
                    x, y, width, height,
                    StarmapVisualTheme.DETAIL_SECTION_SURFACE,
                    StarmapVisualTheme.DETAIL_SECTION_EDGE,
                    StarmapVisualTheme.SHELL_SHADOW);
            if (labelWidth > 0)
            {
                StarmapTerminalPrimitives.drawLabelCap(draw,
                        x + 1, y + 2, labelWidth,
                        StarmapVisualTheme.DETAIL_LABEL_SURFACE);
            }
            int stroke = StarmapVisualTheme.FRAME_STROKE_VIRTUAL;
            draw.fillVirtual(draw.virtual(x + width - 2), draw.virtual(y + 2),
                    draw.virtual(x + width - 2) + stroke, draw.virtual(y + height - 2),
                    toneColor);
        }

        if (label != null)
        {
            graphics.drawString(font,
                    fitText(label.getString(), Math.max(1, labelWidth - 6)),
                    x + 4, y + 2, StarmapVisualTheme.TEXT_SECONDARY, false);
        }
        int lineX = x + (labelWidth > 0 ? labelWidth + 4 : 4);
        int lineWidth = Math.max(1, width - (lineX - x) - 6);
        for (int i = 0; i < section.lines().size(); i++)
        {
            StarmapDetailLine line = section.lines().get(i);
            graphics.drawString(font, fitText(line.text().getString(), lineWidth),
                    lineX, y + 2 + i * StarmapVisualTheme.DETAIL_LINE_HEIGHT,
                    toneColor(line.tone()), false);
        }
    }

    private int totalSectionHeight(List<StarmapDetailSection> sections)
    {
        if (sections.isEmpty())
            return 0;
        int height = 0;
        for (StarmapDetailSection section : sections)
            height += sectionHeight(section);
        return height + (sections.size() - 1) * StarmapVisualTheme.SECTION_GAP;
    }

    private static int sectionHeight(StarmapDetailSection section)
    {
        return Math.max(StarmapVisualTheme.DETAIL_SECTION_MIN_HEIGHT,
                2 + section.lines().size() * StarmapVisualTheme.DETAIL_LINE_HEIGHT);
    }

    private int sectionLabelWidth(Component label, int sectionWidth)
    {
        int preferred = font.width(label) + 7;
        int maximum = Math.min(StarmapVisualTheme.DETAIL_LABEL_MAX_WIDTH,
                Math.max(1, sectionWidth / 3));
        return Math.min(maximum,
                Math.max(StarmapVisualTheme.DETAIL_LABEL_MIN_WIDTH, preferred));
    }

    private static int toneColor(StarmapDetailLine.Tone tone)
    {
        return switch (tone)
        {
            case PRIMARY -> StarmapVisualTheme.TEXT_PRIMARY;
            case SECONDARY -> StarmapVisualTheme.TEXT_SECONDARY;
            case BODY -> StarmapVisualTheme.TEXT_BODY;
            case CURRENT -> StarmapVisualTheme.STATUS_CURRENT;
            case VISITED -> StarmapVisualTheme.STATUS_VISITED;
            case ATTENTION -> StarmapVisualTheme.STATUS_ATTENTION;
            case DANGER -> StarmapVisualTheme.STATUS_DANGER;
            case FUEL -> StarmapVisualTheme.STATUS_FUEL;
            case DISABLED -> StarmapVisualTheme.TEXT_DISABLED;
        };
    }

    private String fitText(String text, int maximumWidth)
    {
        if (font.width(text) <= maximumWidth)
            return text;
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > maximumWidth)
            end--;
        return end == 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }

    /** Wraps text to a pixel width while honoring explicit newlines. */
    private List<String> wrappedDescription(String text, int maximumWidth)
    {
        if (text.equals(cachedDescriptionText) && maximumWidth == cachedDescriptionWidth)
            return cachedDescriptionLines;
        cachedDescriptionText = text;
        cachedDescriptionWidth = maximumWidth;
        cachedDescriptionLines = wrapText(text, maximumWidth);
        return cachedDescriptionLines;
    }

    private List<String> wrapText(String text, int maximumWidth)
    {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n"))
        {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++)
            {
                char character = paragraph.charAt(i);
                String candidate = line.toString() + character;
                if (font.width(candidate) > maximumWidth && line.length() > 0)
                {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(character);
            }
            if (line.length() > 0)
                lines.add(line.toString());
        }
        return List.copyOf(lines);
    }
}
