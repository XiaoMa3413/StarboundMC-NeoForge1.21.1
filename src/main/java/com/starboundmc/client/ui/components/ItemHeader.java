package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

/**
 * The selected item's identity in words: its name, and how many units one craft yields.
 *
 * <p>It deliberately draws no icon of its own: {@link OutputPreviewSlot} already shows the item at
 * the output slot's own coordinate, and a second copy here would be the duplication this screen is
 * meant to be rid of. The header's leading cell is reserved for that socket, so the text starts
 * past it instead of running under the slot.
 */
public final class ItemHeader extends UIElement {
    private static final int NAME_H = 11;
    private static final int YIELD_H = 8;
    /** Room for the socket the output slot draws into, plus the gap after it. */
    private static final int SOCKET_CELL = 22;

    private final Label name = new Label();
    private final Label yield = new Label();

    /**
     * @param width             the header's width
     * @param reserveSocketCell leave room at the left for the real output socket
     */
    public ItemHeader(int width, boolean reserveSocketCell) {
        addClass("sb-item-header");
        setOverflowVisible(false);
        layout(layout -> layout.width(width).height(height()).flexDirection(FlexDirection.ROW));

        if (reserveSocketCell) {
            var cell = new UIElement().addClass("sb-socket-cell");
            cell.setAllowHitTest(false);
            cell.layout(layout -> layout.width(SOCKET_CELL).heightPercent(100));
            addChild(cell);
        }

        name.addClass("sb-item-name");
        yield.addClass("sb-item-count");
        configure(name, NAME_H);
        configure(yield, YIELD_H);
        var text = new UIElement().addClass("sb-item-header-text");
        text.setOverflowVisible(false);
        text.layout(layout -> layout.flexGrow(1).height(height()).flexDirection(FlexDirection.COLUMN));
        text.addChildren(name, yield);
        addChild(text);
    }

    /** Show one item: its name, the yield line, and its explain tooltip. */
    public ItemHeader setItem(Component itemName, Component yieldLine, Component tooltip) {
        name.setText(itemName);
        yield.setText(yieldLine);
        style(style -> style.tooltips(tooltip));
        return this;
    }

    /** Nothing selected: say so rather than leaving a stale item on screen. */
    public ItemHeader clear(Component placeholder) {
        name.setText(placeholder);
        yield.setText(Component.empty());
        return this;
    }

    /** Total height this header occupies, so a layout can reserve room for it. */
    public static int height() {
        return NAME_H + YIELD_H;
    }

    private static void configure(Label label, int height) {
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.layout(layout -> layout.widthPercent(100).height(height));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
    }
}
