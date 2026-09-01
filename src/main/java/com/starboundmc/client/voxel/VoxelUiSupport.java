package com.starboundmc.client.voxel;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;

final class VoxelUiSupport {
    private VoxelUiSupport() {
    }

    static UIElement positioned(String styleClass, int left, int top, int width, int height) {
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

    static UIElement slotSocket(String styleClass, int left, int top) {
        var slot = positioned("machine-slot-socket", left, top, 18, 18);
        slot.addClass(styleClass);
        return slot;
    }

    static Label label(Component text, String styleClass,
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
                .adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        label.style(style -> style.tooltips(text));
        return label;
    }

    static void center(Label label) {
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
    }
}
