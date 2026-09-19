package com.starboundmc.client.epp;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.starboundmc.epp.EppItem;
import com.starboundmc.epp.EppMenu;
import com.starboundmc.item.ModDataComponents;
import com.starboundmc.story.ModAttachments;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemContainerContents;

/** Equipment readouts use synchronized menu items; vanilla owns all item slots. */
public final class EquipmentConfigRoot extends UIElement {
    private final EppMenu menu;
    private final Label packName, oxygen, modules, rigName, rigStatus;
    private final ProgressBar tank = new ProgressBar();

    public EquipmentConfigRoot(int left, int top, EppMenu menu) {
        this.menu = menu;
        addClass("equipment-config"); setAllowHitTest(false);
        layout(l -> l.widthPercent(100).heightPercent(100));
        var shell = box("equipment-shell", left, top, EppMenu.WIDTH, EppMenu.HEIGHT);
        shell.addChild(text(Component.translatable("gui.starboundmc.equipment.title"), "equipment-title", 12, 10, 224, 13));
        shell.addChild(box("equipment-divider", 12, 29, 224, 1));
        shell.addChild(text(Component.literal("EPP"), "equipment-caption", 17, 34, 40, 9));
        shell.addChild(box("equipment-socket", 27, 46, 22, 22));
        packName = text(Component.empty(), "equipment-name", 60, 35, 174, 10);
        oxygen = text(Component.empty(), "equipment-detail", 60, 48, 174, 10);
        modules = text(Component.empty(), "equipment-muted", 60, 60, 174, 9);
        tank.addClass("equipment-tank");
        tank.setAllowHitTest(false);
        tank.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(60).top(72).width(174).height(3));
        tank.barContainer.layout(l -> l.paddingAll(0));
        tank.label.setDisplay(false);
        shell.addChildren(packName, oxygen, modules, tank);
        shell.addChild(box("equipment-divider", 12, 79, 224, 1));
        shell.addChild(box("equipment-socket", 27, 87, 22, 22));
        rigName = text(Component.empty(), "equipment-name", 60, 85, 174, 10);
        rigStatus = text(Component.empty(), "equipment-detail", 60, 99, 174, 10);
        packName.setAllowHitTest(true);
        rigName.setAllowHitTest(true);
        shell.addChildren(rigName, rigStatus);
        shell.addChild(box("equipment-divider", 12, 113, 224, 1));
        shell.addChild(text(Component.translatable("container.inventory"), "equipment-caption", 44, 116, 172, 9));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            shell.addChild(box("equipment-inventory-slot", 43 + col * 18, 127 + row * 18, 18, 18));
        for (int col = 0; col < 9; col++)
            shell.addChild(box("equipment-hotbar-slot", 43 + col * 18, 185, 18, 18));
        addChild(shell);
        refresh();
    }
    public void refresh() {
        var pack = menu.getSlot(0).getItem();
        boolean equipped = pack.getItem() instanceof EppItem;
        packName.setText(equipped ? pack.getHoverName() : Component.translatable("gui.starboundmc.epp.empty"));
        packName.style(s -> s.tooltips(equipped ? pack.getHoverName() : Component.translatable("gui.starboundmc.epp.empty")));
        oxygen.setText(equipped ? Component.translatable("tooltip.starboundmc.epp.oxygen", EppItem.oxygen(pack), EppItem.capacity(pack))
                : Component.translatable("gui.starboundmc.equipment.life_support"));
        int installed = (int) pack.getOrDefault(ModDataComponents.EPP_MODULES, ItemContainerContents.EMPTY).nonEmptyStream().count();
        int sockets = equipped ? ((EppItem) pack.getItem()).moduleSlots() : 0;
        modules.setText(Component.translatable("gui.starboundmc.equipment.modules", installed, sockets));
        tank.setProgress(equipped ? (float) EppItem.oxygen(pack) / EppItem.capacity(pack) : 0);
        var rig = menu.getSlot(37).getItem();
        rigName.setText(rig.isEmpty() ? Component.translatable("gui.starboundmc.mobility.slot") : rig.getHoverName());
        rigName.style(s -> s.tooltips(rig.isEmpty() ? Component.translatable("gui.starboundmc.mobility.slot") : rig.getHoverName()));
        var player = Minecraft.getInstance().player;
        boolean charged = player != null && player.getData(ModAttachments.MOBILITY_STATE).charged;
        rigStatus.setText(Component.translatable(rig.isEmpty() ? "gui.starboundmc.equipment.uninstalled"
                : charged ? "gui.starboundmc.equipment.rig_ready" : "gui.starboundmc.equipment.rig_spent"));
    }
    private static UIElement box(String cls, int x, int y, int w, int h) {
        var element = new UIElement().addClass(cls);
        element.setAllowHitTest(false);
        element.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(w).height(h));
        return element;
    }
    private static Label text(Component value, String cls, int x, int y, int w, int h) {
        var label = new Label();
        label.setText(value);
        label.addClass(cls); label.setAllowHitTest(false); label.setOverflowVisible(false);
        label.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(w).height(h));
        label.textStyle(s -> s.adaptiveWidth(false).textWrap(TextWrap.HIDE));
        return label;
    }
}
