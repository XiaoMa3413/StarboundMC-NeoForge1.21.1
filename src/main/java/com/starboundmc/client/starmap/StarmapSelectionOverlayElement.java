package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.Objects;

/**
 * Draws selection brackets in a separate layer so layout re-resolution of an
 * orbiting node cannot make the marker snap independently of the body.
 */
final class StarmapSelectionOverlayElement extends UIElement {
    private final StarmapTerminalRoot root;
    private String selectedKey;
    private ISubscription selectionAnimation = StarmapUiAnimations.none();

    StarmapSelectionOverlayElement(StarmapTerminalRoot root) {
        this.root = root;
        addClass("starmap-selection-layer");
        layout(layout -> layout.widthPercent(100).heightPercent(100)
                .positionType(TaffyPosition.ABSOLUTE));
        style(style -> style.zIndex(18).color(0xFFFFFFFF));
        setAllowHitTest(false);
    }

    void refresh() {
        String nextKey = root.selectionTargetKey();
        if (Objects.equals(nextKey, selectedKey))
            return;
        selectedKey = nextKey;
        selectionAnimation.unsubscribe();
        if (nextKey != null) {
            selectionAnimation = StarmapUiAnimations.tint(this, 0x00FFFFFF, 0xFFFFFFFF,
                    StarmapUiAnimations.SELECTION_DURATION, null);
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        float alpha = ((getStyle().color() >>> 24) & 0xFF) / 255.0F;
        root.drawSelectionOverlay(context.graphics, alpha);
    }
}
