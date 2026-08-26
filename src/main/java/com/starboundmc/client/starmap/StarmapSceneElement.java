package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarmapGalaxyGraph;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.gui.GuiGraphics;

/** Non-interactive LDLib2 scene layer for stars, routes and orbital guides. */
final class StarmapSceneElement extends UIElement {
    private static final int MUTED = 0xFF8CA2B3;
    private static final int ORBIT = 0x665B91A5;
    private static final int ORBIT_SELECTED = 0xB563E2DF;

    private final StarmapTerminalRoot root;
    private final StarmapStarfieldCache starfield = new StarmapStarfieldCache();

    StarmapSceneElement(StarmapTerminalRoot root) {
        this.root = root;
        addClass("starmap-scene-layer");
        layout(layout -> layout.widthPercent(100).heightPercent(100)
                .positionType(TaffyPosition.ABSOLUTE));
        setAllowHitTest(false);
        addEventListener(UIEvents.REMOVED, event -> starfield.release());
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        GuiGraphics graphics = context.graphics;
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY());
        int width = Math.max(1, Math.round(getSizeWidth()));
        int height = Math.max(1, Math.round(getSizeHeight()));
        starfield.draw(graphics, x, y, width, height);
        if (root.getLevel() == StarmapLevel.GALAXY)
            drawGalaxy(graphics, x, y, width, height);
        else if (root.getLevel() == StarmapLevel.SYSTEM)
            drawSystem(graphics, x, y, width, height);
        else
            drawPlanet(graphics, x, y, width, height);
        graphics.flush();
    }

    private void drawGalaxy(GuiGraphics graphics, int x, int y, int width, int height) {
        for (StarmapGalaxyGraph.Route route : root.galaxyGraph().routes()) {
            StarmapGalaxyGraph.Node from = root.galaxyGraph().node(route.fromId());
            StarmapGalaxyGraph.Node to = root.galaxyGraph().node(route.toId());
            if (from == null || to == null)
                continue;
            float[] start = root.galaxyPointF(from.system(), x, y, width, height);
            float[] end = root.galaxyPointF(to.system(), x, y, width, height);
            StarmapVectorDrawing.drawDashedLine(graphics, start[0], start[1], end[0], end[1],
                    route.available() ? MUTED : 0x66566B75);
        }
    }

    private void drawSystem(GuiGraphics graphics, int x, int y, int width, int height) {
        var system = root.getSelectedSystem();
        if (system == null)
            return;
        StarmapViewTransform.Point center = root.viewTransform().toScreen(
                width / 2.0F, height / 2.0F, width, height);
        float centerX = x + center.x();
        float centerY = y + center.y();
        for (PlanetEntry entry : system.getEntries()) {
            if (entry.isMoon())
                continue;
            float radius = root.systemOrbitRadius(entry, width, height);
            StarmapVectorDrawing.drawOrbit(graphics, centerX, centerY, radius,
                    entry == root.getSelectedEntry() ? ORBIT_SELECTED : ORBIT);
            float[] point = root.systemPointF(entry, width, height, root.renderOrbitClock());
            for (PlanetEntry moon : system.getEntries()) {
                if (!moon.isMoon()
                        || !java.util.Objects.equals(moon.getParentEntryId(), entry.getEntryId()))
                    continue;
                StarmapVectorDrawing.drawOrbit(graphics, x + point[0], y + point[1],
                        root.viewTransform().scaleLength(root.moonDisplayRadius(
                                system, moon, width, height, false)),
                        0x453D7182, true);
            }
        }
    }

    private void drawPlanet(GuiGraphics graphics, int x, int y, int width, int height) {
        var system = root.getSelectedSystem();
        var focusedPlanet = root.getFocusedPlanet();
        if (system == null || focusedPlanet == null)
            return;
        StarmapViewTransform.Point center = root.viewTransform().toScreen(
                width / 2.0F, height / 2.0F, width, height);
        float centerX = x + center.x();
        float centerY = y + center.y();
        for (PlanetEntry entry : system.getEntries()) {
            if (!entry.isMoon() || !entry.getParentEntryId().equals(focusedPlanet.getEntryId()))
                continue;
            float radius = root.viewTransform().scaleLength(root.moonDisplayRadius(
                    system, entry, width, height, true));
            StarmapVectorDrawing.drawOrbit(graphics, centerX, centerY, radius,
                    entry == root.getSelectedEntry() ? ORBIT_SELECTED : ORBIT);
        }
    }
}
