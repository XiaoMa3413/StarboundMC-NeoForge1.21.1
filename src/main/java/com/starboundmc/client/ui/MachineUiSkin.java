// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.ui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;

/** Opt-in shipboard edge relief. Pages keep their geometry, fills and accent colours. */
public final class MachineUiSkin {
    private MachineUiSkin() {}

    public static void shell(UIElement element) { relief(element, false, false); }
    public static void button(Button element) { relief(element, false, false); }
    public static void input(UIElement element) { relief(element, true, true); }

    public static void slots(UIElement root, String... selectors) {
        for (String selector : selectors) root.select(selector).forEach(element -> relief(element, true, false));
    }

    private static void relief(UIElement element, boolean inset, boolean input) {
        element.style(style -> style.overlay((graphics, mouseX, mouseY, x, y, width, height, partial) -> {
            int left = Math.round(x), top = Math.round(y);
            int right = Math.round(x + width), bottom = Math.round(y + height);
            if (right - left < 3 || bottom - top < 3) return;
            if (input && element.isFocused()) {
                graphics.fill(left, bottom - 1, right, bottom, 0xFF63D8D4);
                return;
            }
            boolean pressed = element instanceof Button button && button.getState() == Button.State.PRESSED;
            boolean recessed = inset || pressed;
            int upper = !element.isActive() ? 0xFF344B55 : recessed ? 0xFF070F15 : 0xFF526975;
            int lower = !element.isActive() ? 0xFF070F15 : recessed ? 0xFF344B55 : 0xFF070F15;
            graphics.fill(left, top, right, top + 1, upper);
            graphics.fill(left, top + 1, left + 1, bottom, upper);
            graphics.fill(left + 1, bottom - 1, right, bottom, lower);
            graphics.fill(right - 1, top + 1, right, bottom - 1, lower);
        }));
    }

    public static void scrollbars(UIElement root) {
        root.select(".__scroller_head_button__", Button.class).forEach(button -> arrow(button, true));
        root.select(".__scroller_tail_button__", Button.class).forEach(button -> arrow(button, false));
    }

    private static void arrow(Button button, boolean up) {
        button.buttonStyle(style -> style.baseTexture(arrow(up, 0xFF8AA2AD))
                .hoverTexture(arrow(up, 0xFF63D8D4)).pressedTexture(arrow(up, 0xFFD8E7EB)));
    }

    private static IGuiTexture arrow(boolean up, int color) {
        return (graphics, mouseX, mouseY, x, y, width, height, partial) -> {
            if (width < 3 || height < 2) return;
            int left = Math.round(x + (width - 3) / 2), top = Math.round(y + (height - 2) / 2);
            int tip = up ? top : top + 1, base = up ? top + 1 : top;
            graphics.fill(left + 1, tip, left + 2, tip + 1, color);
            graphics.fill(left, base, left + 3, base + 1, color);
        };
    }
}
