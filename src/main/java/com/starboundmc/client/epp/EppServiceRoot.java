// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.starboundmc.epp.EppServiceMenu;
import com.starboundmc.client.ui.MachineUiSkin;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Vanilla menu owns all slots and transactions; this retained tree supplies labels and actions. */
public final class EppServiceRoot extends UIElement {
    private final EppServiceMenu menu;
    private final Label status;
    private final Button upgrade, install, remove;
    private Component lastStatus = Component.empty();
    public EppServiceRoot(int left, int top, EppServiceMenu menu) {
        this.menu = menu;
        addClasses("shipboard-machine", "epp-machine");
        layout(l -> l.widthPercent(100).heightPercent(100)); setAllowHitTest(false);
        var shell = new UIElement().addClass("epp-shell");
        shell.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(left).top(top).width(EppServiceMenu.WIDTH).height(EppServiceMenu.HEIGHT));
        shell.setAllowHitTest(false);
        shell.addChild(label("block.starboundmc.epp_service_station", "epp-title", 12, 10, 260, 14));
        String[] slots = {"pack", "module", "kit"};
        for (int i = 0; i < 3; i++) {
            var socket = new UIElement().addClass("epp-socket");
            int x = 41 + i * 90;
            socket.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(44).width(22).height(22));
            socket.setAllowHitTest(false); shell.addChild(socket);
            shell.addChild(label("gui.starboundmc.epp.service." + slots[i], "epp-caption", 14 + i * 90, 29, 82, 13));
        }
        upgrade = button("upgrade", 12, EppServiceMenu.UPGRADE);
        install = button("install", 102, EppServiceMenu.INSTALL);
        remove = button("remove", 192, EppServiceMenu.REMOVE);
        shell.addChildren(upgrade, install, remove);
        status = label("gui.starboundmc.epp.service.hint", "epp-caption", 12, 103, 260, 30);
        shell.addChild(status);
        shell.addChild(label("container.inventory", "epp-caption", 12, 137, 260, 11));
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) {
            int x = 61 + col * 18, y = row == 3 ? 207 : 149 + row * 18;
            var slot = new UIElement().addClass("epp-slot"); slot.setAllowHitTest(false);
            slot.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(18).height(18));
            shell.addChild(slot);
        }
        addChild(shell);
        MachineUiSkin.shell(shell);
        MachineUiSkin.slots(shell, ".epp-slot", ".epp-socket");
        refresh();
    }
    private Button button(String name, int x, int action) {
        var button = new Button(); button.setText(Component.translatable("gui.starboundmc.epp.service." + name));
        button.addClass("epp-service-button");
        MachineUiSkin.button(button);
        button.text.setAllowHitTest(false);
        button.text.setOverflowVisible(false);
        button.text.layout(l -> l.widthPercent(100).heightPercent(100).marginHorizontal(0));
        button.textStyle(style -> style.adaptiveWidth(false).textWrap(TextWrap.HIDE));
        button.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(76).width(80).height(20));
        button.setOnClick(event -> {
            var mc = Minecraft.getInstance();
            if (button.isActive() && mc.gameMode != null && mc.player != null && mc.player.containerMenu == menu)
                mc.gameMode.handleInventoryButtonClick(menu.containerId, action);
        });
        return button;
    }
    public void refresh() {
        boolean safe = menu.safeEnvironment();
        if (safe) status.removeClass("epp-service-unsafe"); else status.addClass("epp-service-unsafe");
        upgrade.setActive(safe && menu.canUpgrade());
        install.setActive(safe && menu.canInstall());
        remove.setActive(safe && menu.canRemove());
        describe(upgrade, safe, "upgrade");
        describe(install, safe, "install");
        describe(remove, safe, "remove");
        Component next = Component.translatable(safe ? "gui.starboundmc.epp.service.hint" : "gui.starboundmc.epp.service.unsafe");
        if (!next.equals(lastStatus)) { status.setText(next); status.style(style -> style.tooltips(next)); lastStatus = next; }
    }
    private void describe(Button button, boolean safe, String action) {
        String key = !safe ? "unsafe" : menu.getSlot(0).getItem().isEmpty() ? "insert_pack"
                : action + (button.isActive() ? "_ready" : "_requirements");
        button.style(style -> style.tooltips(Component.translatable("gui.starboundmc.epp.service." + key)));
    }
    private static Label label(String key, String cls, int x, int y, int width, int height) {
        var label = new Label(); label.setText(Component.translatable(key)); label.addClass(cls);
        label.style(style -> style.tooltips(Component.translatable(key))); label.setOverflowVisible(false);
        label.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(width).height(height));
        label.textStyle(style -> style.adaptiveWidth(false).textWrap(TextWrap.WRAP)
                .textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.LEFT)
                .textAlignVertical(com.lowdragmc.lowdraglib2.gui.ui.data.Vertical.TOP));
        return label;
    }
}
