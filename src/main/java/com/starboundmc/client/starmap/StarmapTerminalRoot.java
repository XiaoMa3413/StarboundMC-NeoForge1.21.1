package com.starboundmc.client.starmap;

import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
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
    /** True when the central star is explicitly selected in the system view. */
    private boolean centralStarSelected;
    private final UIElement nodeLayer;
    private final StarmapSelectionOverlayElement selectionOverlay;
    private final StarmapChromeElement chrome;
    private final StarmapInfoPanelElement infoPanel;
    private final List<StarmapNodeElement> nodes = new ArrayList<>();
    /** Accumulated simulation ticks. Rendering adds a partial tick below. */
    private double orbitClock;
    /** Continuous phase used by the current render pass and hit testing. */
    private double renderOrbitClock;
    private String infoPanelTargetKey;
    private StarmapInfoPanelPlacement.Side infoPanelSide;
    private int laidOutWidth;
    private int laidOutHeight;

    public StarmapTerminalRoot() {
        addClass("starmap-redraw-root");
        layout(layout -> layout.widthPercent(100).heightPercent(100));
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DOUBLE_CLICK, this::onDoubleClick);

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
        selectionOverlay = new StarmapSelectionOverlayElement(this);
        chrome = new StarmapChromeElement(this);
        infoPanel = new StarmapInfoPanelElement(this);
        addChildren(nodeLayer, selectionOverlay, chrome, infoPanel);
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
        boolean targetSelected = false;
        if (level == StarmapLevel.GALAXY) {
            StarSystem system = nearestSystem(localX, localY, width, height);
            if (system != null) {
                selectedSystem = system;
                selectedEntry = null;
                centralStarSelected = false;
                targetSelected = true;
            }
        } else if (level == StarmapLevel.SYSTEM && selectedSystem != null) {
            float centerX = width / 2.0F;
            float centerY = height / 2.0F;
            if (Math.hypot(localX - centerX, localY - centerY) <= 24.0D) {
                selectCentralStar(selectedSystem);
                targetSelected = true;
            } else {
                PlanetEntry entry = nearestSystemEntry(localX, localY, width, height);
                if (entry != null) {
                    if (entry == selectedEntry) {
                        focusedPlanet = entry;
                        level = StarmapLevel.PLANET;
                    } else {
                        selectedEntry = entry;
                    }
                    targetSelected = true;
                }
            }
        } else if (level == StarmapLevel.PLANET) {
            PlanetEntry target = nearestPlanetTarget(localX, localY, width, height);
            if (target != null) {
                selectedEntry = target;
                targetSelected = true;
            }
        }
        if (!targetSelected && !isInsideInfoPanel(localX, localY))
            clearSelection();
        refreshComponents();
    }

    private void onDoubleClick(UIEvent event) {
        if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT || level != StarmapLevel.GALAXY)
            return;
        float width = getSizeWidth();
        float height = getSizeHeight();
        float localX = event.x - getPositionX();
        float localY = event.y - getPositionY();
        StarSystem system = nearestSystem(localX, localY, width, height);
        if (system != null) {
            enterSystem(system);
            event.stopPropagation();
        }
    }

    private void clearSelection() {
        if (level == StarmapLevel.GALAXY)
            selectedSystem = null;
        selectedEntry = null;
        if (level == StarmapLevel.SYSTEM)
            centralStarSelected = false;
        if (level != StarmapLevel.PLANET)
            focusedPlanet = null;
    }

    private boolean isInsideInfoPanel(float localX, float localY) {
        return isInfoPanelVisible() && infoPanel.containsLocalPoint(localX, localY);
    }

    public void goBack() {
        if (level == StarmapLevel.PLANET) {
            level = StarmapLevel.SYSTEM;
            selectedEntry = focusedPlanet;
            focusedPlanet = null;
            centralStarSelected = false;
        } else if (level == StarmapLevel.SYSTEM) {
            level = StarmapLevel.GALAXY;
            selectedEntry = null;
            centralStarSelected = false;
        }
        refreshComponents();
    }

    /** Prepare continuous visual positions before LDLib2 performs hit testing. */
    void prepareFrame(float partialTick) {
        renderOrbitClock = orbitClock + partialTick;
        int width = Math.max(1, Math.round(getSizeWidth()));
        int height = Math.max(1, Math.round(getSizeHeight()));
        if (width <= 1 || height <= 1)
            return;
        nodes.forEach(node -> node.prepareRender(renderOrbitClock));
        infoPanel.prepareFrame(width, height);
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
        prepareFrame(context.partialTick);
        drawStars(graphics, x, y, width, height);
        if (level == StarmapLevel.GALAXY)
            drawGalaxy(graphics, x, y, width, height);
        else if (level == StarmapLevel.SYSTEM)
            drawSystem(graphics, x, y, width, height);
        else
            drawPlanet(graphics, x, y, width, height);
        // Vector route/orbit vertices use the shared GUI buffer. Flush them
        // before child nodes so the scene always stays behind the UI layer.
        graphics.flush();
        super.drawBackgroundAdditional(context);
    }

    /**
     * Render the selected-body marker from the same interpolated coordinates
     * used by the orbiting nodes. This deliberately lives outside the node
     * layout boxes, which are updated on the simulation tick.
     */
    void drawSelectionOverlay(GuiGraphics graphics) {
        int width = Math.max(1, Math.round(getSizeWidth()));
        int height = Math.max(1, Math.round(getSizeHeight()));
        SelectedVisual selected = selectedVisual(width, height, renderOrbitClock);
        if (selected == null)
            return;
        drawSelectionBrackets(graphics, getPositionX() + selected.x,
                getPositionY() + selected.y, selected.diameter);
        graphics.flush();
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
        float[] first = null;
        for (StarSystem system : StarSystems.all()) {
            float[] position = galaxyPointF(system, x, y, width, height);
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
                drawOrbit(graphics, x + point[0], y + point[1],
                        moonDisplayRadius(selectedSystem, moon, width, height, false),
                        0x453D7182, true);
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

    boolean isCentralStarSelected() {
        return centralStarSelected;
    }

    Component levelLabel() {
        return Component.translatable("gui.starboundmc.starmap.redraw.level." + level.name().toLowerCase());
    }

    void selectSystem(StarSystem system) {
        if (system == null)
            return;
        selectedSystem = system;
        selectedEntry = null;
        centralStarSelected = false;
        refreshComponents();
    }

    void enterSystem(StarSystem system) {
        if (system == null)
            return;
        selectedSystem = system;
        level = StarmapLevel.SYSTEM;
        selectedEntry = null;
        focusedPlanet = null;
        centralStarSelected = false;
        refreshComponents();
    }

    void selectCentralStar(StarSystem system) {
        if (system == null || level != StarmapLevel.SYSTEM || system != selectedSystem)
            return;
        selectedEntry = null;
        focusedPlanet = null;
        centralStarSelected = true;
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
            centralStarSelected = false;
            if (entry == selectedEntry) {
                focusedPlanet = entry;
                level = StarmapLevel.PLANET;
            } else {
                selectedEntry = entry;
            }
        } else if (level == StarmapLevel.PLANET) {
            selectedEntry = entry;
            centralStarSelected = false;
        }
        refreshComponents();
    }

    void performActionFromUi() {
        performAction();
    }

    boolean isActionAvailable() {
        if (level == StarmapLevel.GALAXY)
            return canEnterSelectedSystem();
        if (selectedEntry == null)
            return false;
        return level == StarmapLevel.SYSTEM ? !selectedEntry.isMoon() : isWarpAvailable();
    }

    boolean canEnterSelectedSystem() {
        return level == StarmapLevel.GALAXY && selectedSystem != null && selectedEntry == null;
    }

    boolean isInfoPanelVisible() {
        if (level == StarmapLevel.GALAXY)
            return selectedSystem != null;
        if (level == StarmapLevel.SYSTEM)
            return centralStarSelected || selectedEntry != null;
        return selectedEntry != null;
    }

    Component actionLabel() {
        if (level == StarmapLevel.GALAXY)
            return Component.translatable("gui.starboundmc.starmap.enter");
        if (level == StarmapLevel.SYSTEM)
            return Component.translatable("gui.starboundmc.starmap.redraw.inspect");
        return Component.translatable("gui.starboundmc.starmap.warp");
    }

    /**
     * Human-readable reason for a disabled action. A null result means the
     * selected destination is currently valid.
     */
    Component actionStatus() {
        if (level != StarmapLevel.PLANET || selectedEntry == null)
            return null;
        if (!selectedEntry.isReachable())
            return Component.translatable("gui.starboundmc.starmap.locked");
        if (ClientPlanetState.isWarping())
            return Component.translatable("gui.starboundmc.warping");
        if (selectedEntry.getDestination() == ClientPlanetState.getCurrent())
            return Component.translatable("gui.starboundmc.starmap.current");
        int cost = ShipWarpManager.warpFuelCost(ClientPlanetState.getCurrentEntryId(),
                selectedEntry.getEntryId());
        if (ClientPlanetState.getFuel() < cost)
            return Component.translatable("gui.starboundmc.starmap.action.insufficient_fuel_detail",
                    cost, ClientPlanetState.getFuel());
        return null;
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
                return new StarmapNodeElement.NodePlacement(width / 2, height / 2, 28, true,
                        centralStarSelected);
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

    StarmapInfoPanelPlacement.Placement infoPanelPlacement(int frameWidth, int frameHeight) {
        int preferredWidth = Math.min(230, Math.max(156, frameWidth / 4));
        int panelWidth = Math.min(preferredWidth, Math.max(1, frameWidth - 32));
        int panelHeight;
        if (selectedEntry != null || canEnterSelectedSystem()) {
            panelHeight = 122;
        } else if (centralStarSelected) {
            // Header (34) + metadata (11) + description (28) + padding (16)
            // needs more than the compact 82px card used for a star-system
            // summary with no selected central star.
            panelHeight = 96;
        } else {
            panelHeight = 82;
        }

        SelectedVisual selected = selectedVisual(frameWidth, frameHeight, renderOrbitClock);
        if (selected == null) {
            return new StarmapInfoPanelPlacement.Placement(14.0F, 38.0F,
                    panelWidth, panelHeight, StarmapInfoPanelPlacement.Side.RIGHT);
        }
        if (!selected.key.equals(infoPanelTargetKey)) {
            infoPanelTargetKey = selected.key;
            infoPanelSide = null;
        }
        StarmapInfoPanelPlacement.Placement placement = StarmapInfoPanelPlacement.place(
                0.0F, 0.0F, frameWidth, frameHeight, panelWidth, panelHeight,
                selected.x, selected.y, selected.selectionRadius(), infoPanelSide);
        infoPanelSide = placement.side();
        return placement;
    }

    private SelectedVisual selectedVisual(int width, int height, double phaseClock) {
        if (level == StarmapLevel.GALAXY && selectedSystem != null) {
            float[] point = galaxyPointF(selectedSystem, 0, 0, width, height);
            return new SelectedVisual(point[0], point[1], 22.0F,
                    "galaxy:" + selectedSystem.getSystemId());
        }
        if (level == StarmapLevel.SYSTEM && selectedSystem != null) {
            if (centralStarSelected) {
                return new SelectedVisual(width / 2.0F, height / 2.0F, 28.0F,
                        "system:" + selectedSystem.getSystemId() + ":star");
            }
            if (selectedEntry != null) {
                float[] point = systemPointF(selectedEntry, width, height, phaseClock);
                return new SelectedVisual(point[0], point[1], 16.0F,
                        "system:" + selectedEntry.getEntryId());
            }
            return null;
        }
        if (level != StarmapLevel.PLANET || selectedSystem == null || selectedEntry == null)
            return null;
        if (selectedEntry == focusedPlanet) {
            return new SelectedVisual(width / 2.0F, height / 2.0F, 54.0F,
                    "planet:" + selectedEntry.getEntryId());
        }
        if (!selectedEntry.isMoon() || focusedPlanet == null
                || !java.util.Objects.equals(selectedEntry.getParentEntryId(), focusedPlanet.getEntryId()))
            return null;
        int radius = moonDisplayRadius(selectedSystem, selectedEntry, width, height, true);
        float[] point = orbitPointF(width / 2.0F, height / 2.0F, radius,
                moonDisplayAngle(selectedSystem, selectedEntry),
                StarmapOrbitMotion.moonPhase(phaseClock, selectedEntry.getOrbitRadius()));
        return new SelectedVisual(point[0], point[1], 14.0F,
                "planet:" + selectedEntry.getEntryId());
    }

    private void performAction() {
        if (canEnterSelectedSystem()) {
            enterSystem(selectedSystem);
            return;
        }
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
        float[] point = galaxyPointF(system, x, y, width, height);
        return new int[] { Math.round(point[0]), Math.round(point[1]) };
    }

    private float[] galaxyPointF(StarSystem system, float x, float y, float width, float height) {
        int[] base = StarmapGeometry.galaxyPosition(system);
        return new float[] { x + base[0] * width / (float) BASE_WIDTH,
                y + base[1] * height / (float) BASE_HEIGHT };
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
            float[] point = systemPointF(entry, Math.round(width), Math.round(height), renderOrbitClock);
            double current = Math.hypot(localX - point[0], localY - point[1]);
            double hitRadius = entry.isMoon() ? 14.0D : 18.0D;
            if (current <= hitRadius && current < distance) {
                nearest = entry.isMoon()
                        ? StarSystems.entryById(entry.getParentEntryId()) : entry;
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

    private static void drawOrbit(GuiGraphics graphics, float centerX, float centerY,
                                   float radius, int color) {
        drawOrbit(graphics, centerX, centerY, radius, color, false);
    }

    /** Draw a continuous vector-like orbit rather than a ring of square pixels. */
    private static void drawOrbit(GuiGraphics graphics, float centerX, float centerY,
                                  float radius, int color, boolean dashed) {
        int steps = Math.max(128, (int) Math.ceil(radius * Math.PI * 2.4D));
        float circumference = Math.max(1.0F, (float) (Math.PI * 2.0D * radius));
        int dashSpan = Math.max(2, Math.round(steps * 7.0F / circumference));
        int gapSpan = Math.max(2, Math.round(steps * 11.0F / circumference));
        int dashPeriod = dashSpan + gapSpan;
        int glow = multiplyAlpha(color, dashed ? 0.22F : 0.30F);
        int core = multiplyAlpha(color, dashed ? 0.82F : 0.94F);
        for (int pass = 0; pass < 2; pass++) {
            boolean draw = pass == 0;
            int passColor = draw ? glow : core;
            float width = draw ? (dashed ? 2.8F : 3.4F) : (dashed ? 0.85F : 1.05F);
            for (int i = 0; i < steps; i++) {
                if (dashed && (i % dashPeriod) >= dashSpan)
                    continue;
                double a = Math.PI * 2.0D * i / steps;
                double b = Math.PI * 2.0D * (i + 1) / steps;
                drawSmoothSegment(graphics,
                        centerX + (float) Math.cos(a) * radius,
                        centerY + (float) Math.sin(a) * radius,
                        centerX + (float) Math.cos(b) * radius,
                        centerY + (float) Math.sin(b) * radius,
                        width, passColor);
            }
        }
    }

    /**
     * Kept under the original name for the bootstrap contract. The route is
     * now made from sub-pixel quads with a soft halo and clean dash spacing.
     */
    private static void drawDashedLine(GuiGraphics graphics, int x1, int y1,
                                       int x2, int y2, int color) {
        drawDashedLine(graphics, (float) x1, (float) y1, (float) x2, (float) y2, color);
    }

    private static void drawDashedLine(GuiGraphics graphics, float x1, float y1,
                                       float x2, float y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.01F)
            return;
        float dash = 6.0F;
        float gap = 8.0F;
        int glow = multiplyAlpha(color, 0.28F);
        int core = multiplyAlpha(color, 0.92F);
        for (int pass = 0; pass < 2; pass++) {
            int passColor = pass == 0 ? glow : core;
            float width = pass == 0 ? 3.2F : 1.05F;
            for (float offset = 0.0F; offset < length; offset += dash + gap) {
                float start = offset / length;
                float end = Math.min(length, offset + dash) / length;
                drawSmoothSegment(graphics,
                        x1 + dx * start, y1 + dy * start,
                        x1 + dx * end, y1 + dy * end,
                        width, passColor);
            }
        }
    }

    private void drawSelectionBrackets(GuiGraphics graphics, float centerX, float centerY,
                                       float bodySize) {
        float pad = Math.max(4.0F, bodySize * 0.22F);
        float half = bodySize * 0.5F + pad;
        float left = centerX - half;
        float top = centerY - half;
        float right = centerX + half;
        float bottom = centerY + half;
        float corner = Math.max(5.0F, bodySize * 0.30F);
        float thickness = Math.max(1.0F, Math.min(1.65F, bodySize * 0.08F));
        int core = 0xFF63E2DF;
        int glow = 0x4A63E2DF;
        drawCorner(graphics, left, top, corner, thickness, true, true, glow, core);
        drawCorner(graphics, right, top, corner, thickness, false, true, glow, core);
        drawCorner(graphics, left, bottom, corner, thickness, true, false, glow, core);
        drawCorner(graphics, right, bottom, corner, thickness, false, false, glow, core);
    }

    private static void drawCorner(GuiGraphics graphics, float x, float y, float length,
                                   float thickness, boolean left, boolean top,
                                   int glow, int core) {
        float horizontal = left ? x + length : x - length;
        float vertical = top ? y + length : y - length;
        drawSmoothSegment(graphics, x, y, horizontal, y, thickness * 2.8F, glow);
        drawSmoothSegment(graphics, x, y, x, vertical, thickness * 2.8F, glow);
        drawSmoothSegment(graphics, x, y, horizontal, y, thickness, core);
        drawSmoothSegment(graphics, x, y, x, vertical, thickness, core);
    }

    private static void drawSmoothSegment(GuiGraphics graphics, float x1, float y1,
                                           float x2, float y2, float width, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001F)
            return;
        float half = width * 0.5F;
        float nx = -dy / length * half;
        float ny = dx / length * half;
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(matrix, x1 + nx, y1 + ny, 0).setColor(color);
        buffer.addVertex(matrix, x2 + nx, y2 + ny, 0).setColor(color);
        buffer.addVertex(matrix, x2 - nx, y2 - ny, 0).setColor(color);
        buffer.addVertex(matrix, x1 - nx, y1 - ny, 0).setColor(color);
    }

    private static int multiplyAlpha(int color, float factor) {
        int alpha = (color >>> 24) & 0xFF;
        int scaled = Math.max(0, Math.min(255, Math.round(alpha * factor)));
        return (scaled << 24) | (color & 0x00FFFFFF);
    }

    private record SelectedVisual(float x, float y, float diameter, String key) {
        float selectionRadius() {
            return diameter * 0.5F + Math.max(4.0F, diameter * 0.22F);
        }
    }
}
