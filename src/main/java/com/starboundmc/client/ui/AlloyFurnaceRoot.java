package com.starboundmc.client.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.starboundmc.menu.AlloyFurnaceMenu;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;

/** Compact processing channel; the menu owns slots, coordinates and item interactions. */
public final class AlloyFurnaceRoot extends UIElement {
    private final AlloyFurnaceMenu menu;
    private final UIElement progressFill = box("voxel-refinery-arrow-fill", 1, 1, 0, 6);
    private final UIElement heatFill = box("fuel-gauge-fill", 1, 1, 0, 6);
    private final Label status = label(Component.empty(), "alloy-status", 79, 56, 87, 17);
    private int lastProgress = -1;
    private int lastHeat = -1;
    private String lastState = "";

    public AlloyFurnaceRoot(AlloyFurnaceMenu menu, int left, int top, Component title, Component inventoryTitle) {
        this.menu = menu;
        addClasses("machine-inventory-screen", "shipboard-machine");
        setAllowHitTest(false);
        layout(l -> l.widthPercent(100).heightPercent(100));
        var shell = box("inventory-machine-shell", left, top, 176, 176);
        shell.addClass("alloy-shell");
        var header = box("voxel-machine-header", 3, 3, 170, 12);
        header.addChildren(box("fuel-header-rail", 0, 0, 2, 12),
                label(title, "alloy-title", 7, 1, 159, 10));
        var process = box("alloy-process", 4, 16, 168, 61);
        shell.addChildren(header, process,
                label(Component.translatable("gui.starboundmc.alloy.input"), "machine-inventory-caption", 8, 20, 43, 10),
                label(Component.translatable("gui.starboundmc.alloy.fuel"), "machine-inventory-caption", 8, 55, 43, 10),
                label(Component.translatable("gui.starboundmc.alloy.output"), "machine-inventory-caption", 136, 37, 32, 10),
                label(inventoryTitle, "machine-inventory-caption", 8, 83, 156, 9));
        var progress = box("voxel-refinery-arrow", 79, 39, 30, 8);
        progress.addChild(progressFill);
        var heat = box("fuel-gauge", 56, 39, 16, 8);
        heat.addChild(heatFill);
        shell.addChildren(progress, heat, status);
        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.getSlot(i);
            var socket = box("machine-slot-socket", slot.x - 1, slot.y - 1, 18, 18);
            if (i >= 30) socket.addClass("hotbar-slot-socket");
            shell.addChild(socket);
        }
        addChild(shell);
        MachineUiSkin.shell(shell);
        MachineUiSkin.slots(shell, ".machine-slot-socket");
        refresh();
    }

    public void refresh() {
        int progress = percent(menu.getCookingProgress(), menu.getCookingTotalTime());
        int heat = percent(menu.getLitTime(), menu.getLitDuration());
        String state = menu.getLitTime() > 0 && menu.getCookingProgress() > 0 ? "working"
                : menu.getSlot(0).getItem().isEmpty() ? "empty" : "idle";
        if (progress == lastProgress && heat == lastHeat && state.equals(lastState)) return;
        lastProgress = progress;
        lastHeat = heat;
        lastState = state;
        progressFill.layout(l -> l.width(28F * progress / 100));
        heatFill.layout(l -> l.width(14F * heat / 100));
        Component text = Component.translatable("gui.starboundmc.alloy." + state, progress);
        status.setText(text);
        status.style(s -> s.tooltips(text, Component.translatable("gui.starboundmc.alloy.hint")));
    }

    private static int percent(int value, int total) {
        return total <= 0 ? 0 : (int) Math.clamp(100L * value / total, 0, 100);
    }

    private static UIElement box(String style, int x, int y, int width, int height) {
        var element = new UIElement().addClass(style);
        element.setAllowHitTest(false);
        element.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(width).height(height));
        return element;
    }

    private static Label label(Component text, String style, int x, int y, int width, int height) {
        var label = new Label();
        label.addClass(style);
        label.setText(text);
        label.setOverflowVisible(false);
        label.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(width).height(height));
        label.textStyle(s -> s.adaptiveWidth(false).adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        label.style(s -> s.tooltips(text));
        return label;
    }
}
