package com.starboundmc.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared industrial-sci-fi pixel GUI palette + drawing helpers.
 *  The same visual language as the matter-manipulator upgrade workbench
 *  (UpgradeScreen): dark steel plates, 2px pixel frames, amber corner
 *  brackets, cyan accents and a "PWR" indicator block. */
public final class UiStyle
{
    public static final int C_BG_TOP = 0xFF171A20;
    public static final int C_BG_BOTTOM = 0xFF0D0F14;
    public static final int C_PLATE = 0xFF1F242C;
    public static final int C_BORDER = 0xFF3C4550;
    public static final int C_ACCENT = 0xFF3FD0E8;
    public static final int C_ACCENT_LIGHT = 0xFF8CE6F5;
    public static final int C_AMBER = 0xFFE8A33D;
    public static final int C_AMBER_LIGHT = 0xFFF0C273;
    public static final int C_DANGER = 0xFFFF6B5E;
    public static final int C_TEXT = 0xFFE8E8E8;
    public static final int C_DIM = 0xFF9AA0A6;
    public static final int C_SLOT_BG = 0xFF0B0E13;

    private UiStyle()
    {
    }

    /** Dark steel gradient panel + 2px pixel frame + amber corner brackets. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h)
    {
        g.fillGradient(x, y, x + w, y + h, C_BG_TOP, C_BG_BOTTOM);
        g.fill(x, y, x + w, y + 2, C_BORDER);
        g.fill(x, y + h - 2, x + w, y + h, C_BORDER);
        g.fill(x, y, x + 2, y + h, C_BORDER);
        g.fill(x + w - 2, y, x + w, y + h, C_BORDER);
        g.fill(x + 2, y + 2, x + 6, y + 6, C_AMBER);
        g.fill(x + w - 6, y + 2, x + w - 2, y + 6, C_AMBER);
        g.fill(x + 2, y + h - 6, x + 6, y + h - 2, C_AMBER);
        g.fill(x + w - 6, y + h - 6, x + w - 2, y + h - 2, C_AMBER);
    }

    /** Title + cyan rule + "PWR" indicator block (workbench header band). */
    public static void drawHeader(GuiGraphics g, Font font, int x, int y, Component title, int panelW)
    {
        g.drawString(font, title, x + 8, y + 7, C_TEXT, true);
        g.fill(x + 8, y + 19, x + 8 + font.width(title), y + 20, C_ACCENT);
        g.drawString(font, Component.literal("PWR"), x + panelW - 38, y + 7, C_DIM, true);
        g.fill(x + panelW - 8, y + 8, x + panelW - 4, y + 16, C_AMBER);
        g.fill(x + panelW - 12, y + 8, x + panelW - 8, y + 16, C_ACCENT);
    }

    /** Recessed 18x18 pixel slot socket (pass the slot position, x/y of the socket). */
    public static void drawSlot(GuiGraphics g, int x, int y)
    {
        g.fill(x, y, x + 18, y + 18, C_SLOT_BG);
        g.fill(x, y, x + 18, y + 1, C_BORDER);
        g.fill(x, y + 17, x + 18, y + 18, C_BORDER);
        g.fill(x, y, x + 1, y + 18, C_BORDER);
        g.fill(x + 17, y, x + 18, y + 18, C_BORDER);
    }

    /** Steel plate with a 1px border on the top and bottom edges. */
    public static void drawPlate(GuiGraphics g, int x, int y, int w, int h)
    {
        g.fill(x, y, x + w, y + h, C_PLATE);
        g.fill(x, y, x + w, y + 1, C_BORDER);
        g.fill(x, y + h - 1, x + w, y + h, C_BORDER);
    }

    /** Amber separator with a 1px steel rule above it. */
    public static void drawSeparator(GuiGraphics g, int x, int y, int w)
    {
        g.fill(x, y, x + w, y + 1, C_BORDER);
        g.fill(x, y + 1, x + w, y + 2, C_AMBER);
    }

    /** 14x14 fuel-burn gauge: recessed socket + amber fill scaled by lit/total. */
    public static void drawFuelBar(GuiGraphics g, int x, int y, int lit, int total)
    {
        g.fill(x, y, x + 14, y + 14, C_SLOT_BG);
        g.fill(x, y, x + 14, y + 1, C_BORDER);
        g.fill(x, y + 13, x + 14, y + 14, C_BORDER);
        g.fill(x, y, x + 1, y + 14, C_BORDER);
        g.fill(x + 13, y, x + 14, y + 14, C_BORDER);
        if (lit > 0 && total > 0)
        {
            int h = 12 * lit / total;
            g.fill(x + 1, y + 13 - h, x + 13, y + 13, C_AMBER);
            g.fill(x + 1, y + 13 - h, x + 13, y + 14 - h, C_AMBER_LIGHT);
        }
    }

    /** 24x12 smelting progress channel: cyan fill + light top edge + amber head tick. */
    public static void drawProgressBar(GuiGraphics g, int x, int y, int progress, int total)
    {
        drawHBar(g, x, y, 24, 12, progress, total);
    }

    /** Horizontal recessed channel with cyan fill, a light top edge and an amber head tick. */
    public static void drawHBar(GuiGraphics g, int x, int y, int w, int h, int value, int max)
    {
        g.fill(x, y, x + w, y + h, C_SLOT_BG);
        g.fill(x, y, x + w, y + 1, C_BORDER);
        g.fill(x, y + h - 1, x + w, y + h, C_BORDER);
        g.fill(x, y, x + 1, y + h, C_BORDER);
        g.fill(x + w - 1, y, x + w, y + h, C_BORDER);
        if (value > 0 && max > 0)
        {
            int fw = (w - 2) * value / max;
            g.fill(x + 1, y + 1, x + 1 + fw, y + h - 1, C_ACCENT);
            g.fill(x + 1, y + 1, x + 1 + fw, y + 2, C_ACCENT_LIGHT);
            g.fill(x + fw, y + h / 2 - 1, x + 1 + fw, y + h / 2 + 1, C_AMBER);
        }
    }
}
