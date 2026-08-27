package com.starboundmc.client.fuel;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.starboundmc.menu.FuelControllerMenu;
import com.starboundmc.network.AddFuelPacket;
import com.starboundmc.network.ClientNetworkState;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.warp.ShipFuelService;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Stable LDLib2 component tree for the fuel controller. Vanilla owns all item slots. */
public final class FuelControllerRoot extends UIElement {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 220;

    private final UIElement gauge = new UIElement();
    private final UIElement gaugeFill = new UIElement();
    private final Label gaugeLabel = new Label();
    private final Button refuelButton = new Button();
    private int lastFuel = Integer.MIN_VALUE;
    private int lastMaxFuel = Integer.MIN_VALUE;

    public FuelControllerRoot(int left, int top, Component title, Component inventoryTitle) {
        addClass("machine-inventory-screen");
        setAllowHitTest(false);
        layout(layout -> layout.widthPercent(100).heightPercent(100));

        var shell = new UIElement().addClasses("inventory-machine-shell", "fuel-controller-shell");
        shell.setAllowHitTest(false);
        shell.setOverflowVisible(false);
        shell.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(left)
                .top(top)
                .width(PANEL_W)
                .height(PANEL_H));

        shell.addChildren(
                buildHeader(title),
                buildFuelConsole(),
                buildInventorySection(inventoryTitle));
        addChild(shell);
        refreshFuel();
    }

    private UIElement buildHeader(Component title) {
        var header = positioned("machine-inventory-header", 3, 3, 170, 21);
        var rail = positioned("fuel-header-rail", 0, 0, 3, 21);
        var titleLabel = label(title, "machine-inventory-title", 8, 5, 118, 10);

        var status = label(
                Component.translatable("gui.starboundmc.fuel_controller.system_ready"),
                "fuel-header-status", 126, 6, 39, 8);
        status.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        header.addChildren(rail, titleLabel, status);
        return header;
    }

    private UIElement buildFuelConsole() {
        var console = positioned("fuel-console", 4, 26, 168, 90);

        var gaugeCaption = label(
                Component.translatable("gui.starboundmc.fuel_controller.reserve"),
                "fuel-gauge-caption", 10, 5, 148, 8);

        gauge.addClass("fuel-gauge");
        gauge.setAllowHitTest(false);
        gauge.setOverflowVisible(false);
        gauge.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(24)
                .top(13)
                .width(120)
                .height(14));

        gaugeFill.addClass("fuel-gauge-fill");
        gaugeFill.setAllowHitTest(false);
        gaugeFill.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(2)
                .top(2)
                .widthPercent(0)
                .height(10));

        gaugeLabel.addClass("fuel-gauge-label");
        gaugeLabel.setAllowHitTest(false);
        gaugeLabel.setOverflowVisible(false);
        gaugeLabel.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(0)
                .widthPercent(100)
                .heightPercent(100));
        gaugeLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        gauge.addChildren(gaugeFill, gaugeTick(30), gaugeTick(60), gaugeTick(90), gaugeLabel);

        for (int col = 0; col < FuelControllerMenu.FUEL_SLOTS; col++) {
            console.addChild(slotSocket("fuel-slot-socket", 38 + col * 18, 29));
        }

        refuelButton.setText(Component.translatable("gui.starboundmc.fuel_controller.add"));
        refuelButton.addClass("fuel-refuel-button");
        refuelButton.text.setOverflowVisible(false);
        refuelButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(39)
                .top(52)
                .width(90)
                .height(16));
        refuelButton.textStyle(style -> style
                .adaptiveWidth(true)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        refuelButton.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && refuelButton.isActive()) {
                ModNetwork.sendToServer(new AddFuelPacket());
                event.stopPropagation();
            }
        });

        var localCost = label(
                Component.translatable("gui.starboundmc.starmap.fuel_cost_local",
                        ShipFuelService.WARP_FUEL_COST),
                "fuel-cost-label", 4, 72, 78, 8);
        var crossCost = label(
                Component.translatable("gui.starboundmc.starmap.fuel_cost_cross",
                        ShipFuelService.CROSS_SYSTEM_FUEL_COST),
                "fuel-cost-label", 86, 72, 78, 8);
        centerAndClip(localCost);
        centerAndClip(crossCost);

        console.addChildren(gaugeCaption, gauge, refuelButton, localCost, crossCost);
        return console;
    }

    private UIElement buildInventorySection(Component inventoryTitle) {
        var section = positioned("fuel-inventory-section", 4, 118, 168, 98);
        section.addChild(label(inventoryTitle, "machine-inventory-caption", 4, 4, 156, 9));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                section.addChild(slotSocket("player-slot-socket", 3 + col * 18, 17 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            section.addChild(slotSocket("hotbar-slot-socket", 3 + col * 18, 71));
        }
        return section;
    }

    public void refreshFuel() {
        int maxFuel = Math.max(1, ClientNetworkState.maxFuel());
        int fuel = Math.max(0, Math.min(maxFuel, ClientNetworkState.fuel()));
        if (fuel == lastFuel && maxFuel == lastMaxFuel) return;

        lastFuel = fuel;
        lastMaxFuel = maxFuel;
        gaugeFill.layout(layout -> layout.widthPercent(100F * fuel / maxFuel));
        gaugeLabel.setText(Component.translatable("gui.starboundmc.fuel", fuel, maxFuel));

        gauge.removeClass("fuel-gauge-low");
        gauge.removeClass("fuel-gauge-full");
        if (fuel >= maxFuel) {
            gauge.addClass("fuel-gauge-full");
        } else if (fuel * 4 <= maxFuel) {
            gauge.addClass("fuel-gauge-low");
        }

        boolean canRefuel = fuel < maxFuel;
        refuelButton.setActive(canRefuel);
        refuelButton.style(style -> style.tooltips(Component.translatable(canRefuel
                ? "gui.starboundmc.fuel_controller.tip"
                : "message.starboundmc.fuel.full")));
    }

    private static UIElement gaugeTick(int left) {
        return positioned("fuel-gauge-tick", left, 2, 1, 10);
    }

    private static UIElement slotSocket(String styleClass, int left, int top) {
        var slot = positioned("machine-slot-socket", left, top, 18, 18);
        slot.addClass(styleClass);
        return slot;
    }

    private static UIElement positioned(String styleClass, int left, int top, int width, int height) {
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

    private static Label label(Component text, String styleClass,
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
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        label.style(style -> style.tooltips(text));
        return label;
    }

    private static void centerAndClip(Label label) {
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
    }
}
