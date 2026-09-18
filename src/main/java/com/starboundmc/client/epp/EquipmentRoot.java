// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.starboundmc.epp.EppMenu;
import com.starboundmc.epp.EppConfig;
import com.starboundmc.epp.LifeSupportMenu;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;

/** One instrument panel, one vanilla slot interaction path, retained text updates. */
public final class EquipmentRoot extends UIElement {
    private final Label status;
    private final Label chassis;
    private int lastGeneration = -1;
    private Component lastStatus = Component.empty();
    private final LifeSupportMenu station;
    public EquipmentRoot(int left, int top, Component title, LifeSupportMenu station) {
        this.station = station;
        addClass("epp-screen"); setAllowHitTest(false);
        layout(l -> l.widthPercent(100).heightPercent(100));
        var shell = box("epp-shell", left, top, EppMenu.WIDTH, EppMenu.HEIGHT);
        shell.addChild(box("epp-accent", 0, 0, 3, 26));
        shell.addChild(label(title, "epp-title", 12, 10, 224, 12));
        shell.addChild(box("epp-line", 12, 29, 224, 1));
        shell.addChild(box("epp-socket", 27, 46, 22, 22));
        chassis = label(Component.literal(station == null ? "EPP" : "O₂"), "epp-caption", 20, 77, 42, 12);
        shell.addChild(chassis);
        status = label(Component.empty(), "epp-status", 68, 42, 164, 24);
        shell.addChild(status);
        shell.addChild(label(Component.translatable(station == null ? "gui.starboundmc.epp.equipment_hint" : "gui.starboundmc.epp.station_hint"),
                "epp-caption", 68, 74, 164, 34));
        shell.addChild(box("epp-line", 12, 111, 224, 1));
        shell.addChild(label(Component.translatable("container.inventory"), "epp-caption", 12, 116, 210, 10));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) shell.addChild(box("epp-slot", 43 + col * 18, 127 + row * 18, 18, 18));
        for (int col = 0; col < 9; col++) shell.addChild(box("epp-slot", 43 + col * 18, 185, 18, 18));
        addChild(shell); refresh();
    }
    public void refresh() {
        var snapshot = EppClientState.snapshot;
        int generation = snapshot == null ? 0 : snapshot.generation();
        if (station == null && generation != lastGeneration) {
            chassis.setText(Component.literal(generation == 3 ? "MK.III" : generation == 2 ? "MK.II" : generation == 1 ? "MK.I" : "EPP"));
            lastGeneration = generation;
        }
        Component next = station != null ? Component.translatable(!station.supplied() ? "gui.starboundmc.epp.no_air"
                : station.getSlot(0).getItem().is(com.starboundmc.item.ModItems.OXYGEN_CANISTER.get()) ? "gui.starboundmc.epp.ready"
                : station.progress() > 0 ? "gui.starboundmc.epp.filling" : "gui.starboundmc.epp.insert", station.progress() * 100 / EppConfig.FILL_TICKS)
                : snapshot == null || !snapshot.equipped() ? Component.translatable("gui.starboundmc.epp.empty")
                : Component.translatable("tooltip.starboundmc.epp.oxygen", snapshot.oxygen(), snapshot.capacity());
        if (!next.equals(lastStatus)) { status.setText(next); lastStatus = next; }
    }
    private static UIElement box(String cls, int x, int y, int w, int h) {
        var e = new UIElement().addClass(cls); e.setAllowHitTest(false);
        e.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(w).height(h)); return e;
    }
    private static Label label(Component text, String cls, int x, int y, int w, int h) {
        var e = new Label(); e.setText(text); e.addClass(cls); e.setAllowHitTest(false); e.setOverflowVisible(false);
        e.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(w).height(h));
        e.textStyle(s -> s.adaptiveWidth(false).textWrap(TextWrap.WRAP)
                .textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.LEFT)
                .textAlignVertical(com.lowdragmc.lowdraglib2.gui.ui.data.Vertical.TOP)); return e;
    }
}
