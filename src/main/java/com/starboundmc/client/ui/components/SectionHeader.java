package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.network.chat.Component;

/**
 * Lightweight section heading. It deliberately avoids a panel/card background:
 * hierarchy comes from typography, whitespace and a single separator rule.
 */
public final class SectionHeader extends UIElement {
    private final Label title = new Label();
    private final int width;

    public SectionHeader(Component text, int width) {
        this.width = width;
        addClass("sb-section-header");
        setAllowHitTest(false);
        layout(layout -> layout.width(width).height(10));

        title.setText(text);
        title.addClass("sb-section-title");
        title.setAllowHitTest(false);
        title.setOverflowVisible(false);
        title.layout(layout -> layout.widthPercent(100).height(9));
        title.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        addChild(title);
    }

    public SectionHeader setText(Component text) {
        title.setText(text);
        return this;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY() + 9);
        context.graphics.fill(x, y, x + width, y + 1, 0x362F5963);
    }
}
