package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** A positioned, independently hit-testable celestial node. */
final class StarmapNodeElement extends UIElement {
    private final StarmapTerminalRoot root;
    private final StarSystemRef systemRef;
    private final com.starboundmc.world.starmap.PlanetEntry entry;
    private final boolean centralStar;
    private NodePlacement simulationPlacement = new NodePlacement(0, 0, 0, false, false);
    private NodePlacement renderPlacement = simulationPlacement;
    private ResourceLocation styledTextureId;
    private boolean styledSelected;
    private float styledSize = -1;
    private boolean styleInitialized;

    StarmapNodeElement(StarmapTerminalRoot root, StarSystemRef systemRef,
                       com.starboundmc.world.starmap.PlanetEntry entry, boolean centralStar) {
        this.root = root;
        this.systemRef = systemRef;
        this.entry = entry;
        this.centralStar = centralStar;
        addClass("starmap-node");
        layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE));
        // Selection brackets and the galaxy star-spark intentionally extend
        // beyond the node box. Keep them visible even if a theme later adds
        // an overflow rule to the node class.
        setOverflowVisible(true);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DOUBLE_CLICK, this::onDoubleClick);
        refresh();
    }

    private void onMouseDown(UIEvent event) {
        if (event.button != 0)
            return;
        if (entry == null && root.getLevel() == StarmapLevel.GALAXY
                && !root.isSystemSelectable(systemRef.system())) {
            // A pre-hyperdrive system is rendered as a signal trace only; it
            // must not become a hidden selection path through the node layer.
            event.stopPropagation();
            return;
        }
        // A system overview shows moons as orientation dots only. They must
        // not turn into a second selection target when the user clicks them;
        // the root maps the click to the owning planet instead.
        if (entry != null && entry.isMoon() && root.getLevel() == StarmapLevel.SYSTEM) {
            root.selectEntry(entry);
            event.stopPropagation();
            return;
        }
        if (entry == null) {
            if (root.getLevel() == StarmapLevel.SYSTEM)
                root.selectCentralStar(systemRef.system());
            else
                root.selectSystem(systemRef.system());
        } else {
            root.selectEntry(entry);
        }
        event.stopPropagation();
    }

    private void onDoubleClick(UIEvent event) {
        if (event.button != 0 || entry != null || root.getLevel() != StarmapLevel.GALAXY)
            return;
        root.enterSystem(systemRef.system());
        event.stopPropagation();
    }

    @Override
    public boolean isIntersectWithPoint(double localX, double localY) {
        if (!renderPlacement.visible())
            return false;
        if (entry == null && root.getLevel() == StarmapLevel.GALAXY
                && !root.isSystemSelectable(systemRef.system()))
            return false;
        float centerX = root.getPositionX() + renderPlacement.x();
        float centerY = root.getPositionY() + renderPlacement.y();
        float radius = StarmapHitGeometry.radius(root.getLevel(), entry == null,
                entry != null && entry.isMoon(), entry == root.getFocusedPlanet(),
                root.viewTransform().scale());
        return StarmapHitGeometry.contains(localX, localY, centerX, centerY, radius);
    }

    void refresh() {
        int width = Math.max(1, Math.round(root.getSizeWidth()));
        int height = Math.max(1, Math.round(root.getSizeHeight()));
        NodePlacement placement = root.nodePlacement(systemRef.system(), entry, centralStar, width, height);
        simulationPlacement = placement;
        renderPlacement = placement;
        setDisplay(placement.visible());
        if (!placement.visible())
            return;
        float size = placement.size();
        layout(layout -> layout.left(placement.x() - size / 2F).top(placement.y() - size / 2F)
                .width(size).height(size));
        updateStyle(size, placement.selected());
    }

    /**
     * Prepare a sub-tick visual position without invalidating Taffy's layout.
     * The hit box remains at the last simulation position, while rendering is
     * translated by the fractional position below.
     */
    void prepareRender(double phaseClock) {
        if (!simulationPlacement.visible()) {
            renderPlacement = simulationPlacement;
            return;
        }
        int width = Math.max(1, Math.round(root.getSizeWidth()));
        int height = Math.max(1, Math.round(root.getSizeHeight()));
        renderPlacement = root.nodePlacement(systemRef.system(), entry, centralStar,
                width, height, phaseClock);
    }

    @Override
    public void drawBackgroundTexture(GUIContext context) {
        if (!renderPlacement.visible())
            return;
        float targetX = root.getPositionX() + renderPlacement.x() - renderPlacement.size() / 2F;
        float targetY = root.getPositionY() + renderPlacement.y() - renderPlacement.size() / 2F;
        float dx = targetX - getPositionX();
        float dy = targetY - getPositionY();
        if (Math.abs(dx) < 0.001F && Math.abs(dy) < 0.001F) {
            super.drawBackgroundTexture(context);
            return;
        }
        context.graphics.pose().pushPose();
        context.graphics.pose().translate(dx, dy, 0);
        super.drawBackgroundTexture(context);
        context.graphics.pose().popPose();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        if (!renderPlacement.visible())
            return;
        float targetX = root.getPositionX() + renderPlacement.x() - renderPlacement.size() / 2F;
        float targetY = root.getPositionY() + renderPlacement.y() - renderPlacement.size() / 2F;
        float dx = targetX - getPositionX();
        float dy = targetY - getPositionY();
        GuiGraphics graphics = context.graphics;
        graphics.pose().pushPose();
        graphics.pose().translate(dx, dy, 0);
        if (entry == null && root.getLevel() == StarmapLevel.GALAXY) {
            if (root.isSystemRevealed(systemRef.system()))
                drawGeminiSpark(graphics);
            else
                drawUnknownSignal(graphics);
        }
        graphics.pose().popPose();
    }

    /** Four tapered, softly layered points inspired by the Gemini sparkle mark. */
    private void drawGeminiSpark(GuiGraphics graphics) {
        int centerX = Math.round(getPositionX() + getSizeWidth() / 2F);
        int centerY = Math.round(getPositionY() + getSizeHeight() / 2F);
        int half = Math.max(7, Math.round(getSizeWidth() * 0.68F));
        int rgb = systemRef.system().getStarColor() & 0x00FFFFFF;
        for (int offset = -half; offset <= half; offset++) {
            int distance = Math.abs(offset);
            int taper = Math.max(1, Math.round((half - distance) * 0.30F) + 1);
            int glowAlpha = Math.max(18, 66 - distance * 3);
            int coreAlpha = Math.max(70, 220 - distance * 12);
            int glow = (glowAlpha << 24) | rgb;
            int core = (coreAlpha << 24) | rgb;
            graphics.fill(centerX - taper - 1, centerY + offset,
                    centerX + taper + 2, centerY + offset + 1, glow);
            graphics.fill(centerX + offset, centerY - taper - 1,
                    centerX + offset + 1, centerY + taper + 2, glow);
            graphics.fill(centerX - taper, centerY + offset,
                    centerX + taper + 1, centerY + offset + 1, core);
            graphics.fill(centerX + offset, centerY - taper,
                    centerX + offset + 1, centerY + taper + 1, core);
        }
        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFFFFFFFF);
    }

    /** A quiet, non-interactive trace used for systems hidden by the hyperdrive gate. */
    private void drawUnknownSignal(GuiGraphics graphics) {
        int centerX = Math.round(getPositionX() + getSizeWidth() / 2F);
        int centerY = Math.round(getPositionY() + getSizeHeight() / 2F);
        float radius = Math.max(5.0F, getSizeWidth() * 0.46F);
        int color = 0x8A81909A;
        StarmapVectorDrawing.drawOrbit(graphics, centerX, centerY, radius, color, true);
        StarmapVectorDrawing.drawDashedLine(graphics, centerX - radius * 0.72F, centerY,
                centerX + radius * 0.72F, centerY, color);
        StarmapVectorDrawing.drawDashedLine(graphics, centerX, centerY - radius * 0.72F,
                centerX, centerY + radius * 0.72F, color);
    }

    private void updateStyle(float size, boolean selected) {
        ResourceLocation texture = entry == null ? null : root.nodeTexture(entry);
        if (styleInitialized && styledSize == size && styledSelected == selected
                && java.util.Objects.equals(styledTextureId, texture))
            return;
        styledSize = size;
        styledSelected = selected;
        styledTextureId = texture;
        styleInitialized = true;
        if (selected) addClass("starmap-node-selected");
        else removeClass("starmap-node-selected");
        if (entry == null && root.getLevel() == StarmapLevel.GALAXY
                && !root.isSystemRevealed(systemRef.system()))
            addClass("starmap-node-unknown");
        else
            removeClass("starmap-node-unknown");
        if (entry != null) {
            style(style -> style.backgroundTexture(root.bodyTexture(entry, size, texture)));
        } else {
            if (root.getLevel() == StarmapLevel.GALAXY) {
                style(style -> style.backgroundTexture(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY));
                return;
            }
            style(style -> style.backgroundTexture(SDFRectTexture.of(systemRef.system().getStarColor())
                    .setRadius(size / 2F).setBorderColor(0x885B91A5).setStroke(1)));
        }
    }

    record StarSystemRef(com.starboundmc.world.starmap.StarSystem system) {}
    record NodePlacement(float x, float y, float size, boolean visible, boolean selected) {}
}
