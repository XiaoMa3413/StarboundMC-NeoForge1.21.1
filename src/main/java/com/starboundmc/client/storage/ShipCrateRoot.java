package com.starboundmc.client.storage;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.starboundmc.menu.ShipCrateMenu;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;

/** Stable LDLib2 component tree for the 54-slot ship cargo locker. */
public final class ShipCrateRoot extends UIElement {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 247;

    public ShipCrateRoot(int left, int top, Component title, Component inventoryTitle) {
        addClass("machine-inventory-screen");
        setAllowHitTest(false);
        layout(layout -> layout.widthPercent(100).heightPercent(100));

        var shell = positioned("inventory-machine-shell", left, top, PANEL_W, PANEL_H);
        shell.addClass("ship-crate-shell");
        shell.setOverflowVisible(false);
        shell.addChildren(
                buildHeader(title),
                buildCargoGrid(),
                buildInventorySection(inventoryTitle));
        addChild(shell);
    }

    private UIElement buildHeader(Component title) {
        var header = positioned("crate-header", 3, 3, 170, 25);
        header.addChild(positioned("crate-header-index", 0, 0, 5, 25));
        header.addChild(label(title, "crate-title", 10, 6, 116, 10));

        var capacity = label(
                Component.translatable("gui.starboundmc.ship_crate.capacity", ShipCrateMenu.SLOTS),
                "crate-capacity", 126, 6, 38, 10);
        capacity.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        header.addChild(capacity);
        return header;
    }

    private UIElement buildCargoGrid() {
        var section = positioned("crate-cargo-section", 4, 31, 168, 112);
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                section.addChild(slotSocket("cargo-slot-socket", 3 + col * 18, 2 + row * 18));
            }
        }
        return section;
    }

    private UIElement buildInventorySection(Component inventoryTitle) {
        var section = positioned("crate-inventory-section", 4, 147, 168, 96);
        section.addChild(label(inventoryTitle, "machine-inventory-caption", 4, 4, 156, 9));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                section.addChild(slotSocket("player-slot-socket", 3 + col * 18, 16 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            section.addChild(slotSocket("hotbar-slot-socket", 3 + col * 18, 70));
        }
        return section;
    }

    private static UIElement slotSocket(String styleClass, int left, int top) {
        var slot = positioned("machine-slot-socket", left, top, 18, 18);
        slot.addClass(styleClass);
        return slot;
    }

    private static UIElement positioned(String styleClass, int left, int top, int width, int height) {
        var element = new UIElement().addClass(styleClass);
        element.setAllowHitTest(false);
        element.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(left)
                .top(top)
                .width(width)
                .height(height));
        return element;
    }

    private static Label label(Component text, String styleClass,
                               int left, int top, int width, int height) {
        var label = new Label();
        label.setText(text);
        label.addClass(styleClass);
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(left)
                .top(top)
                .width(width)
                .height(height));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        label.style(style -> style.tooltips(text));
        return label;
    }
}
