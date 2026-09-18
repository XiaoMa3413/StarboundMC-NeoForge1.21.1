// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.starmap;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.client.space.RelayClientState;
import com.starboundmc.encounter.RelayData;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.RelayActionPacket;
import net.minecraft.network.chat.Component;

/** Separate signal entry, not a fake celestial body or surface destination. */
final class RelaySignalPanel extends UIElement {
    private final Label status = new Label();
    private final Button action = new Button();
    private final int containerId;
    RelaySignalPanel(int containerId) {
        this.containerId = containerId;
        layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE).left(22).bottom(20).width(196).height(80).paddingAll(5).gapAll(4));
        style(s -> s.backgroundTexture(SDFRectTexture.of(0xE00A1721)));
        stopInteractionEventsPropagation();
        status.layout(l -> l.widthPercent(100).height(44));
        status.textStyle(s -> s.fontSize(7).textColor(0xFFA4D8DE).textWrap(TextWrap.WRAP));
        status.setAllowHitTest(false);
        action.layout(l -> l.widthPercent(100).height(22));
        action.textStyle(s -> s.fontSize(8).textColor(0xFF63E2DF));
        action.buttonStyle(s -> s.baseTexture(SDFRectTexture.of(0xFF1B3542)).hoverTexture(SDFRectTexture.of(0xFF294D5D)));
        action.setOnClick(event -> {
            var state = RelayClientState.snapshot;
            if (state != null) ModNetwork.sendToServer(new RelayActionPacket(containerId, state.phase() == RelayData.Phase.ACTIVE.ordinal()));
            event.stopPropagation();
        });
        addChildren(status, action);
        setDisplay(false);
    }
    void refresh(boolean engineOnline) {
        var state = RelayClientState.snapshot;
        setDisplay(state != null && state.phase() != RelayData.Phase.UNDISCOVERED.ordinal());
        if (state == null) return;
        String phase = state.phase() >= 0 && state.phase() < RelayData.Phase.values().length
                ? RelayData.Phase.values()[state.phase()].name().toLowerCase(java.util.Locale.ROOT) : "error";
        var mission = Component.translatable("gui.starboundmc.relay." + (state.completed() ? "complete" : state.recovered() ? "return" : "retrieve"));
        int fuel = java.util.Objects.equals(ClientPlanetState.getCurrentEntryId(), state.homeBody()) ? 0
                : com.starboundmc.warp.ShipWarpManager.warpFuelCost(ClientPlanetState.getCurrentEntryId(), state.homeBody());
        var phaseText = state.outsideCrew() > 0 ? Component.translatable("gui.starboundmc.relay.waiting", state.outsideCrew())
                : Component.translatable("gui.starboundmc.relay." + phase);
        status.setText(Component.translatable("gui.starboundmc.relay.status", phaseText, mission, fuel));
        boolean active = state.phase() == RelayData.Phase.ACTIVE.ordinal();
        action.setText(Component.translatable(active ? "gui.starboundmc.relay.leave" : "gui.starboundmc.relay.approach"));
        action.setActive(engineOnline && !ClientPlanetState.isWarping()
                && (active || state.phase() == RelayData.Phase.AVAILABLE.ordinal()));
    }
}
