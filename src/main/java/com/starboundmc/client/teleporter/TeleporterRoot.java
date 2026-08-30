package com.starboundmc.client.teleporter;

import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.client.ClientShipEnvironmentState;
import com.starboundmc.client.ClientTeleporterState;
import com.starboundmc.client.ui.ShipSystemLockOverlay;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.TeleporterListPacket;
import com.starboundmc.network.TeleporterRenamePacket;
import com.starboundmc.network.TeleporterUsePacket;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** LDLib2 component tree for the teleporter destination console. */
public final class TeleporterRoot extends UIElement {
    private static final int MAX_NAME_LENGTH = 64;

    private final ScrollerView destinationList = new ScrollerView();
    private final Map<String, DestinationRow> destinationRows = new LinkedHashMap<>();
    private final Label emptyState = new Label();
    private final TextField nameField = new TextField();
    private final Label networkStatus = new Label();
    private final UIElement detailIcon = new UIElement();
    private final Label detailTitle = new Label();
    private final Label detailType = new Label();
    private final Label detailHint = new Label();
    private final Button warpButton = new Button();
    private final ShipSystemLockOverlay environmentLock;
    private final int containerId;
    private boolean environmentLockInitialized;
    private boolean environmentLocked;
    private String selectedDestinationKey;

    public TeleporterRoot() {
        this(-1);
    }

    public TeleporterRoot(int containerId) {
        this.containerId = containerId;
        addClass("machine-screen");
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER));

        var shell = new UIElement().addClasses("machine-shell", "teleporter-shell");
        shell.layout(layout -> layout
                .width(184)
                .height(178)
                .flexDirection(FlexDirection.COLUMN));

        var header = buildHeader();
        var content = new UIElement().addClass("machine-content");
        content.layout(layout -> layout
                .widthPercent(100)
                .flex(1)
                .paddingAll(5)
                .gapAll(4)
                .flexDirection(FlexDirection.ROW));
        content.addChildren(buildDestinationSection(), buildDetailSection());

        shell.addChildren(header, content);
        addChild(shell);
        environmentLock = new ShipSystemLockOverlay();
        addChild(environmentLock);
        refreshFromClient();
        refreshEnvironmentLock();
    }

    private UIElement buildHeader() {
        var header = new UIElement().addClass("machine-header");
        header.layout(layout -> layout
                .widthPercent(100)
                .height(25)
                .paddingHorizontal(7)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        var title = new Label();
        title.setText(Component.translatable("container.starboundmc.teleporter"));
        title.addClass("machine-title");
        title.layout(layout -> layout.flex(1).height(10));
        title.textStyle(style -> style.textAlignVertical(Vertical.CENTER));

        var status = new Label();
        status.setText(Component.translatable("gui.starboundmc.teleporter.online"));
        status.addClass("teleporter-header-status");
        status.layout(layout -> layout.width(42).height(9));
        status.textStyle(style -> style
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER));

        header.addChildren(title, status);
        return header;
    }

    private UIElement buildDestinationSection() {
        var section = new UIElement().addClasses("teleporter-pane", "teleporter-destination-pane");
        section.setOverflowVisible(false);
        section.layout(layout -> layout
                .width(88)
                .heightPercent(100)
                .paddingAll(4)
                .gapAll(4)
                .flexDirection(FlexDirection.COLUMN));

        var headingRow = new UIElement();
        headingRow.layout(layout -> layout
                .widthPercent(100)
                .height(9)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        var heading = new Label();
        heading.setText(Component.translatable("gui.starboundmc.teleporter.destinations"));
        heading.addClass("machine-section-title");
        heading.layout(layout -> layout.flex(1).height(9));
        heading.textStyle(style -> style.textAlignVertical(Vertical.CENTER));

        networkStatus.addClass("machine-status");
        networkStatus.layout(layout -> layout.width(30).height(8));
        networkStatus.textStyle(style -> style
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER));
        headingRow.addChildren(heading, networkStatus);

        destinationList.addClass("teleporter-destination-list");
        destinationList.layout(layout -> layout.widthPercent(100).flex(1));
        destinationList.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(8)
                .maxScrollPixel(18));
        destinationList.viewPort(view -> view
                .layout(layout -> layout.paddingAll(0))
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY)));
        destinationList.viewContainer(view -> view.layout(layout -> layout
                .widthPercent(100)
                .gapAll(2)
                .flexDirection(FlexDirection.COLUMN)));

        emptyState.setText(Component.translatable("gui.starboundmc.teleporter.empty"));
        emptyState.addClass("machine-empty-state");
        emptyState.setOverflowVisible(false);
        emptyState.layout(layout -> layout.widthPercent(100).height(26));
        emptyState.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textWrap(TextWrap.WRAP)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));

        section.addChildren(headingRow, destinationList);
        return section;
    }

    private UIElement buildDetailSection() {
        var pane = new UIElement().addClasses("teleporter-pane", "teleporter-detail-pane");
        pane.setOverflowVisible(false);
        pane.layout(layout -> layout
                .flex(1)
                .heightPercent(100)
                .paddingAll(4)
                .gapAll(3)
                .flexDirection(FlexDirection.COLUMN));

        var heading = new Label();
        heading.setText(Component.translatable("gui.starboundmc.teleporter.destination_details"));
        heading.addClass("teleporter-detail-heading");
        heading.layout(layout -> layout.widthPercent(100).height(8));

        var identity = new UIElement().addClass("teleporter-detail-identity");
        identity.layout(layout -> layout
                .widthPercent(100)
                .height(34)
                .gapAll(5)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        detailIcon.addClass("teleporter-detail-icon");
        detailIcon.setAllowHitTest(false);
        detailIcon.layout(layout -> layout.width(26).height(26));

        var copy = new UIElement();
        copy.setOverflowVisible(false);
        copy.layout(layout -> layout
                .flex(1)
                .height(26)
                .justifyContent(AlignContent.CENTER)
                .flexDirection(FlexDirection.COLUMN));

        detailTitle.addClass("teleporter-detail-title");
        detailTitle.setOverflowVisible(false);
        detailTitle.layout(layout -> layout.widthPercent(100).height(17));
        detailTitle.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.WRAP));

        detailType.addClass("teleporter-detail-type");
        detailType.setOverflowVisible(false);
        detailType.layout(layout -> layout.widthPercent(100).height(7));
        detailType.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        copy.addChildren(detailTitle, detailType);
        identity.addChildren(detailIcon, copy);

        detailHint.setText(Component.translatable("gui.starboundmc.teleporter.select_destination"));
        detailHint.addClass("teleporter-detail-hint");
        detailHint.setOverflowVisible(false);
        detailHint.layout(layout -> layout.widthPercent(100).height(24));
        detailHint.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.WRAP));

        warpButton.noText();
        warpButton.addClasses("teleporter-console-button", "teleporter-transmit-button");
        warpButton.setOverflowVisible(false);
        warpButton.layout(layout -> layout
                .widthPercent(100)
                .height(20)
                .paddingHorizontal(5)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        var transmitMarker = new UIElement().addClass("teleporter-action-marker");
        transmitMarker.setAllowHitTest(false);
        transmitMarker.layout(layout -> layout.width(2).height(10).marginRight(5));

        var transmitLabel = buildButtonLabel(
                Component.translatable("gui.starboundmc.teleporter.transmit"),
                "teleporter-transmit-label");

        var transmitArrow = new Label();
        transmitArrow.setText(Component.literal(">"));
        transmitArrow.addClass("teleporter-action-arrow");
        transmitArrow.setAllowHitTest(false);
        transmitArrow.setOverflowVisible(false);
        transmitArrow.layout(layout -> layout.width(7).height(10).marginLeft(3));
        transmitArrow.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        warpButton.addChildren(transmitMarker, transmitLabel, transmitArrow);
        warpButton.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && warpButton.isActive()
                    && !isEnvironmentLocked()) {
                sendSelectedDestination();
                event.stopPropagation();
            }
        });

        var divider = new UIElement().addClass("teleporter-control-divider");
        divider.setAllowHitTest(false);
        divider.layout(layout -> layout.widthPercent(100).height(1));

        pane.addChildren(heading, identity, detailHint, warpButton, divider, buildRenameSection());
        return pane;
    }

    private UIElement buildRenameSection() {
        var section = new UIElement().addClasses("teleporter-rename-section", "teleporter-subpane");
        section.layout(layout -> layout
                .widthPercent(100)
                .height(34)
                .gapAll(2)
                .flexDirection(FlexDirection.COLUMN));

        var headingRow = new UIElement();
        headingRow.layout(layout -> layout
                .widthPercent(100)
                .height(8)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        var nameLabel = new Label();
        nameLabel.setText(Component.translatable("gui.starboundmc.teleporter.name"));
        nameLabel.addClasses("machine-field-label", "teleporter-field-caption");
        nameLabel.layout(layout -> layout.flex(1).height(8));

        headingRow.addChild(nameLabel);

        var inputRow = new UIElement();
        inputRow.layout(layout -> layout
                .widthPercent(100)
                .height(20)
                .gapAll(4)
                .flexDirection(FlexDirection.ROW));

        nameField.addClass("teleporter-name-field");
        nameField.layout(layout -> layout.flex(1).height(20));
        nameField.setAnyString();
        nameField.setTextValidator(value -> value.length() <= MAX_NAME_LENGTH);
        nameField.textFieldStyle(style -> style
                .placeholder(Component.translatable("gui.starboundmc.teleporter.name_placeholder"))
                .fontSize(7)
                .textColor(0xFFE7E8E8)
                .cursorColor(0xFFD0D2D2)
                .textShadow(false));
        nameField.addEventListener(UIEvents.KEY_DOWN, event -> {
            if (event.keyCode == GLFW.GLFW_KEY_ENTER || event.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                saveName();
                event.stopPropagation();
            }
        });

        var save = new Button();
        save.noText();
        save.addClasses("teleporter-console-button", "teleporter-save-button");
        save.setOverflowVisible(false);
        save.layout(layout -> layout
                .width(38)
                .height(20)
                .paddingHorizontal(3)
                .alignItems(AlignItems.CENTER));
        save.addChild(buildButtonLabel(
                Component.translatable("gui.starboundmc.teleporter.save"),
                "teleporter-save-label"));
        save.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                saveName();
                event.stopPropagation();
            }
        });

        inputRow.addChildren(nameField, save);
        section.addChildren(headingRow, inputRow);
        return section;
    }

    /** Applies a newly received destination snapshot without rebuilding the whole screen. */
    public void refreshIfDirty() {
        refreshEnvironmentLock();
        if (ClientTeleporterState.consumeDirty()) {
            refreshFromClient();
        }
    }

    private boolean isEnvironmentLocked() {
        return ClientShipEnvironmentState.isLocked(containerId);
    }

    private void refreshEnvironmentLock() {
        boolean locked = isEnvironmentLocked();
        if (!environmentLockInitialized || environmentLocked != locked) {
            environmentLockInitialized = true;
            environmentLocked = locked;
            environmentLock.setLocked(locked);
            if (!locked)
                updateDetail(findSelected(ClientTeleporterState.getDestinations()));
            else
                warpButton.setActive(false);
        } else {
            environmentLock.setLocked(locked);
        }
    }

    int containerId() {
        return containerId;
    }

    private void refreshFromClient() {
        List<TeleporterListPacket.Entry> destinations = ClientTeleporterState.getDestinations();
        nameField.setText(ClientTeleporterState.getCurrentName(), false);
        networkStatus.setText(Component.translatable(
                "gui.starboundmc.teleporter.link_count", destinations.size()));
        if (selectedDestinationKey != null && destinations.stream()
                .noneMatch(entry -> entry.key().equals(selectedDestinationKey))) {
            selectedDestinationKey = null;
        }
        syncDestinationRows(destinations);
        updateDetail(findSelected(destinations));
    }

    private void syncDestinationRows(List<TeleporterListPacket.Entry> destinations) {
        Map<String, TeleporterListPacket.Entry> incoming = new LinkedHashMap<>();
        for (TeleporterListPacket.Entry entry : destinations) {
            incoming.putIfAbsent(entry.key(), entry);
        }

        var staleRows = destinationRows.entrySet().iterator();
        while (staleRows.hasNext()) {
            var rowEntry = staleRows.next();
            if (!incoming.containsKey(rowEntry.getKey())) {
                destinationList.removeScrollViewChild(rowEntry.getValue().button);
                staleRows.remove();
            }
        }

        if (incoming.isEmpty()) {
            if (!destinationList.hasScrollViewChild(emptyState)) {
                destinationList.addScrollViewChild(emptyState);
            }
            destinationRows.clear();
            return;
        }

        destinationList.removeScrollViewChild(emptyState);
        Map<String, DestinationRow> orderedRows = new LinkedHashMap<>();
        int index = 0;
        for (TeleporterListPacket.Entry entry : incoming.values()) {
            DestinationRow row = destinationRows.get(entry.key());
            if (row == null) {
                row = createDestinationRow(entry);
            } else if (!row.entry.equals(entry)) {
                updateDestinationRow(row, entry);
            }

            List<UIElement> children = destinationList.viewContainer.getChildren();
            if (!destinationList.hasScrollViewChild(row.button)) {
                destinationList.addScrollViewChildAt(row.button, index);
            } else if (index >= children.size() || children.get(index) != row.button) {
                destinationList.removeScrollViewChild(row.button);
                destinationList.addScrollViewChildAt(row.button, index);
            }
            orderedRows.put(entry.key(), row);
            index++;
        }

        destinationRows.clear();
        destinationRows.putAll(orderedRows);
        updateSelectionClasses();
    }

    private DestinationRow createDestinationRow(TeleporterListPacket.Entry entry) {
        var button = new Button();
        button.noText();
        button.setOverflowVisible(false);
        button.addClasses("teleporter-destination", "teleporter-destination-row");
        button.layout(layout -> layout
                .widthPercent(100)
                .height(27)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.FLEX_START));

        var selectionMarker = new UIElement().addClass("teleporter-destination-marker");
        selectionMarker.setAllowHitTest(false);
        selectionMarker.layout(layout -> layout.width(2).height(19).marginRight(3));

        var icon = buildDestinationIcon(entry.type());
        var textColumn = new UIElement().addClass("teleporter-destination-copy");
        textColumn.setOverflowVisible(false);
        textColumn.layout(layout -> layout
                .flex(1)
                .height(18)
                .justifyContent(AlignContent.CENTER)
                .flexDirection(FlexDirection.COLUMN));

        var nameLabel = new Label();
        nameLabel.addClass("teleporter-destination-name");
        nameLabel.setOverflowVisible(false);
        nameLabel.layout(layout -> layout.widthPercent(100).height(10));
        nameLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        var typeLabel = new Label();
        typeLabel.addClass("teleporter-destination-type");
        typeLabel.setOverflowVisible(false);
        typeLabel.layout(layout -> layout.widthPercent(100).height(7));
        typeLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        textColumn.addChildren(nameLabel, typeLabel);

        button.addChildren(selectionMarker, icon, textColumn);
        button.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                selectDestination(entry.key());
                event.stopPropagation();
            }
        });

        var row = new DestinationRow(entry, button, nameLabel, typeLabel);
        updateDestinationRow(row, entry);
        return row;
    }

    private void updateDestinationRow(DestinationRow row, TeleporterListPacket.Entry entry) {
        row.entry = entry;
        row.nameLabel.setText(destinationName(entry));
        row.typeLabel.setText(destinationType(entry));
        row.button.style(style -> style.tooltips(destinationName(entry)));
    }

    private void selectDestination(String key) {
        if (isEnvironmentLocked())
            return;
        DestinationRow row = destinationRows.get(key);
        if (row == null) return;
        if (!Objects.equals(selectedDestinationKey, key)) {
            selectedDestinationKey = key;
            updateSelectionClasses();
        }
        updateDetail(row.entry);
    }

    private void updateSelectionClasses() {
        for (var rowEntry : destinationRows.entrySet()) {
            boolean selected = Objects.equals(rowEntry.getKey(), selectedDestinationKey);
            Button button = rowEntry.getValue().button;
            if (selected) {
                button.addClass("teleporter-destination-selected");
            } else {
                button.removeClass("teleporter-destination-selected");
            }
        }
    }

    private TeleporterListPacket.Entry findSelected(List<TeleporterListPacket.Entry> destinations) {
        if (selectedDestinationKey == null) return null;
        return destinations.stream()
                .filter(entry -> entry.key().equals(selectedDestinationKey))
                .findFirst()
                .orElse(null);
    }

    private void updateDetail(TeleporterListPacket.Entry entry) {
        detailIcon.clearAllChildren();
        if (entry == null) {
            detailTitle.setText(Component.translatable("gui.starboundmc.teleporter.no_selection"));
            detailType.setText(Component.empty());
            detailHint.setText(Component.translatable("gui.starboundmc.teleporter.select_destination"));
            warpButton.setActive(false);
            return;
        }

        var icon = buildDestinationIcon(entry.type());
        icon.removeClass("teleporter-destination-icon");
        icon.addClass("teleporter-detail-glyph");
        icon.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(3)
                .top(3)
                .width(20)
                .height(20)
                .marginRight(0));
        detailIcon.addChild(icon);
        detailTitle.setText(destinationName(entry));
        detailType.setText(destinationType(entry));
        detailHint.setText(Component.translatable("gui.starboundmc.teleporter.transmit_hint"));
            warpButton.setActive(!isEnvironmentLocked());
    }

    private void sendSelectedDestination() {
        if (!isEnvironmentLocked() && selectedDestinationKey != null) {
            ModNetwork.sendToServer(new TeleporterUsePacket(selectedDestinationKey));
        }
    }

    /** Builds a small semantic destination mark from LDLib2 elements. */
    private static UIElement buildDestinationIcon(byte type) {
        var icon = new UIElement().addClass("teleporter-destination-icon");
        icon.setAllowHitTest(false);
        icon.layout(layout -> layout
                .width(20)
                .height(20)
                .marginRight(4));

        switch (type) {
            case 0 -> buildShipIcon(icon);
            case 1 -> {
                var planet = new UIElement().addClass("teleporter-icon-planet");
                planet.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(2)
                        .top(2)
                        .width(16)
                        .height(16));
                ResourceLocation sprite = ResourceLocation.fromNamespaceAndPath(
                        StarboundMC.MODID,
                        "textures/gui/starmap/bodies/"
                                + ClientPlanetState.getCurrent().getId() + ".png");
                planet.style(style -> style.backgroundTexture(GuiTextureGroup.of(
                        SpriteTexture.of(sprite),
                        SDFRectTexture.of(0x00000000)
                                .setRadius(8)
                                .setStroke(0.75F)
                                .setBorderColor(0xB8D2D2D2))));
                icon.addChild(planet);
            }
            default -> buildFlagIcon(icon);
        }
        return icon;
    }

    private static void buildShipIcon(UIElement icon) {
        icon.addChildren(
                iconPart("teleporter-icon-ship-nose", 9, 3, 2, 3),
                iconPart("teleporter-icon-ship-body", 8, 6, 4, 8),
                iconPart("teleporter-icon-ship-wing-left", 4, 9, 4, 4),
                iconPart("teleporter-icon-ship-wing-right", 12, 9, 4, 4),
                iconPart("teleporter-icon-ship-tail", 6, 14, 8, 2));
    }

    private static void buildFlagIcon(UIElement icon) {
        icon.addChildren(
                iconPart("teleporter-icon-flag-pole", 5, 3, 1, 14),
                iconPart("teleporter-icon-flag-top", 6, 3, 9, 2),
                iconPart("teleporter-icon-flag-middle", 6, 5, 7, 2),
                iconPart("teleporter-icon-flag-tip", 6, 7, 5, 2),
                iconPart("teleporter-icon-flag-base", 3, 17, 6, 1));
    }

    private static UIElement iconPart(String styleClass, int left, int top, int width, int height) {
        var part = new UIElement().addClasses("teleporter-icon-glyph", styleClass);
        part.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(left)
                .top(top)
                .width(width)
                .height(height));
        return part;
    }

    private static Label buildButtonLabel(Component text, String styleClass) {
        var label = new Label();
        label.setText(text);
        label.addClasses("teleporter-button-label", styleClass);
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.layout(layout -> layout.flex(1).heightPercent(100));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        return label;
    }

    private static Component destinationType(TeleporterListPacket.Entry entry) {
        return switch (entry.type()) {
            case 0 -> Component.translatable("gui.starboundmc.teleporter.type.ship");
            case 1 -> Component.translatable("gui.starboundmc.teleporter.type.planet");
            default -> Component.translatable("gui.starboundmc.teleporter.type.node");
        };
    }

    private static Component destinationName(TeleporterListPacket.Entry entry) {
        return switch (entry.type()) {
            case 0 -> Component.translatable("gui.starboundmc.teleporter.ship");
            case 1 -> Component.translatable("gui.starboundmc.teleporter.planet");
            default -> Component.literal(entry.label());
        };
    }

    private void saveName() {
        if (!isEnvironmentLocked())
            ModNetwork.sendToServer(new TeleporterRenamePacket(nameField.getValue()));
    }

    private static final class DestinationRow {
        private TeleporterListPacket.Entry entry;
        private final Button button;
        private final Label nameLabel;
        private final Label typeLabel;

        private DestinationRow(TeleporterListPacket.Entry entry, Button button,
                               Label nameLabel, Label typeLabel) {
            this.entry = entry;
            this.button = button;
            this.nameLabel = nameLabel;
            this.typeLabel = typeLabel;
        }
    }
}
