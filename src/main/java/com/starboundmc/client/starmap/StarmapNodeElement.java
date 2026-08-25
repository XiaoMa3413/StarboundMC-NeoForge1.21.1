package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.resources.ResourceLocation;

/** A positioned, independently hit-testable celestial node. */
final class StarmapNodeElement extends UIElement {
    private final StarmapTerminalRoot root;
    private final StarSystemRef systemRef;
    private final com.starboundmc.world.starmap.PlanetEntry entry;
    private final boolean centralStar;
    private NodePlacement simulationPlacement = new NodePlacement(0, 0, 0, false, false);
    private NodePlacement renderPlacement = simulationPlacement;
    private String styledTextureId;
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
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        refresh();
    }

    private void onMouseDown(UIEvent event) {
        if (event.button != 0)
            return;
        // A system overview shows moons as orientation dots only. They must
        // not turn into a second selection target when the user clicks them.
        if (entry != null && entry.isMoon() && root.getLevel() == StarmapLevel.SYSTEM) {
            event.stopPropagation();
            return;
        }
        if (entry == null)
            root.selectSystem(systemRef.system());
        else
            root.selectEntry(entry);
        event.stopPropagation();
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

    private void updateStyle(float size, boolean selected) {
        String texture = entry == null ? null : root.nodeTexture(entry);
        if (styleInitialized && styledSize == size && styledSelected == selected
                && java.util.Objects.equals(styledTextureId, texture))
            return;
        styledSize = size;
        styledSelected = selected;
        styledTextureId = texture;
        styleInitialized = true;
        if (selected) addClass("starmap-node-selected");
        else removeClass("starmap-node-selected");
        if (entry != null) {
            if (texture != null) {
                style(style -> style.backgroundTexture(SpriteTexture.of(ResourceLocation.parse(texture))));
                return;
            }
            style(style -> style.backgroundTexture(SDFRectTexture.of(
                    selected ? 0xFF63E2DF : entry.getVisual().getPrimaryColor())
                    .setRadius(Math.max(2, size / 3F))
                    .setBorderColor(selected ? 0xFFB8FFFF : 0x886B94A4).setStroke(selected ? 2 : 1)));
        } else {
            style(style -> style.backgroundTexture(SDFRectTexture.of(systemRef.system().getStarColor())
                    .setRadius(size / 2F).setBorderColor(selected ? 0xFF63E2DF : 0x885B91A5)
                    .setStroke(selected ? 2 : 1)));
        }
    }

    record StarSystemRef(com.starboundmc.world.starmap.StarSystem system) {}
    record NodePlacement(float x, float y, float size, boolean visible, boolean selected) {}
}
