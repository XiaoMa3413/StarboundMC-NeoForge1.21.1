// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.starmap;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.starboundmc.client.space.RelayClientState;
import com.starboundmc.encounter.RelayData;
import net.minecraft.network.chat.Component;

/** Discovery shortcut; encounter commands live in the selected POI's detail panel. */
final class RelaySignalPanel extends UIElement {
    private final StarmapTerminalRoot root;
    RelaySignalPanel(StarmapTerminalRoot root) {
        this.root = root;
        layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE).left(22).bottom(20).width(150).height(22));
        style(s -> s.zIndex(25));
        stopInteractionEventsPropagation();
        var action = new Button();
        action.layout(l -> l.widthPercent(100).heightPercent(100));
        action.setText(Component.translatable("gui.starboundmc.relay.locate"));
        action.textStyle(s -> s.fontSize(8).textColor(0xFFF0BC68));
        action.buttonStyle(s -> s.baseTexture(SDFRectTexture.of(0xE00A1721)).hoverTexture(SDFRectTexture.of(0xFF294D5D)));
        action.setOnClick(event -> {
            root.locateRelay();
            event.stopPropagation();
        });
        addChild(action);
        setDisplay(false);
    }
    void refresh() {
        var state = RelayClientState.snapshot;
        setDisplay(state != null && state.phase() != RelayData.Phase.UNDISCOVERED.ordinal()
                && !root.isInfoPanelVisible());
    }
}
