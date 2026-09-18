package com.starboundmc.client.starmap;

import com.starboundmc.client.StarmapUniverse;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.client.ClientShipEnvironmentState;
import com.starboundmc.client.ui.ShipSystemLockOverlay;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.StartWarpPacket;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.starmap.StarmapGalaxyGraph;
import com.starboundmc.world.universe.StarSystemDefinition;
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
    private StarSystemDefinition selectedSystem;
    private CelestialBodyDefinition selectedEntry;
    private CelestialBodyDefinition focusedPlanet;
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
    private final ShipSystemLockOverlay environmentLock;
    private final RelaySignalPanel relaySignal;
    private boolean relaySelected;
    private final int containerId;
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
        this(-1);
    }

    public StarmapTerminalRoot(int containerId) {
        this(containerId, StarmapGalaxyGraphResources.load(),
                StarmapBodyTextureResolver.clientResources());
    }

    StarmapTerminalRoot(StarmapGalaxyGraph galaxyGraph,
                        StarmapBodyTextureResolver bodyTextures) {
        this(-1, galaxyGraph, bodyTextures);
    }

    StarmapTerminalRoot(int containerId, StarmapGalaxyGraph galaxyGraph,
                        StarmapBodyTextureResolver bodyTextures) {
        this.containerId = containerId;
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
            StarSystemDefinition system = graphNode.system();
            StarmapNodeElement.StarSystemRef ref = new StarmapNodeElement.StarSystemRef(system);
            StarmapNodeElement starNode = new StarmapNodeElement(this, ref, null, false);
            nodes.add(starNode);
            nodeLayer.addChild(starNode);
            for (CelestialBodyDefinition entry : system.bodies()) {
                StarmapNodeElement bodyNode = new StarmapNodeElement(this, ref, entry, false);
                nodes.add(bodyNode);
                nodeLayer.addChild(bodyNode);
            }
        }
        selectionOverlay = new StarmapSelectionOverlayElement(this);
        transitionOverlay = new StarmapTransitionOverlayElement();
        chrome = new StarmapChromeElement(this);
        infoPanel = new StarmapInfoPanelElement(this);
        environmentLock = new ShipSystemLockOverlay();
        nodeLayer.addChild(new RelayMapNode(this));
        relaySignal = new RelaySignalPanel(this);
        addChildren(sceneLayer, nodeLayer, selectionOverlay, transitionOverlay, chrome, infoPanel,
                relaySignal, environmentLock);
        refreshEnvironmentLock();
        addEventListener(UIEvents.TICK, event -> {
            orbitClock += 1.0D;
            renderOrbitClock = orbitClock;
            refreshComponents();
        });
    }

    private void refreshComponents() {
        if (relaySelected && !relayVisible()) relaySelected = false;
        relaySignal.refresh();
        refreshEnvironmentLock();
        nodes.forEach(StarmapNodeElement::refresh);
        selectionOverlay.refresh();
        chrome.refresh();
        infoPanel.refresh();
    }

    private void onMouseDown(UIEvent event) {
        if (isEnvironmentLocked()) {
            event.stopPropagation();
            return;
        }
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
        if (isEnvironmentLocked()) {
            event.stopPropagation();
            return;
        }
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
        if (isEnvironmentLocked()) {
            event.stopPropagation();
            return;
        }
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
        if (isEnvironmentLocked() || !viewDragStarted)
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
        relaySelected = false;
        if (level == StarmapLevel.GALAXY)
            selectedSystem = null;
        selectedEntry = null;
        if (level == StarmapLevel.SYSTEM)
            centralStarSelected = false;
        if (level != StarmapLevel.PLANET)
            focusedPlanet = null;
    }

    private boolean isEnvironmentLocked() {
        return ClientShipEnvironmentState.isLocked(containerId);
    }

    /** The ship's current system is the only deep-space signal available before hyperdrive repair. */
    private String currentSystemId() {
        String entryId = ClientPlanetState.getCurrentEntryId();
        String systemId = StarmapUniverse.systemIdOfEntry(entryId);
        if (systemId != null)
            return systemId;
        StarSystemDefinition current = StarmapUniverse.systemOf(ClientPlanetState.getCurrentEntryId());
        return current == null ? null : current.systemId();
    }

    boolean isSublightOnline() {
        return ClientShipEnvironmentState.canTravelWithinSystem(containerId);
    }

    boolean isHyperdriveOnline() {
        return ClientShipEnvironmentState.canTravelBetweenSystems(containerId);
    }

    /**
     * Before the hyperdrive is repaired, render the current system and an
     * incomplete signal for every other system, without exposing selectable
     * bodies or details from those systems.
     */
    boolean isSystemRevealed(StarSystemDefinition system) {
        if (system == null || isEnvironmentLocked())
            return false;
        return isHyperdriveOnline() || java.util.Objects.equals(system.systemId(), currentSystemId());
    }

    boolean isSystemSelectable(StarSystemDefinition system) {
        return isSystemRevealed(system);
    }

    boolean isRouteRevealed(StarmapGalaxyGraph.Route route) {
        return route != null && isHyperdriveOnline();
    }

    private void refreshEnvironmentLock() {
        environmentLock.setLocked(isEnvironmentLocked());
    }

    private boolean isInsideInfoPanel(float localX, float localY) {
        return isInfoPanelVisible() && infoPanel.containsLocalPoint(localX, localY);
    }

    public void goBack() {
        if (isEnvironmentLocked())
            return;
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
        refreshEnvironmentLock();
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
        chrome.prepareFrame(renderOrbitClock);
        infoPanel.prepareFrame(width, height);
    }

    StarmapLevel getLevel() {
        return level;
    }

    int containerId() {
        return containerId;
    }

    StarSystemDefinition getSelectedSystem() {
        return selectedSystem;
    }

    CelestialBodyDefinition getSelectedEntry() {
        return selectedEntry;
    }

    CelestialBodyDefinition getFocusedPlanet() {
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

    float systemOrbitRadius(CelestialBodyDefinition entry, int width, int height) {
        return viewTransform.scaleLength(Math.max(18,
                entry.orbit().orbitRadius() * Math.min(width, height) / SYSTEM_ORBIT_SCALE));
    }

    boolean isCentralStarSelected() {
        return centralStarSelected;
    }

    Component levelLabel() {
        return Component.translatable("gui.starboundmc.starmap.redraw.level." + level.name().toLowerCase());
    }

    boolean canFocusView() {
        return !isEnvironmentLocked() && isInfoPanelVisible();
    }

    boolean isViewReset() {
        return viewTransform.isReset();
    }

    void resetView() {
        if (isEnvironmentLocked())
            return;
        if (viewTransform.reset()) {
            infoPanelSide = null;
            refreshComponents();
        }
    }

    void focusSelectedView() {
        if (isEnvironmentLocked())
            return;
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

    void selectSystem(StarSystemDefinition system) {
        if (isEnvironmentLocked() || !isSystemSelectable(system))
            return;
        selectedSystem = system;
        relaySelected = false;
        selectedEntry = null;
        centralStarSelected = false;
        refreshComponents();
    }

    void enterSystem(StarSystemDefinition system) {
        if (isEnvironmentLocked() || !isSystemSelectable(system))
            return;
        StarmapGalaxyGraph.Node graphNode = galaxyGraph.node(system);
        if (graphNode == null || !graphNode.available())
            return;
        applyNavigationState(navigationState().enterSystem(system));
        resetViewForLevel();
        refreshComponents();
        playLevelTransition();
    }

    void selectCentralStar(StarSystemDefinition system) {
        if (isEnvironmentLocked() || system == null || level != StarmapLevel.SYSTEM
                || system != selectedSystem)
            return;
        selectedEntry = null;
        relaySelected = false;
        focusedPlanet = null;
        centralStarSelected = true;
        refreshComponents();
    }

    void selectEntry(CelestialBodyDefinition entry) {
        if (isEnvironmentLocked() || entry == null)
            return;
        relaySelected = false;
        if (level == StarmapLevel.SYSTEM) {
            if (entry.orbit().isMoon())
                entry = StarmapUniverse.body(entry.parentEntryId().orElse(null));
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
        if (isEnvironmentLocked())
            return;
        performAction();
    }

    boolean isActionAvailable() {
        if (relaySelected) return !isEnvironmentLocked() && relayActionAvailable();
        return !isEnvironmentLocked() && actionAvailability().available();
    }

    boolean canEnterSelectedSystem() {
        return !relaySelected && !isEnvironmentLocked()
                && level == StarmapLevel.GALAXY && actionAvailability().available();
    }

    boolean isInfoPanelVisible() {
        if (relaySelected) return relayVisible();
        if (level == StarmapLevel.GALAXY)
            return selectedSystem != null;
        if (level == StarmapLevel.SYSTEM)
            return centralStarSelected || selectedEntry != null;
        return selectedEntry != null;
    }

    Component actionLabel() {
        if (relaySelected) return Component.translatable(relayActive()
                ? "gui.starboundmc.relay.leave" : "gui.starboundmc.relay.approach");
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
        if (relaySelected) return relayStatus();
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
            case SUBLIGHT_OFFLINE -> Component.translatable(
                    "gui.starboundmc.starmap.sublight_offline");
            case HYPERDRIVE_OFFLINE -> Component.translatable(
                    "gui.starboundmc.starmap.hyperdrive_offline");
            case INSUFFICIENT_FUEL -> Component.translatable(
                    "gui.starboundmc.starmap.action.insufficient_fuel_detail",
                    availability.requiredFuel(), availability.availableFuel());
            default -> null;
        };
    }

    ResourceLocation previewTexture(CelestialBodyDefinition entry) {
        if (entry == null)
            return null;
        return bodyTextures.resolve(entry.starmapVisual(), level == StarmapLevel.PLANET);
    }

    ResourceLocation nodeTexture(CelestialBodyDefinition entry) {
        // Overview satellites are deliberately dots; a full sprite at this
        // scale reads as noise and makes neighbouring moons appear merged.
        if (entry != null && entry.orbit().isMoon() && level == StarmapLevel.SYSTEM)
            return null;
        return previewTexture(entry);
    }

    IGuiTexture bodyTexture(CelestialBodyDefinition entry, float size, ResourceLocation resolved) {
        return bodyTextures.texture(entry, size, resolved);
    }

    IGuiTexture previewBodyTexture(CelestialBodyDefinition entry, float size) {
        return bodyTextures.texture(entry, size, previewTexture(entry));
    }

    StarmapNodeElement.NodePlacement nodePlacement(StarSystemDefinition system, CelestialBodyDefinition entry,
                                                   boolean centralStar, int width, int height) {
        return nodePlacement(system, entry, centralStar, width, height, orbitClock);
    }

    StarmapNodeElement.NodePlacement nodePlacement(StarSystemDefinition system, CelestialBodyDefinition entry,
                                                   boolean centralStar, int width, int height,
                                                   double phaseClock) {
        if (system == null)
            return new StarmapNodeElement.NodePlacement(0, 0, 0, false, false);
        if (entry == null) {
            if (level == StarmapLevel.GALAXY) {
                float[] point = galaxyPointF(system, 0, 0, width, height);
                boolean revealed = isSystemRevealed(system);
                return new StarmapNodeElement.NodePlacement(point[0], point[1],
                        viewTransform.scaleLength(system == selectedSystem ? 22
                                : revealed ? 16 : 12),
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
            if (entry.orbit().isMoon()) {
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
            if (entry.orbit().isMoon() && focusedPlanet != null
                    && entry.parentEntryId().orElse(null).equals(focusedPlanet.entryId())) {
                StarmapViewTransform.Point center = viewTransform.toScreen(
                        width / 2.0F, height / 2.0F, width, height);
                float radius = viewTransform.scaleLength(
                        moonDisplayRadius(system, entry, width, height, true));
                float[] point = orbitPointF(center.x(), center.y(), radius,
                        moonDisplayAngle(system, entry),
                        StarmapOrbitMotion.moonPhase(phaseClock, entry.orbit().orbitRadius()));
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
        if (relaySelected) {
            panelHeight = 142;
        } else if (selectedEntry != null) {
            int moonCount = selectedSystem == null ? 0
                    : selectedSystem.moonCount(selectedEntry.entryId());
            panelHeight = 122 + (moonCount > 0 ? 11 : 0);
            if (selectedEntry.surface().map(s -> s.environment().coldTier() > 0 || s.environment().heatTier() > 0).orElse(false))
                panelHeight += 28;
        } else if (level == StarmapLevel.GALAXY && selectedSystem != null) {
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
        if (relaySelected) return relayVisual(width, height, phaseClock);
        if (level == StarmapLevel.GALAXY && selectedSystem != null) {
            float[] point = galaxyPointF(selectedSystem, 0, 0, width, height);
            return new SelectedVisual(point[0], point[1], viewTransform.scaleLength(22.0F),
                    "galaxy:" + selectedSystem.systemId());
        }
        if (level == StarmapLevel.SYSTEM && selectedSystem != null) {
            if (centralStarSelected) {
                StarmapViewTransform.Point center = viewTransform.toScreen(
                        width / 2.0F, height / 2.0F, width, height);
                return new SelectedVisual(center.x(), center.y(),
                        viewTransform.scaleLength(28.0F),
                        "system:" + selectedSystem.systemId() + ":star");
            }
            if (selectedEntry != null) {
                float[] point = systemPointF(selectedEntry, width, height, phaseClock);
                return new SelectedVisual(point[0], point[1],
                        viewTransform.scaleLength(16.0F),
                        "system:" + selectedEntry.entryId());
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
                    "planet:" + selectedEntry.entryId());
        }
        if (!selectedEntry.orbit().isMoon() || focusedPlanet == null
                || !java.util.Objects.equals(selectedEntry.parentEntryId().orElse(null), focusedPlanet.entryId()))
            return null;
        StarmapViewTransform.Point center = viewTransform.toScreen(
                width / 2.0F, height / 2.0F, width, height);
        float radius = viewTransform.scaleLength(
                moonDisplayRadius(selectedSystem, selectedEntry, width, height, true));
        float[] point = orbitPointF(center.x(), center.y(), radius,
                moonDisplayAngle(selectedSystem, selectedEntry),
                StarmapOrbitMotion.moonPhase(phaseClock, selectedEntry.orbit().orbitRadius()));
        return new SelectedVisual(point[0], point[1], viewTransform.scaleLength(14.0F),
                "planet:" + selectedEntry.entryId());
    }

    String selectionTargetKey() {
        if (relaySelected) return "poi:abandoned_relay";
        if (level == StarmapLevel.GALAXY && selectedSystem != null)
            return "galaxy:" + selectedSystem.systemId();
        if (level == StarmapLevel.SYSTEM && selectedSystem != null) {
            if (centralStarSelected)
                return "system:" + selectedSystem.systemId() + ":star";
            if (selectedEntry != null)
                return "system:" + selectedEntry.entryId();
            return null;
        }
        if (level == StarmapLevel.PLANET && selectedEntry != null)
            return "planet:" + selectedEntry.entryId();
        return null;
    }

    private void performAction() {
        if (relaySelected) {
            if (isActionAvailable()) ModNetwork.sendToServer(new com.starboundmc.network.RelayActionPacket(containerId, relayActive()));
            return;
        }
        if (canEnterSelectedSystem()) {
            enterSystem(selectedSystem);
            return;
        }
        if (selectedEntry == null)
            return;
        if (level == StarmapLevel.SYSTEM && !selectedEntry.orbit().isMoon()) {
            enterPlanet(selectedEntry);
        } else if (level == StarmapLevel.PLANET && isWarpAvailable()) {
            ModNetwork.sendToServer(new StartWarpPacket(selectedEntry.entryId()));
        }
    }

    /** Change page context without carrying an implicit selection into it. */
    private void enterPlanet(CelestialBodyDefinition planet) {
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
        relaySelected = false;
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
                    selectedEntry != null && selectedEntry.orbit().isMoon());
        }
        if (selectedEntry == null)
            return StarmapActionAvailability.planet(false, false, false,
                    false, 0, 0);
        int fuel = ClientPlanetState.getFuel();
        int cost = ShipWarpManager.warpFuelCost(ClientPlanetState.getCurrentEntryId(),
                selectedEntry.entryId());
        String currentSystem = currentSystemId();
        String targetSystem = StarmapUniverse.systemIdOfEntry(selectedEntry.entryId());
        boolean sameSystem = currentSystem != null && currentSystem.equals(targetSystem);
        return StarmapActionAvailability.planet(true, selectedEntry.isNavigable(),
                ClientPlanetState.isWarping(),
                StarmapUniverse.isCurrent(selectedEntry.entryId()),
                sameSystem, isSublightOnline(), isHyperdriveOnline(),
                fuel, cost);
    }

    boolean isRelaySelected() { return relaySelected; }

    private CelestialBodyDefinition relayHome() {
        var state = com.starboundmc.client.space.RelayClientState.snapshot;
        return state == null ? null : StarmapUniverse.body(state.homeBody());
    }

    boolean relayVisible() {
        var state = com.starboundmc.client.space.RelayClientState.snapshot;
        var home = relayHome();
        if (state == null || home == null || isEnvironmentLocked()
                || state.phase() == com.starboundmc.encounter.RelayData.Phase.UNDISCOVERED.ordinal()) return false;
        var system = StarmapUniverse.systemOf(home.entryId());
        if (!isSystemRevealed(system)) return false;
        return RelayMapPresentation.visibleAt(level, system.systemId(),
                selectedSystem == null ? null : selectedSystem.systemId(), home.entryId(),
                home.parentEntryId().orElse(null), focusedPlanet == null ? null : focusedPlanet.entryId());
    }

    SelectedVisual relayVisual(int width, int height, double clock) {
        if (!relayVisible()) return null;
        var home = relayHome();
        var system = StarmapUniverse.systemOf(home.entryId());
        float[] host;
        float separation;
        if (level == StarmapLevel.GALAXY) {
            host = galaxyPointF(system, 0, 0, width, height);
            separation = 36;
        } else {
            var node = nodePlacement(system, home, false, width, height, clock);
            host = new float[]{node.x(), node.y()};
            separation = level == StarmapLevel.PLANET && home == focusedPlanet ? 49 : 36;
        }
        var point = RelayMapPresentation.offset(host[0], host[1], Math.max(34, viewTransform.scaleLength(separation)));
        return new SelectedVisual(point[0], point[1], viewTransform.scaleLength(16), "poi:abandoned_relay");
    }

    void selectRelay() {
        if (!relayVisible()) return;
        relaySelected = true;
        if (level == StarmapLevel.GALAXY) selectedSystem = null;
        selectedEntry = null;
        centralStarSelected = false;
        refreshComponents();
    }

    void locateRelay() {
        var home = relayHome();
        if (home == null || isEnvironmentLocked()) return;
        var system = StarmapUniverse.systemOf(home.entryId());
        if (!isSystemSelectable(system)) return;
        enterSystem(system);
        selectRelay();
        focusSelectedView();
    }

    boolean relayActive() {
        var state = com.starboundmc.client.space.RelayClientState.snapshot;
        return state != null && state.phase() == com.starboundmc.encounter.RelayData.Phase.ACTIVE.ordinal();
    }

    int relayFuelCost() {
        var state = com.starboundmc.client.space.RelayClientState.snapshot;
        return state == null || relayActive() || java.util.Objects.equals(ClientPlanetState.getCurrentEntryId(), state.homeBody())
                ? 0 : ShipWarpManager.warpFuelCost(ClientPlanetState.getCurrentEntryId(), state.homeBody());
    }

    boolean relayActionAvailable() {
        var state = com.starboundmc.client.space.RelayClientState.snapshot;
        return state != null && relayVisible() && RelayMapPresentation.canAct(state.phase(), state.outsideCrew(),
                ClientPlanetState.isWarping(), isSublightOnline(), isHyperdriveOnline(),
                java.util.Objects.equals(currentSystemId(), StarmapUniverse.systemIdOfEntry(state.homeBody())),
                ClientPlanetState.getFuel(), relayFuelCost());
    }

    Component relayStatus() {
        var state = com.starboundmc.client.space.RelayClientState.snapshot;
        if (state == null) return Component.empty();
        if (state.outsideCrew() > 0) return Component.translatable("gui.starboundmc.relay.waiting", state.outsideCrew());
        if (ClientPlanetState.isWarping()) return Component.translatable("gui.starboundmc.warping");
        if (!relayActive()) {
            if (!isSublightOnline()) return Component.translatable("gui.starboundmc.starmap.sublight_offline");
            if (!java.util.Objects.equals(currentSystemId(), StarmapUniverse.systemIdOfEntry(state.homeBody())) && !isHyperdriveOnline())
                return Component.translatable("gui.starboundmc.starmap.hyperdrive_offline");
            if (ClientPlanetState.getFuel() < relayFuelCost())
                return Component.translatable("gui.starboundmc.starmap.action.insufficient_fuel_detail", relayFuelCost(), ClientPlanetState.getFuel());
        }
        return Component.translatable("gui.starboundmc.relay." + RelayMapPresentation.phaseKey(state.phase()));
    }

    Component relayMission() {
        var state = com.starboundmc.client.space.RelayClientState.snapshot;
        return Component.translatable("gui.starboundmc.relay." + (state != null && state.completed() ? "complete"
                : state != null && state.recovered() ? "return" : "retrieve"));
    }

    Component relayLocation() {
        var home = relayHome();
        return Component.translatable("gui.starboundmc.relay.location", home == null ? Component.empty() : Component.translatable(home.nameKey()));
    }

    float[] galaxyPointF(StarSystemDefinition system, float x, float y, float width, float height) {
        StarmapGalaxyGraph.Node node = galaxyGraph.node(system);
        var position = node == null ? system.galaxyMapPosition() : node.position();
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

    float[] systemPointF(CelestialBodyDefinition entry, int width, int height, double phaseClock) {
        StarmapViewTransform.Point center = viewTransform.toScreen(
                width / 2.0F, height / 2.0F, width, height);
        if (!entry.orbit().isMoon()) {
            float radius = systemOrbitRadius(entry, width, height);
            return orbitPointF(center.x(), center.y(), radius, entry.orbit().orbitAngle(),
                    StarmapOrbitMotion.phase(phaseClock, entry.orbit().orbitRadius()));
        }
        CelestialBodyDefinition parent = StarmapUniverse.body(entry.parentEntryId().orElse(null));
        if (parent == null)
            return new float[] { center.x(), center.y() };
        float[] parentPoint = systemPointF(parent, width, height, phaseClock);
        StarSystemDefinition system = systemForEntry(entry);
        float radius = viewTransform.scaleLength(
                moonDisplayRadius(system, entry, width, height, false));
        return orbitPointF(parentPoint[0], parentPoint[1], radius,
                moonDisplayAngle(system, entry),
                StarmapOrbitMotion.moonPhase(phaseClock, entry.orbit().orbitRadius()));
    }

    private StarSystemDefinition systemForEntry(CelestialBodyDefinition entry) {
        if (entry == null)
            return selectedSystem;
        String systemId = StarmapUniverse.systemIdOfEntry(entry.entryId());
        StarSystemDefinition system = StarmapUniverse.system(systemId);
        return system == null ? selectedSystem : system;
    }

    /** Visual radius for a moon, including room for the parent node and a gap. */
    int moonDisplayRadius(StarSystemDefinition system, CelestialBodyDefinition moon, int width,
                          int height, boolean focusedPlanetView) {
        int minDimension = Math.max(1, Math.min(width, height));
        int ordinal = moonOrdinal(system, moon);
        int authored = focusedPlanetView
                ? moon.orbit().orbitRadius() * minDimension / 150
                : moon.orbit().orbitRadius() * minDimension / 320;
        int minimum = focusedPlanetView ? 44 : 18;
        int spacing = focusedPlanetView ? 16 : 12;
        return Math.max(minimum, authored) + ordinal * spacing;
    }

    private int moonOrdinal(StarSystemDefinition system, CelestialBodyDefinition moon) {
        if (system == null || moon == null)
            return 0;
        int ordinal = 0;
        for (CelestialBodyDefinition candidate : system.bodies()) {
            if (candidate.orbit().isMoon()
                    && java.util.Objects.equals(candidate.parentEntryId().orElse(null), moon.parentEntryId().orElse(null))) {
                if (candidate == moon)
                    return ordinal;
                ordinal++;
            }
        }
        return ordinal;
    }

    /** Spread authored angles into deterministic slots when a parent has many moons. */
    float moonDisplayAngle(StarSystemDefinition system, CelestialBodyDefinition moon) {
        if (system == null || moon == null)
            return moon == null ? 0.0F : moon.orbit().orbitAngle();
        int count = 0;
        for (CelestialBodyDefinition candidate : system.bodies()) {
            if (candidate.orbit().isMoon()
                    && java.util.Objects.equals(candidate.parentEntryId().orElse(null), moon.parentEntryId().orElse(null)))
                count++;
        }
        if (count <= 1)
            return moon.orbit().orbitAngle();
        int ordinal = moonOrdinal(system, moon);
        // Keep a small trace of the authored angle while guaranteeing even
        // separation for dense systems.
        return ordinal * (360.0F / count) + moon.orbit().orbitAngle() * 0.2F;
    }

    record SelectedVisual(float x, float y, float diameter, String key) {
        float selectionRadius() {
            return diameter * 0.5F + Math.max(4.0F, diameter * 0.22F);
        }
    }

}
