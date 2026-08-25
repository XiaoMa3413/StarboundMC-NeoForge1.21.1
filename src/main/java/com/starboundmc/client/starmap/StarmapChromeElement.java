package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import net.minecraft.network.chat.Component;

/** LDLib2 chrome layer: frame, level marker and the contextual back hint. */
final class StarmapChromeElement extends UIElement {
    private static final int FRAME = 0xFF24485B;
    private static final int ACCENT = 0xFF63E2DF;
    private static final int MUTED = 0xFF8CA2B3;

    private final StarmapTerminalRoot root;
    private final Label levelLabel = new Label();
    private final Label backHint = new Label();
    private final Button resetView = new Button();
    private final Button focusTarget = new Button();

    StarmapChromeElement(StarmapTerminalRoot root) {
        this.root = root;
        addClasses("starmap-chrome", "starmap-layer");
        layout(layout -> layout.widthPercent(100).heightPercent(100)
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE));
        setAllowHitTest(false);

        UIElement frame = new UIElement().addClass("starmap-frame")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .left(8).top(8).right(8).bottom(8))
                .style(style -> style.backgroundTexture(new ColorBorderTexture(2, FRAME)));
        frame.setAllowHitTest(false);
        UIElement innerFrame = new UIElement().addClass("starmap-frame-inner")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .left(12).top(12).right(12).bottom(12))
                .style(style -> style.backgroundTexture(new ColorBorderTexture(1, 0x66305C70)));
        innerFrame.setAllowHitTest(false);
        UIElement separator = new UIElement().addClass("starmap-header-rule")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .left(22).top(31).right(22).height(1))
                .style(style -> style.backgroundTexture(SDFRectTexture.of(0x664B8EA0)));
        separator.setAllowHitTest(false);

        levelLabel.addClass("starmap-level-label");
        levelLabel.layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(22).top(16).width(104).height(12));
        levelLabel.textStyle(style -> style.textColor(ACCENT).fontSize(9)
                .textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER)
                .textShadow(true));
        levelLabel.setAllowHitTest(false);

        backHint.addClass("starmap-back-hint");
        backHint.layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .right(22).top(16).width(180).height(12));
        backHint.textStyle(style -> style.textColor(MUTED).fontSize(7)
                .textAlignHorizontal(Horizontal.RIGHT).textAlignVertical(Vertical.CENTER));
        backHint.setAllowHitTest(false);

        UIElement viewControls = new UIElement().addClass("starmap-view-controls")
                .layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .left(132).top(14).width(64).height(16)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW).gapAll(2));
        configureViewButton(resetView);
        configureViewButton(focusTarget);
        resetView.setOnClick(event -> {
            root.resetView();
            event.stopPropagation();
        });
        focusTarget.setOnClick(event -> {
            root.focusSelectedView();
            event.stopPropagation();
        });
        viewControls.addChildren(resetView, focusTarget);
        // Match the proven information-panel fallback: resolve the button
        // during capture as well, so clicks landing on the adaptive text
        // child cannot bypass the intended action on some LDLib2 layouts.
        viewControls.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button != 0)
                return;
            if (resetView.isActive() && resetView.isIntersectWithPoint(event.x, event.y))
                root.resetView();
            else if (focusTarget.isActive()
                    && focusTarget.isIntersectWithPoint(event.x, event.y))
                root.focusSelectedView();
        }, true);
        // These controls float over the scene. Their clicks and wheel input
        // must not start a map drag or zoom the view behind the button.
        viewControls.stopInteractionEventsPropagation();

        addChildren(frame, innerFrame, separator, levelLabel, viewControls, backHint);
        addEventListener(UIEvents.TICK, event -> refresh());
        refresh();
    }

    void refresh() {
        levelLabel.setText(root.levelLabel());
        resetView.setText(Component.translatable("gui.starboundmc.starmap.redraw.reset_view"));
        resetView.setActive(!root.isViewReset());
        focusTarget.setText(Component.translatable("gui.starboundmc.starmap.redraw.focus_target"));
        focusTarget.setActive(root.canFocusView());
        boolean canGoBack = root.getLevel() != StarmapLevel.GALAXY;
        backHint.setDisplay(true);
        backHint.setText(Component.translatable(canGoBack
                ? "gui.starboundmc.starmap.redraw.back_hint"
                : "gui.starboundmc.starmap.redraw.close_hint"));
    }

    private static void configureViewButton(Button button) {
        button.addClasses("starmap-view-button", "starmap-button");
        button.layout(layout -> layout.flex(1).height(16));
        button.textStyle(style -> style.textColor(MUTED).fontSize(6.0F)
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));
        button.buttonStyle(style -> style
                .baseTexture(SDFRectTexture.of(0xB0102632).setRadius(2)
                        .setStroke(1).setBorderColor(0xAA315F70))
                .hoverTexture(SDFRectTexture.of(0xE0183B47).setRadius(2)
                        .setStroke(1).setBorderColor(ACCENT))
                .pressedTexture(SDFRectTexture.of(0xE00B1B24).setRadius(2)
                        .setStroke(1).setBorderColor(0xFF3B8490)));
    }
}
