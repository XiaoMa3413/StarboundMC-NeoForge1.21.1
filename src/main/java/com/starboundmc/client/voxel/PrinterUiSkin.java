// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.voxel;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.starboundmc.recipe.PrintingCategory;
import net.minecraft.client.gui.GuiGraphics;

/** Printer-only edge relief and pixel symbols. All drawing stays inside existing hit bounds. */
final class PrinterUiSkin {
    private static final int EDGE = 0xFF344B55;
    private static final int LIGHT = 0xFF526975;
    private static final int SHADOW = 0xFF070F15;
    private static final int ACCENT = 0xFF63D8D4;

    private static final IGuiTexture ALL = glyph(0b110011, 0b110011, 0, 0, 0b110011, 0b110011);
    private static final IGuiTexture SURVIVAL = glyph(0b001100, 0b001100, 0b111111,
            0b111111, 0b001100, 0b001100);
    private static final IGuiTexture MATERIALS = glyph(0b001100, 0b011110, 0b110011,
            0b110011, 0b011110, 0b001100);
    private static final IGuiTexture MACHINES = glyph(0b010010, 0b111111, 0b100001,
            0b101101, 0b100001, 0b111111);
    private static final IGuiTexture BUILDING = glyph(0b111111, 0b001001, 0b111111,
            0b100100, 0b111111, 0b000000);
    static final IGuiTexture DOWN = glyph(0, 0b100001, 0b010010, 0b001100, 0, 0);
    static final IGuiTexture CLOSE = glyph(0b100001, 0b010010, 0b001100,
            0b001100, 0b010010, 0b100001);

    private PrinterUiSkin() {}

    static void apply(UIElement shell) {
        relief(shell, false, false);
        shell.select(".voxel-printing-category").forEach(element -> relief(element, false, true));
        for (String selector : new String[]{".sb-recipe-search", ".sb-quantity-value"}) {
            shell.select(selector).forEach(element -> relief(element, true, true));
        }
        for (String selector : new String[]{".player-slot-socket", ".hotbar-slot-socket", ".sb-output-slot"}) {
            shell.select(selector).forEach(element -> relief(element, true, false));
        }
        for (String selector : new String[]{".sb-stepper-button", ".sb-craft-button"}) {
            shell.select(selector, Button.class).forEach(button -> relief(button, false, false));
        }
        shell.select(".__scroller_head_button__", Button.class).forEach(button -> scrollArrow(button, true));
        shell.select(".__scroller_tail_button__", Button.class).forEach(button -> scrollArrow(button, false));
    }

    private static void scrollArrow(Button button, boolean up) {
        button.buttonStyle(style -> style
                .baseTexture(arrow(up, 0xFF8AA2AD))
                .hoverTexture(arrow(up, ACCENT))
                .pressedTexture(arrow(up, 0xFFD8E7EB)));
    }

    /** The scrollbar is only 3 logical pixels wide; keep both arrow rows inside that width. */
    private static IGuiTexture arrow(boolean up, int color) {
        return (graphics, mouseX, mouseY, x, y, width, height, partial) -> {
            if (width < 3 || height < 2) return;
            int left = Math.round(x + (width - 3) / 2);
            int top = Math.round(y + (height - 2) / 2);
            int tip = up ? top : top + 1;
            int base = up ? top + 1 : top;
            graphics.fill(left + 1, tip, left + 2, tip + 1, color);
            graphics.fill(left, base, left + 3, base + 1, color);
        };
    }

    private static void relief(UIElement element, boolean inset, boolean input) {
        element.style(style -> style.overlay((graphics, mouseX, mouseY, x, y, width, height, partial) -> {
            int left = Math.round(x);
            int top = Math.round(y);
            int right = Math.round(x + width);
            int bottom = Math.round(y + height);
            if (right - left < 3 || bottom - top < 3) return;
            boolean disabled = !element.isActive();
            boolean pressed = element instanceof Button button && button.getState() == Button.State.PRESSED;
            boolean recessed = inset || pressed;
            int upper = disabled ? EDGE : recessed ? SHADOW : LIGHT;
            int lower = disabled ? SHADOW : recessed ? EDGE : SHADOW;
            if (element.hasClass("sb-output-slot")) {
                upper = EDGE;
                lower = LIGHT;
            }
            if (input && element.isFocused()) {
                // Leave the TextField's focus overlay visible; only reinforce its baseline.
                graphics.fill(left, bottom - 1, right, bottom, ACCENT);
                return;
            }
            edges(graphics, left, top, right, bottom, upper, lower);
        }));
    }

    private static void edges(GuiGraphics graphics, int left, int top, int right, int bottom,
                              int upper, int lower) {
        graphics.fill(left, top, right, top + 1, upper);
        graphics.fill(left, top + 1, left + 1, bottom, upper);
        graphics.fill(left + 1, bottom - 1, right, bottom, lower);
        graphics.fill(right - 1, top + 1, right, bottom - 1, lower);
    }

    static void queueRow(UIElement row) {
        row.style(style -> style.overlay((graphics, mouseX, mouseY, x, y, width, height, partial) -> {
            int left = Math.round(x);
            int top = Math.round(y);
            int right = Math.round(x + width);
            int bottom = Math.round(y + height);
            graphics.fill(left, bottom - 1, right, bottom, 0xFF21333E);
            if (row.hasClass("voxel-queue-row-active")) {
                graphics.fill(left, top, left + 2, bottom - 1, ACCENT);
            }
        }));
    }

    static IGuiTexture category(PrintingCategory category) {
        return switch (category) {
            case ALL -> ALL;
            case SURVIVAL -> SURVIVAL;
            case MATERIALS -> MATERIALS;
            case MACHINES -> MACHINES;
            case BUILDING -> BUILDING;
        };
    }

    /** Six-pixel silhouettes centred at integer coordinates, with no filtered bitmap scaling. */
    private static IGuiTexture glyph(int... rows) {
        return (graphics, mouseX, mouseY, x, y, width, height, partial) -> {
            int left = Math.round(x + (width - 6) / 2);
            int top = Math.round(y + (height - rows.length) / 2);
            for (int row = 0; row < rows.length; row++) {
                for (int column = 0; column < 6; column++) {
                    if ((rows[row] & (1 << (5 - column))) != 0) {
                        graphics.fill(left + column, top + row, left + column + 1,
                                top + row + 1, 0xFF9CB8C3);
                    }
                }
            }
        };
    }
}
