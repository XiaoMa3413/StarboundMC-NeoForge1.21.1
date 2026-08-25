package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.vfyjxf.taffy.style.TaffyPosition;

/**
 * Draws selection brackets in a separate layer so layout re-resolution of an
 * orbiting node cannot make the marker snap independently of the body.
 */
final class StarmapSelectionOverlayElement extends UIElement {
    private final StarmapTerminalRoot root;

    StarmapSelectionOverlayElement(StarmapTerminalRoot root) {
        this.root = root;
        addClass("starmap-selection-layer");
        layout(layout -> layout.widthPercent(100).heightPercent(100)
                .positionType(TaffyPosition.ABSOLUTE));
        style(style -> style.zIndex(18));
        setAllowHitTest(false);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        root.drawSelectionOverlay(context.graphics);
    }
}
