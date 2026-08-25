package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.starboundmc.world.starmap.PlanetEntry;

/** Floating detail card built from LDLib2 elements rather than raw draw calls. */
final class StarmapInfoPanelElement extends UIElement {
    private static final int TEXT = 0xFFEAF5F7;
    private static final int ACCENT = 0xFF63E2DF;
    private static final int MUTED = 0xFF8CA2B3;
    private static final int BUTTON = 0xFF1A4B57;

    private final StarmapTerminalRoot root;
    private final UIElement panel;
    private final UIElement preview;
    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final Label metadata = new Label();
    private final Label status = new Label();
    private final Label description = new Label();
    private final Button action = new Button();

    StarmapInfoPanelElement(StarmapTerminalRoot root) {
        this.root = root;
        addClasses("starmap-info-layer", "starmap-layer");
        layout(layout -> layout.widthPercent(100).heightPercent(100)
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE));
        // The full-screen layer must stay transparent to hit testing; its
        // panel child remains hit-testable so nested buttons receive clicks.
        setAllowHitTest(false);

        panel = new UIElement().addClasses("starmap-info-panel", "starmap-panel")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .width(220).height(126).paddingAll(8))
                .style(style -> style.backgroundTexture(
                        SDFRectTexture.of(0xE00A1420).setRadius(4).setStroke(1)
                                .setBorderColor(0xFF2F6676)));
        panel.setAllowHitTest(true);

        preview = new UIElement().addClass("starmap-info-preview")
                .layout(layout -> layout.width(32).height(32).marginRight(7))
                .style(style -> style.backgroundTexture(SDFRectTexture.of(0xFF173342)
                        .setRadius(3).setBorderColor(0xFF3B8490).setStroke(1)));
        UIElement header = new UIElement().addClass("starmap-info-header")
                .layout(layout -> layout.widthPercent(100).height(34)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW));
        title.addClass("starmap-info-title");
        title.layout(layout -> layout.flex(1).height(14));
        title.textStyle(style -> style.textColor(TEXT).fontSize(9).textShadow(true));
        subtitle.addClass("starmap-info-subtitle");
        subtitle.layout(layout -> layout.flex(1).height(12));
        subtitle.textStyle(style -> style.textColor(ACCENT).fontSize(7));
        UIElement headerText = new UIElement().layout(layout -> layout.flex(1).height(34)
                .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN));
        headerText.addChildren(title, subtitle);
        header.addChildren(preview, headerText);

        metadata.addClass("starmap-info-metadata");
        metadata.layout(layout -> layout.widthPercent(100).height(11));
        metadata.textStyle(style -> style.textColor(MUTED).fontSize(7));
        status.addClass("starmap-info-status");
        status.layout(layout -> layout.widthPercent(100).height(14));
        status.textStyle(style -> style.textColor(0xFFFFB36B).fontSize(7)
                .textWrap(TextWrap.WRAP).textAlignVertical(Vertical.TOP));
        description.addClass("starmap-info-description");
        description.layout(layout -> layout.widthPercent(100).height(28));
        description.textStyle(style -> style.textColor(MUTED).fontSize(7)
                .textWrap(TextWrap.WRAP).textAlignVertical(Vertical.TOP));

        action.addClasses("starmap-action-button", "starmap-button");
        action.layout(layout -> layout.widthPercent(100).height(17));
        action.textStyle(style -> style.textColor(TEXT).fontSize(7)
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));
        action.buttonStyle(style -> style.baseTexture(SDFRectTexture.of(BUTTON).setRadius(2))
                .hoverTexture(SDFRectTexture.of(0xFF276D78).setRadius(2))
                .pressedTexture(SDFRectTexture.of(0xFF123B45).setRadius(2)));
        action.setOnClick(this::onActionClick);

        panel.addChildren(header, metadata, status, description, action);
        // The panel is an absolutely positioned overlay. Handle the action
        // during capture as a fallback for clicks landing on the button's
        // text child; this keeps the command reliable across LDLib2 layouts.
        panel.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0 && action.isDisplayed()
                    && action.isIntersectWithPoint(event.x, event.y))
                onActionClick(event);
        }, true);
        addChild(panel);
        addEventListener(UIEvents.TICK, event -> refresh());
        refresh();
    }

    private void onActionClick(UIEvent event) {
        root.performActionFromUi();
        event.stopPropagation();
    }

    void refresh() {
        PlanetEntry entry = root.getSelectedEntry();
        var system = root.getSelectedSystem();
        boolean visible = root.isInfoPanelVisible() && (system != null || entry != null);
        setDisplay(visible);
        if (!visible)
            return;

        int[] rect = root.infoPanelRect(0, 0, Math.round(root.getSizeWidth()),
                Math.round(root.getSizeHeight()), false);
        panel.layout(layout -> layout.left(rect[0]).top(rect[1]).width(rect[2]).height(rect[3]));
        title.setText(entry == null ? Component.translatable(system.getNameKey())
                : Component.translatable(entry.getNameKey()));
        subtitle.setText(entry == null ? Component.translatable(system.getStarTypeKey())
                : Component.translatable(entry.getTypeKey()));

        if (entry == null) {
            metadata.setText(Component.translatable(
                    "gui.starboundmc.starmap.redraw.body_count", system.getEntries().size()));
            status.setDisplay(false);
            description.setText(Component.translatable(system.getDescriptionKey()));
            boolean canEnter = root.canEnterSelectedSystem();
            action.setDisplay(canEnter);
            action.setText(root.actionLabel());
            action.setActive(canEnter);
            preview.style(style -> style.backgroundTexture(SDFRectTexture.of(system.getStarColor())
                    .setRadius(16).setBorderColor(ACCENT).setStroke(1)));
        } else {
            metadata.setText(Component.translatable("gui.starboundmc.starmap.threat",
                    entry.getThreatLevel()));
            description.setText(Component.translatable(entry.getDescriptionKey()));
            action.setDisplay(true);
            action.setText(root.actionLabel());
            action.setActive(root.isActionAvailable());
            Component reason = root.actionStatus();
            status.setDisplay(reason != null);
            if (reason != null)
                status.setText(reason);
            String texture = root.previewTexture(entry);
            if (texture != null)
                preview.style(style -> style.backgroundTexture(SpriteTexture.of(ResourceLocation.parse(texture))));
            else
                preview.style(style -> style.backgroundTexture(SDFRectTexture.of(entry.getVisual().getPrimaryColor())
                        .setRadius(16).setBorderColor(ACCENT).setStroke(1)));
        }
    }
}
