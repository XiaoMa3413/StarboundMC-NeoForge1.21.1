package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Pixel-aligned console controls; LDLib2 still owns input, text and tooltips. */
final class NovaTerminalButton extends Button {
    @Override
    public void drawBackgroundTexture(GUIContext context) {
        // Replace Button's texture pass instead of painting over its default skin.
        int x = Mth.floor(getPositionX()), y = Mth.floor(getPositionY());
        int w = Mth.floor(getSizeWidth()), h = Mth.floor(getSizeHeight());
        if (w < 4 || h < 4) return;
        boolean active = isActive();
        boolean hover = active && getState() == State.HOVERED;
        boolean pressed = active && getState() == State.PRESSED;
        boolean selected = hasClass("command-selected") || hasClass("ship-ai-topic-current");
        boolean primary = hasClass("command-primary");
        boolean nav = hasClass("command-nav-button");
        int edge = !active ? 0xff30404a : selected ? 0xff8bddd0 : hover ? 0xff7ca9b3 : 0xff466370;
        int fill = !active ? 0xff142029 : pressed ? 0xff10252e : selected ? 0xff25464e
                : hover ? 0xff2b424f : 0xff1c303d;
        GuiGraphics g = context.graphics;
        if (nav) {
            if (selected || hover) g.fill(x, y + 5, x + 2, y + h - 5, edge);
            if (selected) g.fill(x + w - 5, y + h / 2 - 1, x + w - 3, y + h / 2 + 1, edge);
            return;
        }
        if (!primary || !active) {
            boolean task = hasClass("command-task-row");
            // Rows and secondary actions are typography-led, not filled tiles.
            if (task) {
                g.fill(x + 7, y + h - 1, x + w - 7, y + h, 0xff29404a);
                if (selected || hover) g.fill(x, y + 6, x + 2, y + h - 6, edge);
            } else if (selected || hover || pressed) {
                g.fill(x + 6, y + h - 2, x + w - 6, y + h - 1, edge);
            }
            return;
        }
        if (primary && active) {
            edge = hover ? 0xffd6f5e9 : 0xffa4d8c7;
            fill = pressed ? 0xff609d91 : hover ? 0xffacdcca : 0xff83beaa;
        }
        int cut = Math.min(4, Math.min(w / 4, h / 4));
        panel(g, x, y + 1, w, h - 1, cut, 0xff080f16);
        panel(g, x, y, w, h - 1, cut, edge);
        panel(g, x + 1, y + 1, w - 2, h - 3, Math.max(1, cut - 1), fill);
        // Short edge highlights give the panel structure without outlining every side brightly.
        if (active && !pressed) g.fill(x + cut + 1, y + 1, x + Math.min(w - cut - 1, w / 3), y + 2,
                primary ? 0xffc4eadb : 0xff527583);
        if (selected) g.fill(x + 2, y + cut, x + 4, y + h - cut - 1, 0xff9ce5d6);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        // Button's standard pass is inside the content scissor. Drawing the surface there
        // clips off borders and cut corners whenever the control has padding.
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h, int cut, int color) {
        if (w <= 0 || h <= 0) return;
        cut = Math.min(cut, Math.min(w / 2, h / 2));
        g.fill(x, y + cut, x + w, y + h - cut, color);
        for (int row = 0; row < cut; row++) {
            int inset = cut - row;
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }
}
