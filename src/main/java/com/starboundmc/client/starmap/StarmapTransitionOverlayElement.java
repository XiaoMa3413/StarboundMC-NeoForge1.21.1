package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import dev.vfyjxf.taffy.style.TaffyPosition;

/** Lightweight scene curtain that avoids LDLib2's full-screen opacity layer. */
final class StarmapTransitionOverlayElement extends UIElement {
    private ISubscription animation = StarmapUiAnimations.none();

    StarmapTransitionOverlayElement() {
        addClass("starmap-transition-layer");
        layout(layout -> layout.widthPercent(100).heightPercent(100)
                .positionType(TaffyPosition.ABSOLUTE));
        style(style -> style.zIndex(19)
                .backgroundTexture(SDFRectTexture.of(0xFF050912))
                .color(0x00FFFFFF));
        setAllowHitTest(false);
        setDisplay(false);
    }

    void play() {
        animation.unsubscribe();
        setDisplay(true);
        animation = StarmapUiAnimations.tint(this, 0xA8FFFFFF, 0x00FFFFFF,
                StarmapUiAnimations.LEVEL_DURATION, () -> setDisplay(false));
    }
}
