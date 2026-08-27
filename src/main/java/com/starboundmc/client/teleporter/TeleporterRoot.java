package com.starboundmc.client.teleporter;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
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
import com.starboundmc.client.ClientTeleporterState;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.TeleporterListPacket;
import com.starboundmc.network.TeleporterRenamePacket;
import com.starboundmc.network.TeleporterUsePacket;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** LDLib2 component tree for the teleporter destination console. */
public final class TeleporterRoot extends UIElement {
    private static final int MAX_NAME_LENGTH = 64;

    private final ScrollerView destinationList = new ScrollerView();
    private final TextField nameField = new TextField();
    private final Label networkStatus = new Label();

    public TeleporterRoot() {
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
                .gapAll(5)
                .flexDirection(FlexDirection.COLUMN));
        content.addChildren(buildDestinationSection(), buildRenameSection());

        shell.addChildren(header, content, buildCaseSpine());
        addChild(shell);
        refreshFromClient();
    }

    private UIElement buildHeader() {
        var header = new UIElement().addClass("machine-header");
        header.layout(layout -> layout
                .widthPercent(100)
                .height(25)
                .paddingHorizontal(7)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        var signal = new UIElement().addClass("teleporter-beacon");
        signal.layout(layout -> layout
                .width(17)
                .height(15)
                .marginRight(5)
                .gapAll(2)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER)
                .flexDirection(FlexDirection.ROW));
        var signalLeft = new UIElement().addClasses("teleporter-beacon-bar", "teleporter-beacon-bar-low");
        signalLeft.layout(layout -> layout.width(3).height(5));
        var signalCenter = new UIElement().addClasses("teleporter-beacon-bar", "teleporter-beacon-bar-high");
        signalCenter.layout(layout -> layout.width(3).height(11));
        var signalRight = new UIElement().addClasses("teleporter-beacon-bar", "teleporter-beacon-bar-mid");
        signalRight.layout(layout -> layout.width(3).height(8));
        signal.addChildren(signalLeft, signalCenter, signalRight);

        var title = new Label();
        title.setText(Component.translatable("container.starboundmc.teleporter"));
        title.addClass("machine-title");
        title.layout(layout -> layout.flex(1).height(10));
        title.textStyle(style -> style.textAlignVertical(Vertical.CENTER));

        var titlePlate = new UIElement().addClass("teleporter-title-plate");
        titlePlate.layout(layout -> layout
                .flex(1)
                .height(17)
                .marginRight(5)
                .paddingHorizontal(6)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));
        titlePlate.addChild(title);

        var closeHint = new Label();
        closeHint.setText(Component.translatable("gui.starboundmc.teleporter.close_hint"));
        closeHint.addClass("machine-key-hint");
        closeHint.layout(layout -> layout.width(48).height(15));
        closeHint.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));

        header.addChildren(signal, titlePlate, closeHint);
        return header;
    }

    private UIElement buildCaseSpine() {
        var spine = new UIElement().addClass("teleporter-case-spine");
        spine.setAllowHitTest(false);
        spine.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(2)
                .top(5)
                .width(4)
                .height(168)
                .paddingVertical(5)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.SPACE_BETWEEN)
                .flexDirection(FlexDirection.COLUMN));
        for (int i = 0; i < 3; i++) {
            var fastener = new UIElement().addClass("teleporter-case-fastener");
            fastener.layout(layout -> layout.width(2).height(2));
            spine.addChild(fastener);
        }
        return spine;
    }

    private UIElement buildDestinationSection() {
        var section = new UIElement().addClass("machine-section");
        section.layout(layout -> layout
                .flex(1)
                .widthPercent(100)
                .paddingAll(4)
                .gapAll(3)
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

        var headingPlate = new UIElement().addClass("teleporter-section-tab");
        headingPlate.layout(layout -> layout
                .flex(1)
                .height(9)
                .paddingHorizontal(3)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));
        headingPlate.addChild(heading);

        var networkLamp = new UIElement().addClass("teleporter-network-lamp");
        networkLamp.layout(layout -> layout.width(3).height(3).marginHorizontal(3));

        networkStatus.addClass("machine-status");
        networkStatus.layout(layout -> layout.width(55).height(8));
        networkStatus.textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT));
        headingRow.addChildren(headingPlate, networkLamp, networkStatus);

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

        section.addChildren(headingRow, destinationList);
        return section;
    }

    private UIElement buildRenameSection() {
        var section = new UIElement().addClasses("machine-section", "teleporter-rename-section");
        section.layout(layout -> layout
                .widthPercent(100)
                .height(38)
                .paddingAll(4)
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

        var online = new Label();
        online.setText(Component.translatable("gui.starboundmc.teleporter.online"));
        online.addClass("machine-online");
        online.layout(layout -> layout.width(64).height(8));
        online.textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT));
        headingRow.addChildren(nameLabel, online);

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
                .textColor(0xFFE9DFD0)
                .cursorColor(0xFFDEA05C)
                .textShadow(false));
        nameField.addEventListener(UIEvents.KEY_DOWN, event -> {
            if (event.keyCode == GLFW.GLFW_KEY_ENTER || event.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                saveName();
                event.stopPropagation();
            }
        });

        var save = new Button();
        save.setText(Component.translatable("gui.starboundmc.teleporter.save"));
        save.addClasses("machine-button", "machine-button-primary");
        save.layout(layout -> layout.width(56).height(20));
        save.setOnClick(event -> {
            saveName();
            event.stopPropagation();
        });

        inputRow.addChildren(nameField, save);
        section.addChildren(headingRow, inputRow);
        return section;
    }

    /** Applies a newly received destination snapshot without rebuilding the whole screen. */
    public void refreshIfDirty() {
        if (ClientTeleporterState.consumeDirty()) {
            refreshFromClient();
        }
    }

    private void refreshFromClient() {
        List<TeleporterListPacket.Entry> destinations = ClientTeleporterState.getDestinations();
        nameField.setText(ClientTeleporterState.getCurrentName(), false);
        networkStatus.setText(Component.translatable(
                "gui.starboundmc.teleporter.link_count", destinations.size()));
        rebuildDestinations(destinations);
    }

    private void rebuildDestinations(List<TeleporterListPacket.Entry> destinations) {
        destinationList.clearAllScrollViewChildren();
        if (destinations.isEmpty()) {
            var empty = new Label();
            empty.setText(Component.translatable("gui.starboundmc.teleporter.empty"));
            empty.addClass("machine-empty-state");
            empty.layout(layout -> layout.widthPercent(100).height(26));
            empty.textStyle(style -> style
                    .textAlignHorizontal(Horizontal.CENTER)
                    .textAlignVertical(Vertical.CENTER));
            destinationList.addScrollViewChild(empty);
            return;
        }

        for (TeleporterListPacket.Entry entry : destinations) {
            var button = new Button();
            button.noText();
            button.addClasses("machine-button", "teleporter-destination");
            button.layout(layout -> layout
                    .widthPercent(100)
                    .height(18)
                    .alignItems(AlignItems.CENTER)
                    .justifyContent(AlignContent.FLEX_START));
            button.style(style -> style.tooltips(destinationLabel(entry)));

            var nodeLamp = new UIElement().addClass("teleporter-node-lamp");
            nodeLamp.layout(layout -> layout.width(3).height(10).marginRight(5));

            var typeLabel = new Label();
            typeLabel.setText(destinationType(entry));
            typeLabel.addClass("teleporter-destination-type");
            typeLabel.layout(layout -> layout.width(29).height(12).marginLeft(4));
            typeLabel.textStyle(style -> style
                    .textAlignHorizontal(Horizontal.CENTER)
                    .textAlignVertical(Vertical.CENTER));

            var nameLabel = new Label();
            nameLabel.setText(destinationName(entry));
            nameLabel.addClass("teleporter-destination-name");
            nameLabel.layout(layout -> layout.flex(1).height(12));
            nameLabel.textStyle(style -> style
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER)
                    .textWrap(TextWrap.HOVER_ROLL)
                    .rollSpeed(0.55F)
                    .adaptiveWidth(false));

            var routeArrow = new Label();
            routeArrow.setText(Component.literal(">"));
            routeArrow.addClass("teleporter-route-arrow");
            routeArrow.layout(layout -> layout.width(6).height(12).marginLeft(2));
            routeArrow.textStyle(style -> style
                    .textAlignHorizontal(Horizontal.CENTER)
                    .textAlignVertical(Vertical.CENTER));

            button.addChildren(nodeLamp, nameLabel, typeLabel, routeArrow);
            button.setOnClick(event -> {
                ModNetwork.sendToServer(new TeleporterUsePacket(entry.key()));
                event.stopPropagation();
            });
            destinationList.addScrollViewChild(button);
        }
    }

    private static Component destinationLabel(TeleporterListPacket.Entry entry) {
        return Component.empty()
                .append(destinationType(entry))
                .append("  ")
                .append(destinationName(entry));
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
        ModNetwork.sendToServer(new TeleporterRenamePacket(nameField.getValue()));
    }
}
