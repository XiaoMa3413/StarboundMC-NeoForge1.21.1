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
import net.minecraft.resources.ResourceLocation;
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

        shell.addChildren(header, content);
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

        var headerMark = new UIElement().addClass("teleporter-header-mark");
        headerMark.setAllowHitTest(false);
        headerMark.layout(layout -> layout.width(3).height(11).marginRight(6));

        var title = new Label();
        title.setText(Component.translatable("container.starboundmc.teleporter"));
        title.addClass("machine-title");
        title.layout(layout -> layout.flex(1).height(10));
        title.textStyle(style -> style.textAlignVertical(Vertical.CENTER));

        var closeHint = new Label();
        closeHint.setText(Component.translatable("gui.starboundmc.teleporter.close_hint"));
        closeHint.addClass("machine-key-hint");
        closeHint.layout(layout -> layout.width(48).height(15));
        closeHint.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));

        header.addChildren(headerMark, title, closeHint);
        return header;
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
                    .height(24)
                    .alignItems(AlignItems.CENTER)
                    .justifyContent(AlignContent.FLEX_START));
            button.style(style -> style.tooltips(destinationLabel(entry)));

            var icon = buildDestinationIcon(entry.type());

            var textColumn = new UIElement().addClass("teleporter-destination-copy");
            textColumn.layout(layout -> layout
                    .flex(1)
                    .height(18)
                    .justifyContent(AlignContent.CENTER)
                    .flexDirection(FlexDirection.COLUMN));

            var nameLabel = new Label();
            nameLabel.setText(destinationName(entry));
            nameLabel.addClass("teleporter-destination-name");
            nameLabel.layout(layout -> layout.widthPercent(100).height(10));
            nameLabel.textStyle(style -> style
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER)
                    .textWrap(TextWrap.HOVER_ROLL)
                    .rollSpeed(0.55F)
                    .adaptiveWidth(false));

            var typeLabel = new Label();
            typeLabel.setText(destinationType(entry));
            typeLabel.addClass("teleporter-destination-type");
            typeLabel.layout(layout -> layout.widthPercent(100).height(7));
            typeLabel.textStyle(style -> style
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER));
            textColumn.addChildren(nameLabel, typeLabel);

            var routeArrow = new Label();
            routeArrow.setText(Component.literal(">"));
            routeArrow.addClass("teleporter-route-arrow");
            routeArrow.layout(layout -> layout.width(6).height(12).marginLeft(2));
            routeArrow.textStyle(style -> style
                    .textAlignHorizontal(Horizontal.CENTER)
                    .textAlignVertical(Vertical.CENTER));

            button.addChildren(icon, textColumn, routeArrow);
            button.setOnClick(event -> {
                ModNetwork.sendToServer(new TeleporterUsePacket(entry.key()));
                event.stopPropagation();
            });
            destinationList.addScrollViewChild(button);
        }
    }

    /** Builds a small semantic destination mark from LDLib2 elements. */
    private static UIElement buildDestinationIcon(byte type) {
        var icon = new UIElement().addClass("teleporter-destination-icon");
        icon.setAllowHitTest(false);
        icon.layout(layout -> layout
                .width(20)
                .height(20)
                .marginRight(6));

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
