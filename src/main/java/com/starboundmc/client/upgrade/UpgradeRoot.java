package com.starboundmc.client.upgrade;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.starboundmc.item.MatterManipulatorItem;
import com.starboundmc.item.MatterManipulatorModuleItem;
import com.starboundmc.menu.UpgradeMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.UpgradeMatterManipulatorPacket;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** LDLib2 upgrade topology for the Matter Manipulator workbench. */
public final class UpgradeRoot extends UIElement {
    public static final int PANEL_W = 320;
    public static final int PANEL_H = 250;
    private static final int BLUEPRINT_W = 312;
    private static final int BLUEPRINT_H = 102;

    private final UpgradeMenu menu;
    private final Inventory inventory;
    private final Label moduleCount = new Label();
    private final Label detailName = new Label();
    private final Label detailLevel = new Label();
    private final Label detailCost = new Label();
    private final Button upgradeAction = new Button();
    private final List<UpgradeTrack> tracks = new ArrayList<>();
    private ItemStack lastManipulator = ItemStack.EMPTY;
    private int lastModules = Integer.MIN_VALUE;
    private int selectedTrack = -1;

    public UpgradeRoot(int left, int top, Component title, Component inventoryTitle,
                       UpgradeMenu menu, Inventory inventory) {
        this.menu = menu;
        this.inventory = inventory;
        addClass("upgrade-screen");
        setAllowHitTest(false);
        layout(layout -> layout.widthPercent(100).heightPercent(100));

        var shell = positioned("upgrade-shell", left, top, PANEL_W, PANEL_H);
        shell.setOverflowVisible(false);
        shell.addChildren(
                buildHeader(title), new BlueprintCanvas(), buildManipulatorSocket(),
                buildTrack(UpgradeMenu.TRACK_SPEED, "speed", "gui.starboundmc.upgrade.track_speed",
                        MatterManipulatorItem.MAX_UPGRADES, 14, 35, true),
                buildTrack(UpgradeMenu.TRACK_RANGE, "range", "gui.starboundmc.upgrade.track_range",
                        MatterManipulatorItem.MAX_UPGRADES, 14, 87, true),
                buildTrack(UpgradeMenu.TRACK_MINING, "mining", "gui.starboundmc.upgrade.track_mining",
                        MatterManipulatorItem.MAX_MINING_LEVEL, 230, 35, false),
                buildTrack(UpgradeMenu.TRACK_FORTUNE, "fortune", "gui.starboundmc.upgrade.track_fortune",
                        MatterManipulatorItem.MAX_FORTUNE_UPGRADES, 230, 87, false),
                buildDetailPanel(), buildInventorySection(inventoryTitle));
        addChild(shell);
        refreshState();
    }

    private UIElement buildHeader(Component title) {
        var header = positioned("upgrade-header", 4, 3, 312, 18);
        header.addChild(positioned("upgrade-header-rail", 0, 0, 3, 18));
        var titleLabel = label(title, "upgrade-title", 6, 2, 190, 8);
        var hintLabel = label(Component.translatable("gui.starboundmc.upgrade.blueprint_hint"),
                "upgrade-header-hint", 6, 10, 210, 7);
        moduleCount.addClass("upgrade-module-count");
        configureLabel(moduleCount, 218, 5, 87, 9);
        moduleCount.textStyle(style -> style.adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.RIGHT).textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        header.addChildren(titleLabel, hintLabel, moduleCount);
        return header;
    }

    private UIElement buildManipulatorSocket() {
        var socket = positioned("upgrade-manipulator-socket", 151, 66, 18, 18);
        socket.style(style -> style.tooltips(
                Component.translatable("gui.starboundmc.upgrade.manipulator_socket_hint")));
        return socket;
    }

    private UIElement buildTrack(int trackId, String name, String labelKey,
                                 int maxLevel, int left, int top, boolean pointsLeft) {
        var track = new UpgradeTrack(trackId, name, labelKey, maxLevel, pointsLeft);
        track.root.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(left).top(top).width(76).height(35));
        tracks.add(track);
        return track.root;
    }

    private UIElement buildDetailPanel() {
        var detail = positioned("upgrade-detail", 4, 129, 312, 30);
        detail.addChild(positioned("upgrade-detail-accent", 0, 0, 3, 30));
        detail.addChild(positioned("upgrade-detail-top-line", 0, 0, 312, 1));
        detail.addChild(positioned("upgrade-detail-bottom-line", 0, 29, 312, 1));
        detailName.addClass("upgrade-detail-name");
        configureLabel(detailName, 9, 3, 132, 9);
        detailLevel.addClass("upgrade-detail-level");
        configureLabel(detailLevel, 9, 15, 94, 8);
        detailCost.addClass("upgrade-detail-cost");
        configureLabel(detailCost, 105, 15, 105, 8);

        upgradeAction.setText(Component.translatable("gui.starboundmc.upgrade.execute"));
        upgradeAction.addClass("upgrade-main-action");
        upgradeAction.text.setOverflowVisible(false);
        upgradeAction.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(220).top(5).width(84).height(20));
        upgradeAction.textStyle(style -> style.adaptiveWidth(true)
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        upgradeAction.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    && selectedTrack >= 0 && upgradeAction.isActive()) {
                ModNetwork.sendToServer(new UpgradeMatterManipulatorPacket(selectedTrack));
                event.stopPropagation();
            }
        });
        detail.addChildren(detailName, detailLevel, detailCost, upgradeAction);
        return detail;
    }

    private UIElement buildInventorySection(Component inventoryTitle) {
        var section = positioned("upgrade-inventory-section", 75, 162, 170, 85);
        section.addChild(label(inventoryTitle, "upgrade-inventory-caption", 4, 0, 100, 9));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                section.addChild(slotSocket("upgrade-player-socket", 3 + col * 18, 11 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            section.addChild(slotSocket("upgrade-hotbar-socket", 3 + col * 18, 65));
        }
        return section;
    }

    public void refreshState() {
        ItemStack manipulator = menu.getManipulatorContainer().getItem(0);
        int modules = countModules();
        if (ItemStack.matches(lastManipulator, manipulator) && modules == lastModules) return;
        lastManipulator = manipulator.copy();
        lastModules = modules;
        moduleCount.setText(Component.translatable("gui.starboundmc.upgrade.modules_available", modules));
        boolean hasManipulator = manipulator.getItem() instanceof MatterManipulatorItem;
        for (UpgradeTrack track : tracks) track.refresh(manipulator, hasManipulator, modules);
        refreshSelection(hasManipulator, modules);
    }

    private void selectTrack(int trackId) {
        selectedTrack = trackId;
        boolean hasManipulator = lastManipulator.getItem() instanceof MatterManipulatorItem;
        for (UpgradeTrack track : tracks) track.setSelected(track.id == trackId);
        refreshSelection(hasManipulator, lastModules);
    }

    private void refreshSelection(boolean hasManipulator, int modules) {
        UpgradeTrack selected = trackById(selectedTrack);
        if (selected == null) {
            detailName.setText(Component.translatable("gui.starboundmc.upgrade.no_selection"));
            detailLevel.setText(Component.translatable("gui.starboundmc.upgrade.select_node"));
            detailCost.setText(Component.empty());
            upgradeAction.setActive(false);
            upgradeAction.style(style -> style.tooltips(
                    Component.translatable("gui.starboundmc.upgrade.select_node")));
            return;
        }
        int level = hasManipulator ? levelForTrack(lastManipulator, selected.id) : 0;
        detailName.setText(Component.translatable(selected.labelKey));
        detailLevel.setText(Component.translatable(
                "gui.starboundmc.upgrade.level_summary", level, selected.maxLevel));
        Component tooltip;
        boolean active = false;
        if (!hasManipulator) {
            detailCost.setText(Component.translatable("gui.starboundmc.upgrade.insert_tool"));
            tooltip = Component.translatable("message.starboundmc.upgrade.no_manipulator");
        } else if (level >= selected.maxLevel) {
            detailCost.setText(Component.translatable("gui.starboundmc.upgrade.maxed"));
            tooltip = Component.translatable("message.starboundmc.upgrade.max");
        } else {
            int cost = UpgradeMenu.modulesRequiredForTargetLevel(level + 1);
            detailCost.setText(Component.translatable("gui.starboundmc.upgrade.cost", cost));
            if (modules >= cost) {
                tooltip = Component.translatable("gui.starboundmc.upgrade.ready_hint");
                active = true;
            } else {
                tooltip = Component.translatable("message.starboundmc.upgrade.need_modules", cost);
            }
        }
        upgradeAction.setActive(active);
        upgradeAction.style(style -> style.tooltips(tooltip));
    }

    private UpgradeTrack trackById(int id) {
        for (UpgradeTrack track : tracks) if (track.id == id) return track;
        return null;
    }

    private int countModules() {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.getItem() instanceof MatterManipulatorModuleItem) total += stack.getCount();
        }
        return total;
    }

    private static int levelForTrack(ItemStack stack, int track) {
        return switch (track) {
            case UpgradeMenu.TRACK_SPEED -> MatterManipulatorItem.getSpeedLevel(stack);
            case UpgradeMenu.TRACK_RANGE -> MatterManipulatorItem.getRangeLevel(stack);
            case UpgradeMenu.TRACK_MINING -> MatterManipulatorItem.getMiningLevel(stack);
            case UpgradeMenu.TRACK_FORTUNE -> MatterManipulatorItem.getFortuneLevel(stack);
            default -> 0;
        };
    }

    private final class UpgradeTrack {
        private final int id;
        private final String labelKey;
        private final int maxLevel;
        private final UIElement root = new UIElement();
        private final List<Button> nodes = new ArrayList<>();

        private UpgradeTrack(int id, String name, String labelKey, int maxLevel, boolean pointsLeft) {
            this.id = id;
            this.labelKey = labelKey;
            this.maxLevel = maxLevel;
            root.addClasses("upgrade-branch", "upgrade-branch-" + name);
            root.setAllowHitTest(false);
            var branchLabel = label(Component.translatable(labelKey), "upgrade-branch-label", 0, 0, 76, 9);
            branchLabel.textStyle(style -> style.adaptiveWidth(false)
                    .textAlignHorizontal(pointsLeft ? Horizontal.RIGHT : Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
            root.addChild(branchLabel);
            int firstX = pointsLeft ? 76 - maxLevel * 20 : 0;
            for (int index = 0; index < maxLevel; index++) {
                int level = index + 1;
                // Left-hand branches unlock from the node nearest the tool and
                // then grow outwards, so their logical order is screen-reversed.
                int visualIndex = pointsLeft ? maxLevel - 1 - index : index;
                int nodeLeft = firstX + visualIndex * 20;
                var node = new Button();
                node.setText(Component.empty());
                node.addClasses("upgrade-node", "upgrade-node-" + name);
                node.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                        .left(nodeLeft).top(14).width(15).height(15));
                node.addEventListener(UIEvents.CLICK, event -> {
                    if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        selectTrack(id);
                        event.stopPropagation();
                    }
                });
                node.style(style -> style.tooltips(Component.translatable(
                        "gui.starboundmc.upgrade.node_hint", level, maxLevel)));
                nodes.add(node);
                root.addChild(node);
            }
        }

        private void refresh(ItemStack manipulator, boolean hasManipulator, int modules) {
            int level = hasManipulator ? levelForTrack(manipulator, id) : 0;
            int nextCost = level < maxLevel
                    ? UpgradeMenu.modulesRequiredForTargetLevel(level + 1) : Integer.MAX_VALUE;
            for (int index = 0; index < nodes.size(); index++) {
                Button node = nodes.get(index);
                node.removeClass("upgrade-node-unlocked");
                node.removeClass("upgrade-node-ready");
                node.removeClass("upgrade-node-locked");
                node.removeClass("upgrade-node-maxed");
                if (hasManipulator && index < level) {
                    node.addClass(level >= maxLevel ? "upgrade-node-maxed" : "upgrade-node-unlocked");
                } else if (hasManipulator && index == level && modules >= nextCost) {
                    node.addClass("upgrade-node-ready");
                } else {
                    node.addClass("upgrade-node-locked");
                }
            }
        }

        private void setSelected(boolean selected) {
            if (selected) root.addClass("upgrade-branch-selected");
            else root.removeClass("upgrade-branch-selected");
            for (Button node : nodes) {
                if (selected) node.addClass("upgrade-node-selected");
                else node.removeClass("upgrade-node-selected");
            }
        }
    }

    private final class BlueprintCanvas extends UIElement {
        private BlueprintCanvas() {
            addClass("upgrade-blueprint");
            setAllowHitTest(false);
            setOverflowVisible(false);
            layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                    .left(4).top(24).width(BLUEPRINT_W).height(BLUEPRINT_H));
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            float x = getPositionX();
            float y = getPositionY();
            var graphics = context.graphics;
            for (int gx = 8; gx < BLUEPRINT_W; gx += 12) {
                graphics.fill(Math.round(x + gx), Math.round(y), Math.round(x + gx + 1),
                        Math.round(y + BLUEPRINT_H), 0x192B6670);
            }
            for (int gy = 8; gy < BLUEPRINT_H; gy += 12) {
                graphics.fill(Math.round(x), Math.round(y + gy), Math.round(x + BLUEPRINT_W),
                        Math.round(y + gy + 1), 0x192B6670);
            }
            drawConnector(graphics, x, y, UpgradeMenu.TRACK_SPEED,
                    72, 32, 102, 32, 102, 43, 118, 43);
            drawConnector(graphics, x, y, UpgradeMenu.TRACK_RANGE,
                    72, 84, 102, 84, 102, 66, 120, 66);
            drawConnector(graphics, x, y, UpgradeMenu.TRACK_MINING,
                    194, 43, 208, 43, 208, 32, 230, 32);
            drawConnector(graphics, x, y, UpgradeMenu.TRACK_FORTUNE,
                    194, 66, 210, 66, 210, 84, 230, 84);
            int gunColor = lastManipulator.getItem() instanceof MatterManipulatorItem
                    ? 0xFF8FE5D2 : 0xFF637B7C;
            // Broad receiver and stepped barrel: close to the reference's
            // recognisable silhouette while remaining an original line drawing.
            drawPolyline(graphics, x, y, gunColor, 1.25F,
                    108, 28, 157, 28, 169, 34, 185, 45, 202, 45,
                    202, 63, 177, 63, 168, 70, 156, 70, 148, 66,
                    126, 66, 115, 60, 108, 52, 108, 28);
            // Upper casing inset and rear energy housing.
            drawPolyline(graphics, x, y, 0xFF4F8F8F, 1.0F,
                    118, 33, 153, 33, 163, 38, 177, 49, 177, 59,
                    160, 59, 151, 54, 118, 54, 118, 33);
            drawPolyline(graphics, x, y, 0xFF4F8F8F, 1.0F,
                    108, 38, 97, 38, 90, 45, 90, 57, 100, 64,
                    116, 64);

            // Forward emitter sleeve and muzzle split.
            drawPolyline(graphics, x, y, gunColor, 1.15F,
                    177, 49, 208, 49, 208, 59, 177, 59);
            drawPolyline(graphics, x, y, 0xFF4F8F8F, 1.0F,
                    190, 49, 190, 59, 201, 59, 201, 49);

            // Angled grip and lower power-cell cage.
            drawPolyline(graphics, x, y, 0xFF4F8F8F, 1.0F,
                    139, 66, 157, 66, 151, 75, 151, 91, 132, 91,
                    132, 80, 139, 66);
            drawPolyline(graphics, x, y, gunColor, 1.15F,
                    132, 80, 116, 80, 110, 91, 145, 91);

            // Vents, trigger and small diagnostic marks give the blueprint
            // enough internal structure without using a texture copy.
            drawPolyline(graphics, x, y, 0xFF4F8F8F, 0.9F,
                    122, 58, 145, 58);
            drawPolyline(graphics, x, y, 0xFF4F8F8F, 0.9F,
                    119, 61, 142, 61);
            drawPolyline(graphics, x, y, 0xFF4F8F8F, 0.9F,
                    136, 72, 128, 72, 128, 78, 134, 78);
            drawPolyline(graphics, x, y, 0xFF4F8F8F, 0.9F,
                    158, 41, 164, 41, 164, 48, 158, 48, 158, 41);
            graphics.flush();
        }

        private void drawConnector(net.minecraft.client.gui.GuiGraphics graphics, float x, float y,
                                   int track, float... points) {
            int color = selectedTrack == track ? 0xFF89E7E2 : 0xFF657A7D;
            drawPolyline(graphics, x, y, color, selectedTrack == track ? 1.5F : 1.0F, points);
        }

        private void drawPolyline(net.minecraft.client.gui.GuiGraphics graphics, float x, float y,
                                  int color, float width, float... coordinates) {
            List<Vector2f> points = new ArrayList<>();
            for (int index = 0; index < coordinates.length; index += 2) {
                points.add(new Vector2f(x + coordinates[index], y + coordinates[index + 1]));
            }
            DrawerHelper.drawLines(graphics, points, color, color, width);
        }
    }

    private static UIElement slotSocket(String styleClass, int left, int top) {
        return positioned("upgrade-slot-socket", left, top, 18, 18).addClass(styleClass);
    }

    private static UIElement positioned(String styleClass, int left, int top, int width, int height) {
        var element = new UIElement().addClass(styleClass);
        element.setAllowHitTest(false);
        element.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(left).top(top).width(width).height(height));
        return element;
    }

    private static Label label(Component text, String styleClass,
                               int left, int top, int width, int height) {
        var label = new Label();
        label.setText(text);
        label.addClass(styleClass);
        configureLabel(label, left, top, width, height);
        return label;
    }

    private static void configureLabel(Label label, int left, int top, int width, int height) {
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(left).top(top).width(width).height(height));
        label.textStyle(style -> style.adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
    }
}
