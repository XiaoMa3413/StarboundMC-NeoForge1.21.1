package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.StartWarpPacket;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarmapGalaxyGraph;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Full-screen LDLib2 canvas for the new starmap terminal. */
public final class StarmapTerminalRoot extends UIElement {
    private static final int BASE_WIDTH = 250;
    private static final int BASE_HEIGHT = 220;
    /** Keeps the outer authored orbit inside the framed viewport. */
    private static final int SYSTEM_ORBIT_SCALE = 300;

    private StarmapLevel level = StarmapLevel.GALAXY;
    private StarSystem selectedSystem;
    private PlanetEntry selectedEntry;
    private PlanetEntry focusedPlanet;
    /** True when the central star is explicitly selected in the system view. */
    private boolean centralStarSelected;
    private final StarmapGalaxyGraph galaxyGraph;
    private final StarmapBodyTextureResolver bodyTextures;
    private final StarmapViewTransform viewTransform = new StarmapViewTransform();
    private final StarmapSceneElement sceneLayer;
    private final UIElement nodeLayer;
    private final StarmapSelectionOverlayElement selectionOverlay;
    private final StarmapTransitionOverlayElement transitionOverlay;
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
    private boolean viewDragStarted;
    private boolean viewDragMoved;
    private float viewDragStartMouseX;
    private float viewDragStartMouseY;
    private float viewDragStartOffsetX;
    private float viewDragStartOffsetY;

    public StarmapTerminalRoot() {
        this(StarmapGalaxyGraphResources.load(), StarmapBodyTextureResolver.clientResources());
    }

    StarmapTerminalRoot(StarmapGalaxyGraph galaxyGraph,
                        StarmapBodyTextureResolver bodyTextures) {
        this.galaxyGraph = java.util.Objects.requireNonNull(galaxyGraph, "galaxyGraph");
        this.bodyTextures = java.util.Objects.requireNonNull(bodyTextures, "bodyTextures");
        addClass("starmap-redraw-root");
        layout(layout -> layout.widthPercent(100).heightPercent(100));
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.CLICK, this::onClick);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);

        sceneLayer = new StarmapSceneElement(this);
        nodeLayer = new UIElement().addClass("starmap-node-layer")
                .layout(layout -> layout.widthPercent(100).heightPercent(100)
                        .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE))
                .setAllowHitTest(false);
        for (StarmapGalaxyGraph.Node graphNode : galaxyGraph.nodes()) {
            StarSystem system = graphNode.system();
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
        transitionOverlay = new StarmapTransitionOverlayElement();
        chrome = new StarmapChromeElement(this);
        infoPanel = new StarmapInfoPanelElement(this);
        addChildren(sceneLayer, nodeLayer, selectionOverlay, transitionOverlay, chrome, infoPanel);
        addEventListener(UIEvents.TICK, event -> {
            orbitClock += 1.0D;
            renderOrbitClock = orbitClock;
            refreshComponents();
        });
    }

    private void refreshComponents() {
        nodes.forEach(StarmapNodeElement::refresh);
        selectionOverlay.refresh();
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

        viewDragStarted = false;
        viewDragMoved = false;
        float localX = event.x - getPositionX();
        float localY = event.y - getPositionY();
        if (isInsideInfoPanel(localX, localY))
            return;
        // Celestial nodes stop propagation themselves. Reaching the root is
        // therefore the single definition of a background drag gesture.
        viewDragStarted = true;
        viewDragStartMouseX = event.x;
        viewDragStartMouseY = event.y;
        viewDragStartOffsetX = viewTransform.offsetX();
        viewDragStartOffsetY = viewTransform.offsetY();
        event.stopPropagation();
    }

    private void onClick(UIEvent event) {
        if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !viewDragStarted)
            return;
        if (!viewDragMoved) {
            clearSelection();
            refreshComponents();
        }
        viewDragStarted = false;
        viewDragMoved = false;
        event.stopPropagation();
    }

    private void onMouseWheel(UIEvent event) {
        float width = getSizeWidth();
        float height = getSizeHeight();
        float localX = event.x - getPositionX();
        float localY = event.y - getPositionY();
        if (width <= 1.0F || height <= 1.0F || isInsideInfoPanel(localX, localY))
            return;
        if (viewTransform.zoomAt(event.deltaY, localX, localY, width, height))
            refreshComponents();
        // Do not let the container screen reinterpret a wheel gesture that
        // was made over the map, including gestures at the zoom boundary.
        event.stopPropagation();
    }

    boolean dragView(float mouseX, float mouseY) {
        if (!viewDragStarted)
            return false;
        float deltaX = mouseX - viewDragStartMouseX;
        float deltaY = mouseY - viewDragStartMouseY;
        if (!viewDragMoved && Math.hypot(deltaX, deltaY) <= 3.0D)
            return true;
        viewDragMoved = true;
        float width = getSizeWidth();
        float height = getSizeHeight();
        if (viewTransform.setOffset(viewDragStartOffsetX + deltaX,
                viewDragStartOffsetY + deltaY, width, height))
            refreshComponents();
        return true;
    }

    void finishViewDrag() {
        viewDragStarted = false;
        viewDragMoved = false;
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
        StarmapNavigationState current = navigationState();
        StarmapNavigationState next = current.goBack();
        if (!next.equals(current)) {
            applyNavigationState(next);
            resetViewForLevel();
        }
        refreshComponents();
        if (!next.equals(current))
            playLevelTransition();
    }

    /** Prepare continuous visual positions before LDLib2 performs hit testing. */
    void prepareFrame(float partialTick) {
        renderOrbitClock = orbitClock + partialTick;
        int width = Math.max(1, Math.round(getSizeWidth()));
        int height = Math.max(1, Math.round(getSizeHeight()));
        if (width <= 1 || height <= 1)
            return;
        if (width != laidOutWidth || height != laidOutHeight) {
            laidOutWidth = width;
            laidOutHeight = height;
            viewTransform.constrain(width, height);
            refreshComponents();
        }
        nodes.forEach(node -> node.prepareRender(renderOrbitClock));
        infoPanel.prepareFrame(width, height);
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

    PlanetEntry getFocusedPlanet() {
        return focusedPlanet;
    }

    StarmapGalaxyGraph galaxyGraph() {
        return galaxyGraph;
    }

    StarmapViewTransform viewTransform() {
        return viewTransform;
    }

    double renderOrbitClock() {
        return renderOrbitClock;
    }

    float systemOrbitRadius(PlanetEntry entry, int width, int height) {
        return viewTransform.scaleLength(Math.max(18,
                entry.getOrbitRadius() * Math.min(width, height) / SYSTEM_ORBIT_SCALE));
    }

    boolean isCentralStarSelected() {
        return centralStarSelected;
    }

    Component levelLabel() {
        return Component.translatable("gui.starboundmc.starmap.redraw.level." + level.name().toLowerCase());
    }

    boolean canFocusView() {
        return isInfoPanelVisible();
    }

    boolean isViewReset() {
        return viewTransform.isReset();
    }

    void resetView() {
        if (viewTransform.reset()) {
            infoPanelSide = null;
            refreshComponents();
        }
    }

    void focusSelectedView() {
        int width = Math.max(1, Math.round(getSizeWidth()));
        int height = Math.max(1, Math.round(getSizeHeight()));
        SelectedVisual selected = selectedVisual(width, height, renderOrbitClock);
        if (selected == null)
            return;
        StarmapViewTransform.Point world = viewTransform.toWorld(
                selected.x, selected.y, width, height);
        if (viewTransform.focus(world.x(), world.y(), width, height)) {
            infoPanelSide = null;
            refreshComponents();
        }
    }

    private void resetViewForLevel() {
        viewTransform.reset();
        infoPanelSide = null;
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
        StarmapGalaxyGraph.Node graphNode = galaxyGraph.node(system);
        if (graphNode == null || !graphNode.available())
            return;
        applyNavigationState(navigationState().enterSystem(system));
        resetViewForLevel();
        refreshComponents();
        playLevelTransition();
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
                enterPlanet(entry);
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
        return actionAvailability().available();
    }

    boolean canEnterSelectedSystem() {
        return level == StarmapLevel.GALAXY && actionAvailability().available();
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
        StarmapActionAvailability.Result availability = actionAvailability();
        return switch (availability.reason()) {
            case SYSTEM_LOCKED -> Component.translatable(
                    "gui.starboundmc.starmap.system_locked");
            case SYSTEM_UNREACHABLE -> Component.translatable(
                    "gui.starboundmc.starmap.system_unreachable");
            case BODY_LOCKED -> Component.translatable("gui.starboundmc.starmap.locked");
            case WARP_IN_PROGRESS -> Component.translatable("gui.starboundmc.warping");
            case CURRENT_DESTINATION -> Component.translatable(
                    "gui.starboundmc.starmap.current");
            case INSUFFICIENT_FUEL -> Component.translatable(
                    "gui.starboundmc.starmap.action.insufficient_fuel_detail",
                    availability.requiredFuel(), availability.availableFuel());
            default -> null;
        };
    }

    ResourceLocation previewTexture(PlanetEntry entry) {
        if (entry == null)
            return null;
        return bodyTextures.resolve(entry.getVisual(), level == StarmapLevel.PLANET);
    }

    ResourceLocation nodeTexture(PlanetEntry entry) {
        // Overview satellites are deliberately dots; a full sprite at this
        // scale reads as noise and makes neighbouring moons appear merged.
        if (entry != null && entry.isMoon() && level == StarmapLevel.SYSTEM)
            return null;
        return previewTexture(entry);
    }

    IGuiTexture bodyTexture(PlanetEntry entry, float size, ResourceLocation resolved) {
        return bodyTextures.texture(entry, size, resolved);
    }

    IGuiTexture previewBodyTexture(PlanetEntry entry, float size) {
        return bodyTextures.texture(entry, size, previewTexture(entry));
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
                float[] point = galaxyPointF(system, 0, 0, width, height);
                return new StarmapNodeElement.NodePlacement(point[0], point[1],
                        viewTransform.scaleLength(system == selectedSystem ? 22 : 16),
                        true, system == selectedSystem);
            }
            if (level == StarmapLevel.SYSTEM && system == selectedSystem) {
                StarmapViewTransform.Point center = viewTransform.toScreen(
                        width / 2.0F, height / 2.0F, width, height);
                return new StarmapNodeElement.NodePlacement(center.x(), center.y(),
                        viewTransform.scaleLength(28), true, centralStarSelected);
            }
            return new StarmapNodeElement.NodePlacement(0, 0, 0, false, false);
        }
        if (system != selectedSystem)
            return new StarmapNodeElement.NodePlacement(0, 0, 0, false, false);
        if (level == StarmapLevel.SYSTEM) {
            float[] point = systemPointF(entry, width, height, phaseClock);
            if (entry.isMoon()) {
                // Satellites remain visible as subordinate dots in the system
                // view, but are intentionally not selectable (see node input).
                return new StarmapNodeElement.NodePlacement(point[0], point[1],
                        viewTransform.scaleLength(6), true, false);
            }
            return new StarmapNodeElement.NodePlacement(point[0], point[1],
                    viewTransform.scaleLength(16), true, entry == selectedEntry);
        }
        if (level == StarmapLevel.PLANET) {
            if (entry == focusedPlanet) {
                StarmapViewTransform.Point center = viewTransform.toScreen(
                        width / 2.0F, height / 2.0F, width, height);
                return new StarmapNodeElement.NodePlacement(center.x(), center.y(),
                        viewTransform.scaleLength(54), true, entry == selectedEntry);
            }
            if (entry.isMoon() && focusedPlanet != null
                    && entry.getParentEntryId().equals(focusedPlanet.getEntryId())) {
                StarmapViewTransform.Point center = viewTransform.toScreen(
                        width / 2.0F, height / 2.0F, width, height);
                float radius = viewTransform.scaleLength(
                        moonDisplayRadius(system, entry, width, height, true));
                float[] point = orbitPointF(center.x(), center.y(), radius,
                        moonDisplayAngle(system, entry),
                        StarmapOrbitMotion.moonPhase(phaseClock, entry.getOrbitRadius()));
                return new StarmapNodeElement.NodePlacement(point[0], point[1],
                        viewTransform.scaleLength(14), true, entry == selectedEntry);
            }
        }
        return new StarmapNodeElement.NodePlacement(0, 0, 0, false, false);
    }

    StarmapInfoPanelPlacement.Placement infoPanelPlacement(int frameWidth, int frameHeight) {
        int preferredWidth = Math.min(230, Math.max(156, frameWidth / 4));
        int panelWidth = Math.min(preferredWidth, Math.max(1, frameWidth - 32));
        int panelHeight;
        if (selectedEntry != null || (level == StarmapLevel.GALAXY && selectedSystem != null)) {
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

    SelectedVisual selectedVisual(int width, int height, double phaseClock) {
        if (level == StarmapLevel.GALAXY && selectedSystem != null) {
            float[] point = galaxyPointF(selectedSystem, 0, 0, width, height);
            return new SelectedVisual(point[0], point[1], viewTransform.scaleLength(22.0F),
                    "galaxy:" + selectedSystem.getSystemId());
        }
        if (level == StarmapLevel.SYSTEM && selectedSystem != null) {
            if (centralStarSelected) {
                StarmapViewTransform.Point center = viewTransform.toScreen(
                        width / 2.0F, height / 2.0F, width, height);
                return new SelectedVisual(center.x(), center.y(),
                        viewTransform.scaleLength(28.0F),
                        "system:" + selectedSystem.getSystemId() + ":star");
            }
            if (selectedEntry != null) {
                float[] point = systemPointF(selectedEntry, width, height, phaseClock);
                return new SelectedVisual(point[0], point[1],
                        viewTransform.scaleLength(16.0F),
                        "system:" + selectedEntry.getEntryId());
            }
            return null;
        }
        if (level != StarmapLevel.PLANET || selectedSystem == null || selectedEntry == null)
            return null;
        if (selectedEntry == focusedPlanet) {
            StarmapViewTransform.Point center = viewTransform.toScreen(
                    width / 2.0F, height / 2.0F, width, height);
            return new SelectedVisual(center.x(), center.y(),
                    viewTransform.scaleLength(54.0F),
                    "planet:" + selectedEntry.getEntryId());
        }
        if (!selectedEntry.isMoon() || focusedPlanet == null
                || !java.util.Objects.equals(selectedEntry.getParentEntryId(), focusedPlanet.getEntryId()))
            return null;
        StarmapViewTransform.Point center = viewTransform.toScreen(
                width / 2.0F, height / 2.0F, width, height);
        float radius = viewTransform.scaleLength(
                moonDisplayRadius(selectedSystem, selectedEntry, width, height, true));
        float[] point = orbitPointF(center.x(), center.y(), radius,
                moonDisplayAngle(selectedSystem, selectedEntry),
                StarmapOrbitMotion.moonPhase(phaseClock, selectedEntry.getOrbitRadius()));
        return new SelectedVisual(point[0], point[1], viewTransform.scaleLength(14.0F),
                "planet:" + selectedEntry.getEntryId());
    }

    String selectionTargetKey() {
        if (level == StarmapLevel.GALAXY && selectedSystem != null)
            return "galaxy:" + selectedSystem.getSystemId();
        if (level == StarmapLevel.SYSTEM && selectedSystem != null) {
            if (centralStarSelected)
                return "system:" + selectedSystem.getSystemId() + ":star";
            if (selectedEntry != null)
                return "system:" + selectedEntry.getEntryId();
            return null;
        }
        if (level == StarmapLevel.PLANET && selectedEntry != null)
            return "planet:" + selectedEntry.getEntryId();
        return null;
    }

    private void performAction() {
        if (canEnterSelectedSystem()) {
            enterSystem(selectedSystem);
            return;
        }
        if (selectedEntry == null)
            return;
        if (level == StarmapLevel.SYSTEM && !selectedEntry.isMoon()) {
            enterPlanet(selectedEntry);
        } else if (level == StarmapLevel.PLANET && isWarpAvailable()) {
            ModNetwork.sendToServer(new StartWarpPacket(selectedEntry.getEntryId()));
        }
    }

    /** Change page context without carrying an implicit selection into it. */
    private void enterPlanet(PlanetEntry planet) {
        StarmapNavigationState current = navigationState();
        StarmapNavigationState next = current.enterPlanet(planet);
        if (next.equals(current))
            return;
        applyNavigationState(next);
        resetViewForLevel();
        refreshComponents();
        playLevelTransition();
    }

    private void playLevelTransition() {
        transitionOverlay.play();
    }

    private StarmapNavigationState navigationState() {
        return new StarmapNavigationState(level, selectedSystem, selectedEntry,
                focusedPlanet, centralStarSelected);
    }

    private void applyNavigationState(StarmapNavigationState state) {
        level = state.level();
        selectedSystem = state.selectedSystem();
        selectedEntry = state.selectedEntry();
        focusedPlanet = state.focusedPlanet();
        centralStarSelected = state.centralStarSelected();
    }

    private boolean isWarpAvailable() {
        return level == StarmapLevel.PLANET && actionAvailability().available();
    }

    private StarmapActionAvailability.Result actionAvailability() {
        if (level == StarmapLevel.GALAXY) {
            StarmapGalaxyGraph.Node graphNode = galaxyGraph.node(selectedSystem);
            return StarmapActionAvailability.galaxy(
                    selectedSystem != null && selectedEntry == null,
                    graphNode != null && graphNode.unlocked(),
                    graphNode != null && graphNode.reachable());
        }
        if (level == StarmapLevel.SYSTEM) {
            return StarmapActionAvailability.system(selectedEntry != null,
                    selectedEntry != null && selectedEntry.isMoon());
        }
        if (selectedEntry == null)
            return StarmapActionAvailability.planet(false, false, false,
                    false, 0, 0);
        int fuel = ClientPlanetState.getFuel();
        int cost = ShipWarpManager.warpFuelCost(ClientPlanetState.getCurrentEntryId(),
                selectedEntry.getEntryId());
        return StarmapActionAvailability.planet(true, selectedEntry.isReachable(),
                ClientPlanetState.isWarping(),
                selectedEntry.getDestination() == ClientPlanetState.getCurrent(),
                fuel, cost);
    }

    float[] galaxyPointF(StarSystem system, float x, float y, float width, float height) {
        StarmapGalaxyGraph.Node node = galaxyGraph.node(system);
        var position = node == null ? system.getGalaxyMapPosition() : node.position();
        float worldX = position.pixelX(BASE_WIDTH) * width / (float) BASE_WIDTH;
        float worldY = position.pixelY(BASE_HEIGHT) * height / (float) BASE_HEIGHT;
        StarmapViewTransform.Point point = viewTransform.toScreen(
                worldX, worldY, width, height);
        return new float[] { x + point.x(), y + point.y() };
    }

    private static float[] orbitPointF(float centerX, float centerY, float radius,
                                       float angle, float phase) {
        double radians = Math.toRadians(angle) + phase;
        return new float[] { centerX + (float) Math.cos(radians) * radius,
                centerY + (float) Math.sin(radians) * radius };
    }

    float[] systemPointF(PlanetEntry entry, int width, int height, double phaseClock) {
        StarmapViewTransform.Point center = viewTransform.toScreen(
                width / 2.0F, height / 2.0F, width, height);
        if (!entry.isMoon()) {
            float radius = systemOrbitRadius(entry, width, height);
            return orbitPointF(center.x(), center.y(), radius, entry.getOrbitAngle(),
                    StarmapOrbitMotion.phase(phaseClock, entry.getOrbitRadius()));
        }
        PlanetEntry parent = StarSystems.entryById(entry.getParentEntryId());
        if (parent == null)
            return new float[] { center.x(), center.y() };
        float[] parentPoint = systemPointF(parent, width, height, phaseClock);
        StarSystem system = systemForEntry(entry);
        float radius = viewTransform.scaleLength(
                moonDisplayRadius(system, entry, width, height, false));
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
    int moonDisplayRadius(StarSystem system, PlanetEntry moon, int width,
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
    float moonDisplayAngle(StarSystem system, PlanetEntry moon) {
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

    record SelectedVisual(float x, float y, float diameter, String key) {
        float selectionRadius() {
            return diameter * 0.5F + Math.max(4.0F, diameter * 0.22F);
        }
    }

}
