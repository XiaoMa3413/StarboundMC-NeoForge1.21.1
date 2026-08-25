package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import net.minecraft.network.chat.Component;

/** LDLib2 chrome layer: frame, level marker and the contextual back hint. */
final class StarmapChromeElement extends UIElement {
    private static final int FRAME = 0xFF24485B;
    private static final int ACCENT = 0xFF63E2DF;
    private static final int MUTED = 0xFF8CA2B3;

    private final StarmapTerminalRoot root;
    private final Label levelLabel = new Label();
    private final Label backHint = new Label();

    StarmapChromeElement(StarmapTerminalRoot root) {
        this.root = root;
        addClasses("starmap-chrome", "starmap-layer");
        layout(layout -> layout.widthPercent(100).heightPercent(100)
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE));
        setAllowHitTest(false);

        UIElement frame = new UIElement().addClass("starmap-frame")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .left(8).top(8).right(8).bottom(8))
                .style(style -> style.backgroundTexture(new ColorBorderTexture(2, FRAME)));
        UIElement innerFrame = new UIElement().addClass("starmap-frame-inner")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .left(12).top(12).right(12).bottom(12))
                .style(style -> style.backgroundTexture(new ColorBorderTexture(1, 0x66305C70)));
        UIElement separator = new UIElement().addClass("starmap-header-rule")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .left(22).top(31).right(22).height(1))
                .style(style -> style.backgroundTexture(SDFRectTexture.of(0x664B8EA0)));

        levelLabel.addClass("starmap-level-label");
        levelLabel.layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(22).top(16).width(150).height(12));
        levelLabel.textStyle(style -> style.textColor(ACCENT).fontSize(9)
                .textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER)
                .textShadow(true));

        backHint.addClass("starmap-back-hint");
        backHint.layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .right(22).top(16).width(180).height(12));
        backHint.textStyle(style -> style.textColor(MUTED).fontSize(7)
                .textAlignHorizontal(Horizontal.RIGHT).textAlignVertical(Vertical.CENTER));

        addChildren(frame, innerFrame, separator, levelLabel, backHint);
        addEventListener(UIEvents.TICK, event -> refresh());
        refresh();
    }

    void refresh() {
        levelLabel.setText(root.levelLabel());
        boolean canGoBack = root.getLevel() != StarmapLevel.GALAXY;
        backHint.setDisplay(canGoBack);
        if (canGoBack)
            backHint.setText(Component.translatable("gui.starboundmc.starmap.redraw.back_hint"));
    }
}
