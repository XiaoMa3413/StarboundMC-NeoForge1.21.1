package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Graphite console keys with pixel edge relief. LDLib2 owns hit testing and focus. */
final class NovaTerminalButton extends Button {
    private Runnable action = () -> {};

    NovaTerminalButton() {
        setFocusable(true);
        setOverflowVisible(false);
        layout(l -> l.paddingHorizontal(8).paddingVertical(3).alignItems(AlignItems.CENTER));
        text.setAllowHitTest(false);
        text.setOverflowVisible(false);
        text.layout(l -> l.flex(1).minWidth(0).heightPercent(100).marginHorizontal(0));
        textStyle(s -> s.adaptiveWidth(false).textWrap(TextWrap.WRAP)
                .textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).lineSpacing(1));
        addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isActive()) {
                action.run();
                event.stopPropagation();
            }
        });
        addEventListener(UIEvents.KEY_DOWN, event -> {
            if (isFocused() && isActive() && (event.keyCode == GLFW.GLFW_KEY_ENTER
                    || event.keyCode == GLFW.GLFW_KEY_KP_ENTER || event.keyCode == GLFW.GLFW_KEY_SPACE)) {
                action.run();
                event.stopPropagation();
            }
        });
    }

    NovaTerminalButton onActivate(Runnable action) {
        this.action = action;
        return this;
    }

    @Override
    public void drawBackgroundTexture(GUIContext context) {
        int x = Mth.floor(getPositionX()), y = Mth.floor(getPositionY());
        int w = Mth.floor(getSizeWidth()), h = Mth.floor(getSizeHeight());
        if (w < 4 || h < 4) return;
        boolean active = isActive();
        boolean hover = active && getState() == State.HOVERED;
        boolean pressed = active && getState() == State.PRESSED;
        boolean selected = hasClass("command-selected") || hasClass("ship-ai-topic-current");
        GuiGraphics g = context.graphics;
        if (hasClass("command-nav-button")) {
            if (selected || hover || pressed) g.fill(x, y, x + w, y + h,
                    pressed ? 0xff132832 : selected ? 0xff1c333e : 0xff192e38);
            if (selected) g.fill(x + 9, y + h - 2, x + w - 9, y + h, 0xff63d8d4);
        } else if (hasClass("command-task-row")) {
            if (selected || hover) g.fill(x, y, x + w, y + h, selected ? 0xff142b34 : 0xff10232c);
            g.fill(x, y + h - 1, x + w, y + h, 0xff29414b);
            if (selected) g.fill(x, y + 7, x + 2, y + h - 7, 0xff63d8d4);
        } else if (hasClass("command-link") || hasClass("command-filter")) {
            if ((hasClass("command-filter") && selected) || hover || pressed)
                g.fill(x + 2, y + h - 1, x + w - 2, y + h, selected ? 0xff63d8d4 : 0xff526f7b);
        } else {
            int fill = !active ? 0xff142129 : pressed ? 0xff122932
                    : hover ? 0xff2b4652 : hasClass("command-primary") ? 0xff203e49 : 0xff1b303c;
            int upper = !active ? 0xff344b55 : pressed ? 0xff070f15 : 0xff526975;
            int lower = pressed ? 0xff526975 : 0xff070f15;
            g.fill(x, y, x + w, y + h, fill);
            g.fill(x, y, x + w, y + 1, upper);
            g.fill(x, y + 1, x + 1, y + h, upper);
            g.fill(x + 1, y + h - 1, x + w, y + h, lower);
            g.fill(x + w - 1, y + 1, x + w, y + h - 1, lower);
            if (active && (selected || hasClass("command-primary")))
                g.fill(x + 6, y + 6, x + 8, y + h - 6, 0xff63d8d4);
        }
        if (active && isFocused()) {
            int color = 0xff63d8d4;
            g.fill(x + 1, y + 1, x + w - 1, y + 2, color);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, color);
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        // The surface is drawn outside the content scissor, keeping 1px edges intact.
    }
}
