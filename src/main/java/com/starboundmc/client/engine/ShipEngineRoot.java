package com.starboundmc.client.engine;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.starboundmc.menu.ShipEngineMenu;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Retained instrument panel. Decorative elements never intercept vanilla slot gestures. */
public final class ShipEngineRoot extends UIElement {
    private final ShipEngineMenu menu;
    private final Label status;
    private final Label hyperdrive;
    private ShipEngineMenu.Status previous;
    private Boolean previousHyperdrive;

    public ShipEngineRoot(int left, int top, ShipEngineMenu menu, Component title, Component inventoryTitle) {
        this.menu = menu;
        addClass("engine-screen");
        setAllowHitTest(false);
        layout(l -> l.widthPercent(100).heightPercent(100));
        var shell = box("engine-shell", left, top, ShipEngineMenu.WIDTH, ShipEngineMenu.HEIGHT);
        shell.setOverflowVisible(false);
        shell.addChild(box("engine-rail", 0, 0, 3, 26));
        shell.addChild(label(title, "engine-title", 12, 9, 198, 12));
        shell.addChild(label(Component.literal("PROPULSION / 01"), "engine-code", 194, 12, 78, 9));
        shell.addChild(box("engine-divider", 12, 27, 256, 1));

        var reactor = new EngineReactorElement(menu);
        reactor.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(20).top(32).width(92).height(92));
        shell.addChild(reactor);
        shell.addChild(box("engine-socket", 57, 69, 18, 18));
        shell.addChild(label(Component.translatable("gui.starboundmc.engine.module"),
                "engine-caption", 30, 124, 82, 8));

        shell.addChild(label(Component.translatable("gui.starboundmc.engine.sublight"),
                "engine-subtitle", 126, 37, 140, 11));
        status = label(Component.empty(), "engine-status", 126, 53, 140, 39);
        status.textStyle(s -> s.textWrap(TextWrap.WRAP));
        shell.addChild(status);
        shell.addChild(label(Component.translatable("gui.starboundmc.engine.auto_install"),
                "engine-caption", 126, 97, 140, 20));
        hyperdrive = label(Component.empty(), "engine-caption", 126, 119, 140, 10);
        shell.addChild(hyperdrive);

        shell.addChild(box("engine-divider", 12, 132, 256, 1));
        shell.addChild(label(inventoryTitle, "engine-caption", 12, 138, 45, 18));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                shell.addChild(box("engine-inventory-slot", 59 + col * 18, 145 + row * 18, 18, 18));
        for (int col = 0; col < 9; col++)
            shell.addChild(box("engine-hotbar-slot", 59 + col * 18, 203, 18, 18));
        addChild(shell);
        refresh();
    }

    public void refresh() {
        if (previous != menu.status()) {
            previous = menu.status();
            status.setText(Component.translatable("gui.starboundmc.engine.status."
                    + previous.name().toLowerCase(Locale.ROOT)));
            status.removeClass("engine-status-online");
            status.removeClass("engine-status-igniting");
            if (previous == ShipEngineMenu.Status.ONLINE) status.addClass("engine-status-online");
            if (previous == ShipEngineMenu.Status.IGNITING) status.addClass("engine-status-igniting");
        }
        if (previousHyperdrive == null || previousHyperdrive != menu.hyperdriveOnline()) {
            previousHyperdrive = menu.hyperdriveOnline();
            hyperdrive.setText(Component.translatable(menu.hyperdriveOnline()
                    ? "gui.starboundmc.engine.hyperdrive_online" : "gui.starboundmc.engine.hyperdrive_offline"));
        }
    }

    private static UIElement box(String cls, int x, int y, int w, int h) {
        var element = new UIElement().addClass(cls);
        element.setAllowHitTest(false);
        element.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(w).height(h));
        return element;
    }

    private static Label label(Component text, String cls, int x, int y, int w, int h) {
        var label = new Label();
        label.setText(text);
        label.addClass(cls);
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(w).height(h));
        label.textStyle(s -> s.adaptiveWidth(false).textWrap(TextWrap.WRAP));
        return label;
    }
}
