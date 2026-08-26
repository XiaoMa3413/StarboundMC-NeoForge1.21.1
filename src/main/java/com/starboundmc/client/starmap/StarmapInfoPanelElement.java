package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.network.chat.Component;
import com.starboundmc.world.starmap.PlanetEntry;

import java.util.List;
import java.util.Objects;

/** Floating detail card built from LDLib2 elements rather than raw draw calls. */
final class StarmapInfoPanelElement extends UIElement {
    private static final int TEXT = 0xFFEAF5F7;
    private static final int ACCENT = 0xFF63E2DF;
    private static final int MUTED = 0xFF8CA2B3;
    private static final int BUTTON = 0xFF1A4B57;

    private final StarmapTerminalRoot root;
    private final Transform2D panelTransform = Transform2D.identity().pivot(0.0F, 0.0F);
    private final UIElement panel;
    private final UIElement panelSurface;
    private final UIElement content;
    private final UIElement preview;
    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final Label metadata = new Label();
    private final Label satelliteCount = new Label();
    private final Label status = new Label();
    private final Label description = new Label();
    private final Button action = new Button();
    private StarmapInfoPanelPlacement.Placement placement;
    private boolean targetVisible;
    private boolean panelExpanded;
    private String contentKey;
    private ISubscription surfaceAnimation = StarmapUiAnimations.none();
    private ISubscription contentAnimation = StarmapUiAnimations.none();

    StarmapInfoPanelElement(StarmapTerminalRoot root) {
        this.root = root;
        addClasses("starmap-info-layer", "starmap-layer");
        layout(layout -> layout.widthPercent(100).heightPercent(100)
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE));
        // The full-screen layer must stay transparent to hit testing; its
        // panel child remains hit-testable so nested buttons receive clicks.
        setAllowHitTest(false);

        panel = new UIElement().addClass("starmap-info-anchor")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .width(220).height(126))
                .style(style -> style.transform2D(panelTransform));
        panel.setAllowHitTest(false);

        panelSurface = new UIElement().addClasses("starmap-info-panel", "starmap-panel")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .left(0).top(0).widthPercent(100).heightPercent(100))
                .style(style -> style.backgroundTexture(
                        SDFRectTexture.of(0xE00A1420).setRadius(4).setStroke(1)
                                .setBorderColor(0xFF2F6676))
                        .transform2D(fullPanelTransform()));
        panelSurface.setAllowHitTest(false);

        content = new UIElement();
        content.addClass("starmap-info-content");
        content.layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(0).top(0).widthPercent(100).heightPercent(100).paddingAll(8));
        content.style(style -> style.zIndex(1));

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
        satelliteCount.addClass("starmap-info-satellites");
        satelliteCount.layout(layout -> layout.widthPercent(100).height(11));
        satelliteCount.textStyle(style -> style.textColor(ACCENT).fontSize(7));
        satelliteCount.setDisplay(false);
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

        content.addChildren(header, metadata, satelliteCount, status, description, action);
        panel.addChildren(panelSurface, content);
        // The panel is an absolutely positioned overlay. Handle the action
        // during capture as a fallback for clicks landing on the button's
        // text child; this keeps the command reliable across LDLib2 layouts.
        panel.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            double[] point = { event.x, event.y };
            panelTransform.inversePoint(panel, point);
            if (event.button == 0 && action.isDisplayed()
                    && action.isIntersectWithPoint(point[0], point[1]))
                onActionClick(event);
        }, true);
        addChild(panel);
        addEventListener(UIEvents.REMOVED, event -> {
            cancelAnimations();
        });
        setDisplay(false);
        refresh();
    }

    private void onActionClick(UIEvent event) {
        root.performActionFromUi();
        event.stopPropagation();
    }

    void prepareFrame(int width, int height) {
        if (!isDisplayed() || !targetVisible)
            return;
        StarmapInfoPanelPlacement.Placement next = root.infoPanelPlacement(width, height);
        if (next.equals(placement))
            return;
        placement = next;
        panelTransform.translate(next.x(), next.y());
        panel.clearPoseCache();
    }

    boolean containsLocalPoint(float x, float y) {
        return placement != null && placement.contains(x, y);
    }

    void refresh() {
        PlanetEntry entry = root.getSelectedEntry();
        var system = root.getSelectedSystem();
        boolean visible = root.isInfoPanelVisible() && (system != null || entry != null);
        String nextContentKey = visible ? root.selectionTargetKey() : null;
        boolean visibilityChanged = visible != targetVisible;
        if (visibilityChanged) {
            targetVisible = visible;
            contentKey = nextContentKey;
            if (visible)
                showAnimated();
            else
                hideAnimated();
        } else if (visible && !Objects.equals(nextContentKey, contentKey)) {
            contentKey = nextContentKey;
            if (panelExpanded)
                playContentReveal(0x48FFFFFF);
            else
                content.setDisplay(false);
        }
        if (!visible)
            return;

        int width = Math.max(1, Math.round(root.getSizeWidth()));
        int height = Math.max(1, Math.round(root.getSizeHeight()));
        StarmapInfoPanelPlacement.Placement next = root.infoPanelPlacement(width, height);
        panel.layout(layout -> layout.left(0).top(0).width(next.width()).height(next.height()));
        prepareFrame(width, height);
        title.setText(entry == null ? Component.translatable(system.getNameKey())
                : Component.translatable(entry.getNameKey()));
        subtitle.setText(entry == null ? Component.translatable(system.getStarTypeKey())
                : Component.translatable(entry.getTypeKey()));

        if (entry == null) {
            satelliteCount.setDisplay(false);
            metadata.setText(Component.translatable(
                    "gui.starboundmc.starmap.redraw.body_count", system.getEntries().size()));
            description.setText(Component.translatable(system.getDescriptionKey()));
            boolean galaxy = root.getLevel() == StarmapLevel.GALAXY;
            boolean canEnter = galaxy && root.canEnterSelectedSystem();
            Component reason = galaxy ? root.actionStatus() : null;
            status.setDisplay(reason != null);
            if (reason != null)
                status.setText(reason);
            action.setDisplay(galaxy);
            if (galaxy) {
                action.setText(root.actionLabel());
                action.setActive(canEnter);
            }
            preview.style(style -> style.backgroundTexture(SDFRectTexture.of(system.getStarColor())
                    .setRadius(16).setBorderColor(ACCENT).setStroke(1)));
        } else {
            int moons = system == null ? 0 : system.getMoonCount(entry);
            satelliteCount.setDisplay(moons > 0);
            if (moons > 0)
                satelliteCount.setText(Component.translatable(
                        "gui.starboundmc.starmap.redraw.moon_count", moons));
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
            preview.style(style -> style.backgroundTexture(
                    root.previewBodyTexture(entry, 32.0F)));
        }
    }

    private void showAnimated() {
        cancelAnimations();
        panelExpanded = false;
        panel.setAllowHitTest(false);
        content.setDisplay(false);
        panelSurface.style(style -> style.transform2D(linePanelTransform()));
        setDisplay(true);
        surfaceAnimation = StarmapUiAnimations.transform(panelSurface,
                linePanelTransform(), fullPanelTransform(),
                StarmapUiAnimations.PANEL_EXPAND_DURATION, Eases.QUAD_OUT, () -> {
                    if (!targetVisible)
                        return;
                    panelExpanded = true;
                    playContentReveal(0x00FFFFFF);
                });
    }

    private void hideAnimated() {
        cancelAnimations();
        panelExpanded = false;
        panel.setAllowHitTest(false);
        content.setDisplay(false);
        Transform2D from = panelSurface.getStyle().transform2D().copy();
        surfaceAnimation = StarmapUiAnimations.transform(panelSurface,
                from, linePanelTransform(), StarmapUiAnimations.PANEL_COLLAPSE_DURATION,
                Eases.QUAD_IN, () -> {
                    if (!targetVisible)
                        setDisplay(false);
                });
    }

    private void playContentReveal(int fromColor) {
        contentAnimation.unsubscribe();
        content.setDisplay(true);
        contentAnimation = StarmapUiAnimations.tintTogether(List.of(
                        title, subtitle, metadata, satelliteCount, status, description),
                fromColor, 0xFFFFFFFF,
                StarmapUiAnimations.PANEL_CONTENT_DURATION, () -> {
                    if (targetVisible)
                        panel.setAllowHitTest(true);
                });
    }

    private void cancelAnimations() {
        surfaceAnimation.unsubscribe();
        contentAnimation.unsubscribe();
        surfaceAnimation = StarmapUiAnimations.none();
        contentAnimation = StarmapUiAnimations.none();
    }

    private static Transform2D linePanelTransform() {
        return Transform2D.identity().pivot(0.5F, 0.5F).scale(1.0F, 0.018F);
    }

    private static Transform2D fullPanelTransform() {
        return Transform2D.identity().pivot(0.5F, 0.5F);
    }
}
