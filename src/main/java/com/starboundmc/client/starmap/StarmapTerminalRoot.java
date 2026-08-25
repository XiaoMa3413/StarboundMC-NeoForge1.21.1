package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.starboundmc.client.StarmapGeometry;
import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.StartWarpPacket;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Full-screen LDLib2 canvas for the new starmap terminal. */
public final class StarmapTerminalRoot extends UIElement {
    private static final int BASE_WIDTH = 250;
    private static final int BASE_HEIGHT = 220;
    /** Keeps the outer authored orbit inside the framed viewport. */
    private static final int SYSTEM_ORBIT_SCALE = 300;
    private static final int STAR = 0xFFB9D7E5;
    private static final int MUTED = 0xFF8CA2B3;
    private static final int GRID = 0x243A6373;
    private static final int ORBIT = 0x665B91A5;
    private static final int ORBIT_SELECTED = 0xB563E2DF;

    private StarmapLevel level = StarmapLevel.GALAXY;
    private StarSystem selectedSystem;
    private PlanetEntry selectedEntry;
    private PlanetEntry focusedPlanet;
    private final UIElement nodeLayer;
    private final StarmapChromeElement chrome;
    private final StarmapInfoPanelElement infoPanel;
    private final List<StarmapNodeElement> nodes = new ArrayList<>();
    /** Accumulated simulation ticks. Rendering adds a partial tick below. */
    private double orbitClock;
    /** Continuous phase used by the current render pass and hit testing. */
    private double renderOrbitClock;
    private int laidOutWidth;
    private int laidOutHeight;

    public StarmapTerminalRoot() {
        addClass("starmap-redraw-root");
        layout(layout -> layout.widthPercent(100).heightPercent(100));
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);

        nodeLayer = new UIElement().addClass("starmap-node-layer")
                .layout(layout -> layout.widthPercent(100).heightPercent(100)
                        .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE))
                .setAllowHitTest(false);
        for (StarSystem system : StarSystems.all()) {
            StarmapNodeElement.StarSystemRef ref = new StarmapNodeElement.StarSystemRef(system);
            StarmapNodeElement starNode = new StarmapNodeElement(this, ref, null, false);
            nodes.add(starNode);
            nodeLayer.addChild(starNode);
            for (PlanetEntry entry : system.getEntries()) {
                StarmapNodeElement bodyNode = new StarmapNodeElement(this, ref, entry, false);
                nodes.add(bodyNode);
                nodeLayer.addChild(bodyNode);
            }
        }
        chrome = new StarmapChromeElement(this);
        infoPanel = new StarmapInfoPanelElement(this);
        addChildren(nodeLayer, chrome, infoPanel);
        addEventListener(UIEvents.TICK, event -> {
            orbitClock += 1.0D;
            renderOrbitClock = orbitClock;
            refreshComponents();
        });
    }

    private void refreshComponents() {
        nodes.forEach(StarmapNodeElement::refresh);
        chrome.refresh();
        infoPanel.refresh();
    }

    private void onMouseDown(UIEvent event) {
        if (event.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            goBack();
            event.stopPropagation();
            return;
        }
        if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT)
            return;

        float width = getSizeWidth();
        float height = getSizeHeight();
        float localX = event.x - getPositionX();
        float localY = event.y - getPositionY();
        if (level == StarmapLevel.GALAXY) {
            StarSystem system = nearestSystem(localX, localY, width, height);
            if (system != null) {
                if (system == selectedSystem)
                    level = StarmapLevel.SYSTEM;
                else {
                    selectedSystem = system;
                    selectedEntry = null;
                }
            }
        } else if (level == StarmapLevel.SYSTEM && selectedSystem != null) {
            PlanetEntry entry = nearestSystemEntry(localX, localY, width, height);
            if (entry != null) {
                if (entry == selectedEntry) {
                    focusedPlanet = entry;
                    level = StarmapLevel.PLANET;
                } else {
                    selectedEntry = entry;
                }
            }
        } else if (level == StarmapLevel.PLANET) {
            PlanetEntry target = nearestPlanetTarget(localX, localY, width, height);
            if (target != null)
                selectedEntry = target;
        }
        refreshComponents();
    }

    public void goBack() {
        if (level == StarmapLevel.PLANET) {
            level = StarmapLevel.SYSTEM;
            selectedEntry = focusedPlanet;
            focusedPlanet = null;
        } else if (level == StarmapLevel.SYSTEM) {
            level = StarmapLevel.GALAXY;
            selectedEntry = null;
        }
        refreshComponents();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        GuiGraphics graphics = context.graphics;
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY());
        int width = Math.max(1, Math.round(getSizeWidth()));
        int height = Math.max(1, Math.round(getSizeHeight()));
        if (width != laidOutWidth || height != laidOutHeight) {
            laidOutWidth = width;
            laidOutHeight = height;
            refreshComponents();
        }
        // Layout is resolved before the render pass. Keep the UI node boxes at
        // their tick positions, but let each node draw at this interpolated
        // phase so the orbital motion does not snap every 1/20 second.
        renderOrbitClock = orbitClock + context.partialTick;
        nodes.forEach(node -> node.prepareRender(renderOrbitClock));
        drawStars(graphics, x, y, width, height);
        if (level == StarmapLevel.GALAXY)
            drawGalaxy(graphics, x, y, width, height);
        else if (level == StarmapLevel.SYSTEM)
            drawSystem(graphics, x, y, width, height);
        else
            drawPlanet(graphics, x, y, width, height);
        super.drawBackgroundAdditional(context);
    }

    private void drawStars(GuiGraphics graphics, int x, int y, int width, int height) {
        Random random = new Random(0x5EEDL);
        for (int i = 0; i < Math.max(120, width * height / 6500); i++) {
            int px = x + random.nextInt(Math.max(1, width));
            int py = y + random.nextInt(Math.max(1, height));
            int roll = random.nextInt(12);
            int size = roll == 0 ? 3 : roll < 3 ? 2 : 1;
            int color = roll == 0 ? 0xB9D7E5 : roll < 4 ? 0x789BB0 : 0x526B7C;
            graphics.fill(px, py, px + size, py + size, color | 0xFF000000);
            if (roll == 0) {
                graphics.fill(px - 2, py + 1, px + size + 2, py + 2, 0x385B91A5);
                graphics.fill(px + 1, py - 2, px + 2, py + size + 2, 0x385B91A5);
            }
        }
        int gridStep = Math.max(32, Math.min(width, height) / 5);
        for (int gx = x + gridStep; gx < x + width; gx += gridStep)
            graphics.fill(gx, y + 12, gx + 1, y + height - 12, GRID);
        for (int gy = y + gridStep; gy < y + height; gy += gridStep)
            graphics.fill(x + 12, gy, x + width - 12, gy + 1, GRID);
    }

    private void drawGalaxy(GuiGraphics graphics, int x, int y, int width, int height) {
        int[] first = null;
        for (StarSystem system : StarSystems.all()) {
            int[] position = galaxyPoint(system, x, y, width, height);
            if (first != null)
                drawDashedLine(graphics, first[0], first[1], position[0], position[1], MUTED);
            first = position;
        }
    }

    private void drawSystem(GuiGraphics graphics, int x, int y, int width, int height) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        for (PlanetEntry entry : selectedSystem.getEntries()) {
            if (entry.isMoon())
                continue;
            int radius = Math.max(18, entry.getOrbitRadius() * Math.min(width, height)
                    / SYSTEM_ORBIT_SCALE);
            drawOrbit(graphics, centerX, centerY, radius,
                    entry == selectedEntry ? ORBIT_SELECTED : ORBIT);
            float[] point = systemPointF(entry, width, height, renderOrbitClock);
            for (PlanetEntry moon : selectedSystem.getEntries()) {
                if (!moon.isMoon()
                        || !java.util.Objects.equals(moon.getParentEntryId(), entry.getEntryId()))
                    continue;
                drawOrbit(graphics, Math.round(x + point[0]), Math.round(y + point[1]),
                        moonDisplayRadius(selectedSystem, moon, width, height, false),
                        0x453D7182);
            }
        }
    }

    private void drawPlanet(GuiGraphics graphics, int x, int y, int width, int height) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        if (focusedPlanet == null)
            return;
        for (PlanetEntry entry : selectedSystem.getEntries()) {
            if (!entry.isMoon() || !entry.getParentEntryId().equals(focusedPlanet.getEntryId()))
                continue;
            int radius = moonDisplayRadius(selectedSystem, entry, width, height, true);
            drawOrbit(graphics, centerX, centerY, radius,
                    entry == selectedEntry ? ORBIT_SELECTED : ORBIT);
        }
    }

    StarmapLevel getLevel() {
        return level;
    }

    StarSystem getSelectedSystem() {
        return selectedSystem;
    }

    PlanetEntry getSelectedEntry() {
        return selectedEntry;
    }

    Component levelLabel() {
        return Component.translatable("gui.starboundmc.starmap.redraw.level." + level.name().toLowerCase());
    }

    void selectSystem(StarSystem system) {
        if (system == null)
            return;
        if (system == selectedSystem) {
            level = StarmapLevel.SYSTEM;
            selectedEntry = null;
        } else {
            selectedSystem = system;
            selectedEntry = null;
        }
        refreshComponents();
    }

    void selectEntry(PlanetEntry entry) {
        if (entry == null)
            return;
        if (level == StarmapLevel.SYSTEM) {
            if (entry.isMoon())
                entry = StarSystems.entryById(entry.getParentEntryId());
            if (entry == null)
                return;
            if (entry == selectedEntry) {
                focusedPlanet = entry;
                level = StarmapLevel.PLANET;
            } else {
                selectedEntry = entry;
            }
        } else if (level == StarmapLevel.PLANET) {
            selectedEntry = entry;
        }
        refreshComponents();
    }

    void performActionFromUi() {
        performAction();
    }

    boolean isActionAvailable() {
        if (selectedEntry == null)
            return false;
        return level == StarmapLevel.SYSTEM ? !selectedEntry.isMoon() : isWarpAvailable();
    }

    String previewTexture(PlanetEntry entry) {
        if (entry == null)
            return null;
        String focus = entry.getVisual().getFocusTextureId();
        String normal = entry.getVisual().getTextureId();
        return level == StarmapLevel.PLANET && focus != null ? focus : normal;
    }

    String nodeTexture(PlanetEntry entry) {
        // Overview satellites are deliberately dots; a full sprite at this
        // scale reads as noise and makes neighbouring moons appear merged.
        if (entry != null && entry.isMoon() && level == StarmapLevel.SYSTEM)
            return null;
        return previewTexture(entry);
    }

    StarmapNodeElement.NodePlacement nodePlacement(StarSystem system, PlanetEntry entry,
                                                   boolean centralStar, int width, int height) {
        return nodePlacement(system, entry, centralStar, width, height, orbitClock);
    }

    StarmapNodeElement.NodePlacement nodePlacement(StarSystem system, PlanetEntry entry,
                                                   boolean centralStar, int width, int height,
                                                   double phaseClock) {
        if (system == null)
            return new StarmapNodeElement.NodePlacement(0, 0, 0, false, false);
        if (entry == null) {
            if (level == StarmapLevel.GALAXY) {
                int[] point = galaxyPoint(system, 0, 0, width, height);
                return new StarmapNodeElement.NodePlacement(point[0], point[1],
                        system == selectedSystem ? 22 : 16, true, system == selectedSystem);
            }
            if (level == StarmapLevel.SYSTEM && system == selectedSystem)
                return new StarmapNodeElement.NodePlacement(width / 2, height / 2, 28, true, false);
            return new StarmapNodeElement.NodePlacement(0, 0, 0, false, false);
        }
        if (system != selectedSystem)
            return new StarmapNodeElement.NodePlacement(0, 0, 0, false, false);
        if (level == StarmapLevel.SYSTEM) {
            float[] point = systemPointF(entry, width, height, phaseClock);
            if (entry.isMoon()) {
                // Satellites remain visible as subordinate dots in the system
                // view, but are intentionally not selectable (see node input).
                return new StarmapNodeElement.NodePlacement(point[0], point[1], 6,
                        true, false);
            }
            return new StarmapNodeElement.NodePlacement(point[0], point[1], 16,
                    true, entry == selectedEntry);
        }
        if (level == StarmapLevel.PLANET) {
            if (entry == focusedPlanet)
                return new StarmapNodeElement.NodePlacement(width / 2, height / 2, 54,
                        true, entry == selectedEntry);
            if (entry.isMoon() && focusedPlanet != null
                    && entry.getParentEntryId().equals(focusedPlanet.getEntryId())) {
                int centerX = width / 2;
                int centerY = height / 2;
                int radius = moonDisplayRadius(system, entry, width, height, true);
            float[] point = orbitPointF(centerX, centerY, radius,
                        moonDisplayAngle(system, entry),
                        StarmapOrbitMotion.moonPhase(phaseClock, entry.getOrbitRadius()));
                return new StarmapNodeElement.NodePlacement(point[0], point[1], 14,
                        true, entry == selectedEntry);
            }
        }
        return new StarmapNodeElement.NodePlacement(0, 0, 0, false, false);
    }

    /**
     * Shared geometry of the floating info panel so drawing and hit testing
     * always agree. Returns {@code [panelX, panelY, panelWidth, panelHeight]}.
     *
     * @param forHitTest when true the button-bearing height (122) is used even
     *                   if a system is selected, matching the action rect the
     *                   drawing layer paints for a selected entry.
     */
    int[] infoPanelRect(int frameX, int frameY, int frameWidth, int frameHeight,
                        boolean forHitTest) {
        int panelWidth = Math.min(230, Math.max(170, frameWidth / 4));
        int panelX = Math.max(frameX + 14, frameX + frameWidth - panelWidth - 18);
        int panelY = frameY + 38;
        int panelHeight = selectedEntry == null && !forHitTest ? 82 : 122;
        return new int[] { panelX, panelY, panelWidth, panelHeight };
    }

    private void performAction() {
        if (selectedEntry == null)
            return;
        if (level == StarmapLevel.SYSTEM && !selectedEntry.isMoon()) {
            focusedPlanet = selectedEntry;
            level = StarmapLevel.PLANET;
            refreshComponents();
        } else if (level == StarmapLevel.PLANET && isWarpAvailable()) {
            ModNetwork.sendToServer(new StartWarpPacket(selectedEntry.getEntryId()));
        }
    }

    private boolean isWarpAvailable() {
        if (selectedEntry == null || !selectedEntry.isReachable()
                || ClientPlanetState.isWarping()
                || selectedEntry.getDestination() == ClientPlanetState.getCurrent())
            return false;
        int cost = ShipWarpManager.warpFuelCost(ClientPlanetState.getCurrentEntryId(),
                selectedEntry.getEntryId());
        return ClientPlanetState.getFuel() >= cost;
    }

    private int[] galaxyPoint(StarSystem system, int x, int y, int width, int height) {
        int[] base = StarmapGeometry.galaxyPosition(system);
        return new int[] { x + base[0] * width / BASE_WIDTH, y + base[1] * height / BASE_HEIGHT };
    }

    private static int[] orbitPoint(int centerX, int centerY, int radius, float angle, float phase) {
        float[] point = orbitPointF(centerX, centerY, radius, angle, phase);
        return new int[] { Math.round(point[0]), Math.round(point[1]) };
    }

    private static float[] orbitPointF(float centerX, float centerY, float radius,
                                       float angle, float phase) {
        double radians = Math.toRadians(angle) + phase;
        return new float[] { centerX + (float) Math.cos(radians) * radius,
                centerY + (float) Math.sin(radians) * radius };
    }

    private StarSystem nearestSystem(float localX, float localY, float width, float height) {
        StarSystem nearest = null;
        double distance = Double.MAX_VALUE;
        for (StarSystem system : StarSystems.all()) {
            int[] point = galaxyPoint(system, 0, 0, Math.round(width), Math.round(height));
            double current = Math.hypot(localX - point[0], localY - point[1]);
            if (current <= 20 && current < distance) {
                nearest = system;
                distance = current;
            }
        }
        return nearest;
    }

    private PlanetEntry nearestSystemEntry(float localX, float localY, float width, float height) {
        PlanetEntry nearest = null;
        double distance = Double.MAX_VALUE;
        for (PlanetEntry entry : selectedSystem.getEntries()) {
            if (entry.isMoon())
                continue;
            float[] point = systemPointF(entry, Math.round(width), Math.round(height), renderOrbitClock);
            double current = Math.hypot(localX - point[0], localY - point[1]);
            if (current <= 18 && current < distance) {
                nearest = entry;
                distance = current;
            }
        }
        return nearest;
    }

    private PlanetEntry nearestPlanetTarget(float localX, float localY, float width, float height) {
        int centerX = Math.round(width / 2.0F);
        int centerY = Math.round(height / 2.0F);
        if (Math.hypot(localX - centerX, localY - centerY) <= 30)
            return focusedPlanet;
        PlanetEntry nearest = null;
        double distance = Double.MAX_VALUE;
        for (PlanetEntry moon : selectedSystem.getEntries()) {
            if (!moon.isMoon() || !moon.getParentEntryId().equals(focusedPlanet.getEntryId()))
                continue;
            int radius = moonDisplayRadius(selectedSystem, moon, Math.round(width),
                    Math.round(height), true);
            float[] point = orbitPointF(centerX, centerY, radius,
                    moonDisplayAngle(selectedSystem, moon),
                    StarmapOrbitMotion.moonPhase(renderOrbitClock, moon.getOrbitRadius()));
            double current = Math.hypot(localX - point[0], localY - point[1]);
            if (current <= 14 && current < distance) {
                nearest = moon;
                distance = current;
            }
        }
        return nearest;
    }

    private int[] systemPoint(PlanetEntry entry, int width, int height) {
        float[] point = systemPointF(entry, width, height, renderOrbitClock);
        return new int[] { Math.round(point[0]), Math.round(point[1]) };
    }

    private float[] systemPointF(PlanetEntry entry, int width, int height, double phaseClock) {
        int centerX = width / 2;
        int centerY = height / 2;
        if (!entry.isMoon()) {
            int radius = Math.max(18, entry.getOrbitRadius() * Math.min(width, height)
                    / SYSTEM_ORBIT_SCALE);
            return orbitPointF(centerX, centerY, radius, entry.getOrbitAngle(),
                    StarmapOrbitMotion.phase(phaseClock, entry.getOrbitRadius()));
        }
        PlanetEntry parent = StarSystems.entryById(entry.getParentEntryId());
        if (parent == null)
            return new float[] { centerX, centerY };
        float[] parentPoint = systemPointF(parent, width, height, phaseClock);
        StarSystem system = systemForEntry(entry);
        int radius = moonDisplayRadius(system, entry, width, height, false);
        return orbitPointF(parentPoint[0], parentPoint[1], radius,
                moonDisplayAngle(system, entry),
                StarmapOrbitMotion.moonPhase(phaseClock, entry.getOrbitRadius()));
    }

    private StarSystem systemForEntry(PlanetEntry entry) {
        if (entry == null)
            return selectedSystem;
        String systemId = StarSystems.systemIdOfEntry(entry.getEntryId());
        StarSystem system = StarSystems.byId(systemId);
        return system == null ? selectedSystem : system;
    }

    /** Visual radius for a moon, including room for the parent node and a gap. */
    private int moonDisplayRadius(StarSystem system, PlanetEntry moon, int width,
                                  int height, boolean focusedPlanetView) {
        int minDimension = Math.max(1, Math.min(width, height));
        int ordinal = moonOrdinal(system, moon);
        int authored = focusedPlanetView
                ? moon.getOrbitRadius() * minDimension / 150
                : moon.getOrbitRadius() * minDimension / 320;
        int minimum = focusedPlanetView ? 44 : 18;
        int spacing = focusedPlanetView ? 16 : 12;
        return Math.max(minimum, authored) + ordinal * spacing;
    }

    private int moonOrdinal(StarSystem system, PlanetEntry moon) {
        if (system == null || moon == null)
            return 0;
        int ordinal = 0;
        for (PlanetEntry candidate : system.getEntries()) {
            if (candidate.isMoon()
                    && java.util.Objects.equals(candidate.getParentEntryId(), moon.getParentEntryId())) {
                if (candidate == moon)
                    return ordinal;
                ordinal++;
            }
        }
        return ordinal;
    }

    /** Spread authored angles into deterministic slots when a parent has many moons. */
    private float moonDisplayAngle(StarSystem system, PlanetEntry moon) {
        if (system == null || moon == null)
            return moon == null ? 0.0F : moon.getOrbitAngle();
        int count = 0;
        for (PlanetEntry candidate : system.getEntries()) {
            if (candidate.isMoon()
                    && java.util.Objects.equals(candidate.getParentEntryId(), moon.getParentEntryId()))
                count++;
        }
        if (count <= 1)
            return moon.getOrbitAngle();
        int ordinal = moonOrdinal(system, moon);
        // Keep a small trace of the authored angle while guaranteeing even
        // separation for dense systems.
        return ordinal * (360.0F / count) + moon.getOrbitAngle() * 0.2F;
    }

    private static void drawOrbit(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int steps = Math.max(48, radius * 2);
        for (int i = 0; i < steps; i++) {
            double radians = Math.PI * 2.0D * i / steps;
            int x = centerX + Math.round((float) Math.cos(radians) * radius);
            int y = centerY + Math.round((float) Math.sin(radians) * radius);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawDashedLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i < steps; i += 8) {
            float a = i / (float) Math.max(1, steps);
            float b = Math.min(1.0F, (i + 4) / (float) Math.max(1, steps));
            int ax = Math.round(x1 + (x2 - x1) * a);
            int ay = Math.round(y1 + (y2 - y1) * a);
            int bx = Math.round(x1 + (x2 - x1) * b);
            int by = Math.round(y1 + (y2 - y1) * b);
            graphics.fill(Math.min(ax, bx), Math.min(ay, by), Math.max(ax, bx) + 1,
                    Math.max(ay, by) + 1, color);
        }
    }
}
