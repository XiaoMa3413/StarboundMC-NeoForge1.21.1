package com.starboundmc.client;

import com.starboundmc.client.space.GalaxyEnvironmentBlend;
import com.starboundmc.client.space.StarSystemResolver;
import com.starboundmc.menu.ShipConsoleMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.StartWarpPacket;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * The ship console's star map, three levels deep. The outer starfield view
 * shows one dim star per system on a deep-space canvas: click a star to read
 * its intro (name, type, description, body count) and press "Enter System" to
 * open that system. The system overview shows the star and its bodies on an
 * orbit canvas: pick a primary body (or the star itself, info only), read its
 * details on the right, and select the same body again to open its enlarged
 * focus composition. Moons are overview thumbnails and become selectable only
 * inside their parent's focus composition. Back and right-click step out one
 * level at a time. A
 * cross-system warp plays as a three-stage animation with the view advancing
 * automatically — departure system, then deep space (the ship crosses a
 * dashed route between the two stars), then the arrival system — and all
 * manual view switches are locked while warping. Closing the star map
 * remembers the current page and selection and
 * reopening returns to the same spot. Locked bodies (placeholders
 * with no dimension destination) are visible but cannot be warped to. The
 * interaction is Starbound-style: select first, then confirm.
 */
public class ShipConsoleScreen extends AbstractContainerScreen<ShipConsoleMenu>
{
    // ---- Layout (high-resolution panel) ----
    private static final int PANEL_W = 520;
    private static final int PANEL_H = 290;
    private StarSystem selectedSystem = null;
    private PlanetEntry selectedEntry = null;
    private boolean selectedStar = false;
    private final List<StarmapBody> bodyButtons = new ArrayList<>();
    private StarmapStar starButton = null;
    private PlanetEntry focusedEntry = null;
    private PlanetEntry focusArmedEntry = null;
    private final List<FocusedBody> focusBodyButtons = new ArrayList<>();
    private SciFiButton warpButton = null;

    // ---- Star-map navigation state ----
    private StarmapPage page = StarmapPage.GALAXY;
    private StarSystem selectedGalaxySystem = null;
    private final List<GalaxyStar> galaxyStars = new ArrayList<>();
    private SciFiButton enterButton = null;
    private SciFiButton backButton = null;
    private SciFiButton closeDetailButton = null;
    private StarmapLayout layout;
    private StarmapChromeRenderer chromeRenderer;
    private StarmapOverlayRenderer overlayRenderer;
    private StarmapDetailRenderer detailRenderer;
    private final StarmapDetailContentFactory detailContentFactory
            = new StarmapDetailContentFactory();
    private boolean detailDrawerOpen;

    // ---- Page memory: restored when the star map reopens (session-only) ----
    private static StarmapPage rememberedPage = StarmapPage.GALAXY;
    private static String rememberedSystemId = null;
    private static String rememberedEntryId = null;
    private static String rememberedFocusEntryId = null;
    private static boolean rememberedStar = false;

    public ShipConsoleScreen(ShipConsoleMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void init()
    {
        boolean preserveCompactDrawer = this.layout != null && this.layout.compact() && this.detailDrawerOpen;
        if (this.layout != null)
            this.rememberPage();
        this.layout = StarmapLayout.calculate(this.width, this.height);
        StarmapHiDpiGraphics hiDpi = new StarmapHiDpiGraphics(StarmapUiDensity.forGuiScale(
                Minecraft.getInstance().getWindow().getGuiScale()));
        this.chromeRenderer = new StarmapChromeRenderer(hiDpi);
        this.overlayRenderer = new StarmapOverlayRenderer(hiDpi);
        this.imageWidth = this.layout.panelWidth();
        this.imageHeight = this.layout.panelHeight();
        super.init();
        this.detailRenderer = new StarmapDetailRenderer(this.font, this.layout, hiDpi);
        this.bodyButtons.clear();
        this.starButton = null;
        this.focusedEntry = null;
        this.focusArmedEntry = null;
        this.focusBodyButtons.clear();

        // Galaxy view: one clickable star per system on the deep-space canvas.
        // First click selects the star (intro panel), a second click on the
        // same star enters its system (double-click to enter).
        this.galaxyStars.clear();
        for (StarSystem system : StarSystems.all())
        {
            int[] pos = StarmapGeometry.galaxyPosition(system);
            GalaxyStar star = new GalaxyStar(
                    this.screenCanvasX(pos[0]), this.screenCanvasY(pos[1]),
                    system, b -> {
                        if (this.selectedGalaxySystem == system
                                && (!this.layout.compact() || this.detailDrawerOpen))
                            this.enterSystem(system);
                        else
                        {
                            this.selectGalaxyStar(system);
                            this.openDetailDrawer();
                        }
                    });
            this.galaxyStars.add(star);
            this.addRenderableWidget(star);
        }

        StarmapLayout.Bounds action = this.layout.actionButton();
        StarmapLayout.Bounds back = this.layout.backButton();
        StarmapLayout.Bounds close = this.layout.closeButton();
        this.enterButton = new SciFiButton(this.leftPos + action.x(), this.topPos + action.y(),
                action.width(), action.height(),
                Component.translatable("gui.starboundmc.starmap.enter"),
                b -> this.enterSystem(this.selectedGalaxySystem), hiDpi,
                SciFiButton.Style.PRIMARY);
        this.backButton = new SciFiButton(this.leftPos + back.x(), this.topPos + back.y(),
                back.width(), back.height(),
                Component.translatable("gui.starboundmc.starmap.back"),
                b -> this.backOneLevel(), hiDpi, SciFiButton.Style.SECONDARY);
        this.warpButton = new SciFiButton(this.leftPos + action.x(), this.topPos + action.y(),
                action.width(), action.height(),
                Component.translatable("gui.starboundmc.starmap.warp"),
                b -> this.startWarp(), hiDpi, SciFiButton.Style.PRIMARY);
        this.closeDetailButton = new SciFiButton(this.leftPos + close.x(), this.topPos + close.y(),
                close.width(), close.height(), Component.literal("×"),
                b -> this.closeDetailDrawer(), hiDpi, SciFiButton.Style.SECONDARY);

        // Restore the page the player last left, or default to the galaxy
        // view with the docked star preselected.
        this.page = StarmapPage.GALAXY;
        this.selectedGalaxySystem = null;
        if (rememberedPage == StarmapPage.GALAXY && rememberedSystemId != null)
        {
            this.selectedGalaxySystem = StarSystems.byId(rememberedSystemId);
        }
        PlanetEntry current = StarSystems.entryById(ClientPlanetState.getCurrentEntryId());
        if (this.selectedGalaxySystem == null)
        {
            this.selectedGalaxySystem = current != null
                    ? StarSystems.byId(StarSystems.systemIdOfEntry(current.getEntryId()))
                    : StarSystems.byId(StarSystems.SYS_MAIN);
        }
        if (rememberedPage.showsSystemContext() && rememberedSystemId != null)
        {
            StarSystem sys = StarSystems.byId(rememberedSystemId);
            if (sys != null)
            {
                this.page = StarmapPage.SYSTEM;
                this.selectedSystem = sys;
                this.selectSystem(sys);
                // Restore the selected body/star if it is still valid.
                if (rememberedStar)
                {
                    this.selectStar();
                }
                else if (rememberedEntryId != null)
                {
                    PlanetEntry entry = StarSystems.entryById(rememberedEntryId);
                    if (entry != null && sys.getEntries().contains(entry))
                    {
                        this.selectEntry(rememberedPage == StarmapPage.BODY_FOCUS
                                ? entry : this.systemOverviewEntry(sys, entry));
                    }
                }
                if (rememberedPage == StarmapPage.BODY_FOCUS
                        && rememberedFocusEntryId != null)
                {
                    PlanetEntry focus = StarSystems.entryById(rememberedFocusEntryId);
                    if (focus != null && sys.getEntries().contains(focus))
                    {
                        this.page = StarmapPage.BODY_FOCUS;
                        this.focusedEntry = focus;
                        this.createFocusBodyButtons(focus);
                    }
                }
            }
        }
        this.detailDrawerOpen = !this.layout.compact() || preserveCompactDrawer;
        this.applyMode();
        this.updateInteractionState();
    }

    @Override
    public void onClose()
    {
        this.rememberPage();
        super.onClose();
    }

    private void rememberPage()
    {
        // Remember the page (and selection) so reopening the star map returns
        // to the same spot — including the auto-advanced view of a warp.
        if (this.page == StarmapPage.GALAXY)
        {
            rememberedPage = StarmapPage.GALAXY;
            rememberedSystemId = this.selectedGalaxySystem != null
                    ? this.selectedGalaxySystem.getSystemId() : null;
            rememberedEntryId = null;
            rememberedFocusEntryId = null;
            rememberedStar = false;
        }
        else
        {
            rememberedPage = this.page;
            rememberedSystemId = this.selectedSystem != null
                    ? this.selectedSystem.getSystemId() : null;
            rememberedEntryId = null;
            rememberedFocusEntryId = this.page == StarmapPage.BODY_FOCUS
                    && this.focusedEntry != null ? this.focusedEntry.getEntryId() : null;
            rememberedStar = false;
            if (this.selectedStar)
            {
                rememberedStar = true;
            }
            else if (this.selectedEntry != null)
            {
                rememberedEntryId = this.selectedEntry.getEntryId();
            }
        }
    }

    private void selectSystem(StarSystem system)
    {
        this.selectedSystem = system;
        this.focusArmedEntry = null;
        for (StarmapBody body : this.bodyButtons)
        {
            this.removeWidget(body);
        }
        this.bodyButtons.clear();
        for (PlanetEntry entry : system.getEntries())
        {
            // Moons remain visible on the overview canvas, but their details
            // and warp targets belong exclusively to the parent body's focus
            // composition.
            if (entry.isMoon())
                continue;
            int[] pos = this.bodyPos(entry);
            StarmapBody body = new StarmapBody(pos[0], pos[1], entry, b -> {
                if (this.page == StarmapPage.SYSTEM
                        && this.focusArmedEntry == entry
                        && !ClientPlanetState.isWarping())
                {
                    this.enterBodyFocus(entry);
                }
                else
                {
                    this.selectEntry(entry);
                    this.focusArmedEntry = entry;
                    this.openDetailDrawer();
                }
            });
            this.bodyButtons.add(body);
            this.addRenderableWidget(body);
        }

        // The star itself is clickable: shows the system description only.
        if (this.starButton == null)
        {
            this.starButton = new StarmapStar(this.screenCanvasCenterX(), this.screenCanvasCenterY(), b -> {
                this.selectStar();
                this.openDetailDrawer();
            });
            this.addRenderableWidget(this.starButton);
        }

        // Default selection: the ship's current entry if it is in this system,
        // otherwise the first reachable body.
        PlanetEntry current = StarSystems.entryById(ClientPlanetState.getCurrentEntryId());
        if (current != null && system.getEntries().contains(current))
        {
            this.selectEntry(this.systemOverviewEntry(system, current));
        }
        else
        {
            PlanetEntry first = system.getEntries().stream()
                    .filter(entry -> !entry.isMoon())
                    .filter(PlanetEntry::isReachable)
                    .findFirst()
                    .orElseGet(() -> system.getEntries().stream()
                            .filter(entry -> !entry.isMoon())
                            .findFirst()
                            .orElse(system.getEntries().get(0)));
            this.selectEntry(first);
        }
    }

    private PlanetEntry systemOverviewEntry(StarSystem system, PlanetEntry entry)
    {
        if (!entry.isMoon() || entry.getParentEntryId() == null)
            return entry;
        PlanetEntry parent = StarSystems.entryById(entry.getParentEntryId());
        return parent != null && system.getEntries().contains(parent) ? parent : entry;
    }

    private void selectGalaxyStar(StarSystem system)
    {
        this.selectedGalaxySystem = system;
    }

    private void openDetailDrawer()
    {
        if (this.layout != null && this.layout.compact() && !this.detailDrawerOpen)
        {
            this.detailDrawerOpen = true;
            this.applyMode();
        }
    }

    private void closeDetailDrawer()
    {
        if (this.layout != null && this.layout.compact() && this.detailDrawerOpen)
        {
            this.detailDrawerOpen = false;
            this.applyMode();
        }
    }

    /** Leave the galaxy view and open the selected system's star map. */
    private void enterSystem(StarSystem system)
    {
        if (system == null)
            return;
        this.page = StarmapPage.SYSTEM;
        this.selectSystem(system);
        // The compact galaxy drawer belongs to the system-entry flow. Start
        // the newly opened system with an unobstructed map; its default body
        // remains selected and opens the drawer only after an explicit click.
        if (this.layout != null && this.layout.compact())
            this.detailDrawerOpen = false;
        this.applyMode();
    }

    /** Open the selected body as the centered, enlarged focus composition. */
    private void enterBodyFocus(PlanetEntry entry)
    {
        if (entry == null || this.selectedSystem == null
                || !this.selectedSystem.getEntries().contains(entry)
                || ClientPlanetState.isWarping())
            return;
        this.page = StarmapPage.BODY_FOCUS;
        this.selectEntry(entry);
        this.focusedEntry = entry;
        this.focusArmedEntry = null;
        this.createFocusBodyButtons(entry);
        if (this.layout != null && this.layout.compact())
            this.detailDrawerOpen = false;
        this.applyMode();
    }

    private void createFocusBodyButtons(PlanetEntry entry)
    {
        this.clearFocusBodyButtons();
        for (StarmapFocusGeometry.Placement placement
                : StarmapFocusGeometry.placements(this.selectedSystem, entry))
        {
            PlanetEntry placedEntry = placement.entry();
            FocusedBody body = new FocusedBody(
                    this.screenCanvasX(placement.x()), this.screenCanvasY(placement.y()),
                    placedEntry, placement.diameter(),
                    b -> {
                        this.selectEntry(placedEntry);
                        this.openDetailDrawer();
                    });
            this.focusBodyButtons.add(body);
            this.addRenderableWidget(body);
        }
    }

    private void clearFocusBodyButtons()
    {
        for (FocusedBody body : this.focusBodyButtons)
            this.removeWidget(body);
        this.focusBodyButtons.clear();
    }

    /** Step back one navigation level without discarding the system selection. */
    private void backOneLevel()
    {
        if (this.page == StarmapPage.BODY_FOCUS)
        {
            PlanetEntry overviewEntry = this.focusedEntry == null || this.selectedSystem == null
                    ? null : this.systemOverviewEntry(this.selectedSystem, this.focusedEntry);
            this.page = StarmapPage.SYSTEM;
            this.focusedEntry = null;
            this.focusArmedEntry = null;
            this.clearFocusBodyButtons();
            if (overviewEntry != null)
                this.selectEntry(overviewEntry);
            if (this.layout != null && this.layout.compact())
                this.detailDrawerOpen = false;
            this.applyMode();
            return;
        }
        this.page = StarmapPage.GALAXY;
        this.selectedSystem = null;
        this.selectedEntry = null;
        this.selectedStar = false;
        this.focusedEntry = null;
        this.focusArmedEntry = null;
        this.clearFocusBodyButtons();
        if (this.layout != null && this.layout.compact())
            this.detailDrawerOpen = false;
        this.applyMode();
    }

    /** Show/hide widgets according to the current view (idempotent). */
    private void applyMode()
    {
        for (GalaxyStar star : this.galaxyStars)
        {
            this.removeWidget(star);
        }
        if (this.enterButton != null)
            this.removeWidget(this.enterButton);
        if (this.backButton != null)
            this.removeWidget(this.backButton);
        if (this.warpButton != null)
            this.removeWidget(this.warpButton);
        if (this.closeDetailButton != null)
            this.removeWidget(this.closeDetailButton);
        for (StarmapBody body : this.bodyButtons)
        {
            this.removeWidget(body);
        }
        if (this.starButton != null)
            this.removeWidget(this.starButton);
        for (FocusedBody body : this.focusBodyButtons)
            this.removeWidget(body);

        if (this.page == StarmapPage.GALAXY)
        {
            for (GalaxyStar star : this.galaxyStars)
            {
                this.addRenderableWidget(star);
            }
            if (this.enterButton != null)
                this.addRenderableWidget(this.enterButton);
        }
        else if (this.page == StarmapPage.SYSTEM)
        {
            if (this.backButton != null)
                this.addRenderableWidget(this.backButton);
            if (this.warpButton != null)
                this.addRenderableWidget(this.warpButton);
            for (StarmapBody body : this.bodyButtons)
            {
                this.addRenderableWidget(body);
            }
            if (this.starButton != null)
                this.addRenderableWidget(this.starButton);
        }
        else
        {
            if (this.backButton != null)
                this.addRenderableWidget(this.backButton);
            if (this.warpButton != null)
                this.addRenderableWidget(this.warpButton);
            for (FocusedBody body : this.focusBodyButtons)
                this.addRenderableWidget(body);
        }

        boolean detailVisible = this.layout != null && this.layout.detailVisible(this.detailDrawerOpen);
        if (this.enterButton != null)
            this.enterButton.visible = this.page == StarmapPage.GALAXY && detailVisible;
        if (this.warpButton != null)
            this.warpButton.visible = this.page.showsSystemContext() && detailVisible;
        if (this.backButton != null)
        {
            this.backButton.setMessage(Component.translatable(
                    this.page == StarmapPage.BODY_FOCUS
                            ? "gui.starboundmc.starmap.back_system"
                            : "gui.starboundmc.starmap.back"));
        }
        if (this.closeDetailButton != null && this.layout != null && this.layout.compact()
                && this.detailDrawerOpen)
        {
            this.closeDetailButton.visible = true;
            this.addRenderableWidget(this.closeDetailButton);
        }
    }

    private void selectEntry(PlanetEntry entry)
    {
        this.selectedEntry = entry;
        this.selectedStar = false;
    }

    private void selectStar()
    {
        this.selectedEntry = null;
        this.selectedStar = true;
        this.focusArmedEntry = null;
    }

    /**
     * Auto-advances the view during a cross-system warp: the player watches
     * the departure system (ship flies out), then the deep-space starfield
     * (the ship crosses the dashed route between the two stars), then the
     * arrival system (ship re-enters and docks). The boundaries follow the
     * continuous environment weights rather than discrete flight phases.
     * Same-system warps never leave the current view. All manual view switches
     * are locked while warping (see containerTick).
     */
    private void syncWarpView()
    {
        if (!ClientPlanetState.isWarping())
            return;
        PlanetEntry current = StarSystems.entryById(ClientPlanetState.getCurrentEntryId());
        PlanetEntry target = StarSystems.entryById(ClientPlanetState.getWarpEntryId());
        if (current == null || target == null)
            return;
        StarSystem from = StarSystems.byId(StarSystems.systemIdOfEntry(current.getEntryId()));
        StarSystem to = StarSystems.byId(StarSystems.systemIdOfEntry(target.getEntryId()));
        if (from == null || to == null || from == to)
            return;

        GalaxyEnvironmentBlend environment = StarSystemResolver.latestEnvironment();
        float departureInfluence = environment.influence(from);
        float arrivalInfluence = environment.influence(to);
        if (departureInfluence >= 0.12F && departureInfluence >= arrivalInfluence)
            this.setSystemWarpView(from);
        else if (arrivalInfluence >= 0.12F)
            this.setSystemWarpView(to);
        else
            this.setGalaxyWarpView();
    }

    /** Silent switch to a system view (no selection reset when already there). */
    private void setSystemWarpView(StarSystem system)
    {
        if (this.page != StarmapPage.SYSTEM || this.selectedSystem != system)
        {
            this.focusedEntry = null;
            this.clearFocusBodyButtons();
            this.page = StarmapPage.SYSTEM;
            this.selectedSystem = system;
            this.selectSystem(system);
            this.applyMode();
        }
    }

    /** Silent switch to the galaxy view. */
    private void setGalaxyWarpView()
    {
        if (this.page != StarmapPage.GALAXY)
        {
            this.page = StarmapPage.GALAXY;
            this.selectedSystem = null;
            this.selectedEntry = null;
            this.selectedStar = false;
            this.focusedEntry = null;
            this.focusArmedEntry = null;
            this.clearFocusBodyButtons();
            this.applyMode();
        }
    }

    /** Screen position of a body: around the canvas center, or around its parent for moons. */
    private int[] bodyPos(PlanetEntry entry)
    {
        int[] base = this.bodyPosInBaseCanvas(entry);
        return new int[] { this.screenCanvasX(base[0]), this.screenCanvasY(base[1]) };
    }

    /** Mirrors StarMapCanvas positioning exactly: truncate in base space, then project once. */
    private int[] bodyPosInBaseCanvas(PlanetEntry entry)
    {
        return StarmapGeometry.bodyPosition(entry);
    }

    private int screenCanvasX(int baseX)
    {
        return this.leftPos + this.layout.viewport().projectX(baseX);
    }

    private int screenCanvasY(int baseY)
    {
        return this.topPos + this.layout.viewport().projectY(baseY);
    }

    private int screenCanvasCenterX()
    {
        return this.leftPos + this.layout.viewport().centerX();
    }

    private int screenCanvasCenterY()
    {
        return this.topPos + this.layout.viewport().centerY();
    }

    private int scaledCanvasSize(int baseSize)
    {
        return this.layout.viewport().projectSize(baseSize);
    }

    private void startWarp()
    {
        if (this.selectedEntry == null || !this.selectedEntry.isReachable())
            return;
        if (ClientPlanetState.isWarping())
            return;
        if (this.selectedEntry.getDestination() == ClientPlanetState.getCurrent())
            return;
        PlanetEntry warpTarget = this.selectedEntry;
        if (this.page == StarmapPage.BODY_FOCUS)
        {
            this.page = StarmapPage.SYSTEM;
            this.focusedEntry = null;
            this.focusArmedEntry = null;
            this.clearFocusBodyButtons();
            if (this.selectedSystem != null)
                this.selectEntry(this.systemOverviewEntry(this.selectedSystem, warpTarget));
            if (this.layout != null && this.layout.compact())
                this.detailDrawerOpen = false;
            this.applyMode();
        }
        ModNetwork.sendToServer(new StartWarpPacket(warpTarget.getEntryId()));
        // Keep the star map open: the ship animation plays on the canvas,
        // synchronized with the 3D warp transition rendered by the overlay layer.
    }

    @Override
    public void containerTick()
    {
        super.containerTick();
        this.updateInteractionState();
    }

    private void updateInteractionState()
    {
        boolean warping = ClientPlanetState.isWarping();
        boolean mapInteractive = !warping
                && !(this.layout.compact() && this.detailDrawerOpen);
        int cost = this.selectedEntry == null ? 0
                : ShipWarpManager.warpFuelCost(ClientPlanetState.getCurrentEntryId(), this.selectedEntry.getEntryId());
        if (this.warpButton != null)
        {
            this.warpButton.active = !warping
                    && this.selectedEntry != null
                    && this.selectedEntry.isReachable()
                    && this.selectedEntry.getDestination() != ClientPlanetState.getCurrent()
                    && ClientPlanetState.getFuel() >= cost;
            this.warpButton.setMessage(this.warpActionLabel(warping, cost));
        }
        // During a warp every view switch is locked: the UI advances itself
        // through departure system -> deep space -> arrival system (see
        // syncWarpView), so the player cannot change views mid-flight.
        for (GalaxyStar star : this.galaxyStars)
        {
            star.active = mapInteractive;
        }
        if (this.enterButton != null)
        {
            this.enterButton.active = !warping && this.selectedGalaxySystem != null;
            this.enterButton.setMessage(warping
                    ? Component.translatable("gui.starboundmc.warping")
                    : this.selectedGalaxySystem == null
                            ? Component.translatable("gui.starboundmc.starmap.action.select_star")
                            : Component.translatable("gui.starboundmc.starmap.enter"));
        }
        if (this.backButton != null)
        {
            this.backButton.active = !warping;
        }
        // System view: bodies and the star lock while warping (same as before).
        for (StarmapBody body : this.bodyButtons)
        {
            body.active = mapInteractive;
        }
        if (this.starButton != null)
        {
            this.starButton.active = mapInteractive;
        }
        for (FocusedBody body : this.focusBodyButtons)
        {
            body.active = mapInteractive;
        }
    }

    private Component warpActionLabel(boolean warping, int cost)
    {
        if (warping)
            return Component.translatable("gui.starboundmc.warping");
        if (this.selectedEntry == null)
            return Component.translatable("gui.starboundmc.starmap.action.select_destination");
        if (!this.selectedEntry.isReachable())
            return Component.translatable("gui.starboundmc.starmap.locked");
        if (this.selectedEntry.getDestination() == ClientPlanetState.getCurrent())
            return Component.translatable("gui.starboundmc.starmap.current");
        if (ClientPlanetState.getFuel() < cost)
            return Component.translatable("gui.starboundmc.starmap.action.insufficient_fuel");
        return Component.translatable("gui.starboundmc.starmap.warp");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        // Cross-system warps auto-advance the view: departure system -> deep
        // space -> arrival system. Must run before super.render so the widget
        // set matches the current stage.
        this.syncWarpView();
        super.render(graphics, mouseX, mouseY, partialTick);
        // Ship animation on top of everything, in absolute screen coordinates.
        if (this.page == StarmapPage.GALAXY)
        {
            this.overlayRenderer.renderGalaxyWarp(
                    graphics, this.leftPos, this.topPos, this.layout);
        }
        else if (this.page == StarmapPage.SYSTEM)
        {
            this.overlayRenderer.renderSystemShip(
                    graphics, this.selectedSystem, this.leftPos, this.topPos, this.layout);
        }
        if (this.layout.compact() && this.detailDrawerOpen)
            this.renderCompactDetailDrawer(graphics, mouseX, mouseY, partialTick);
        this.renderActionTooltip(graphics, mouseX, mouseY);
    }

    private void renderActionTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        SciFiButton action = this.page == StarmapPage.GALAXY ? this.enterButton : this.warpButton;
        if (action == null || !action.visible || action.active
                || !isPointInside(action.getX(), action.getY(), action.getWidth(), action.getHeight(), mouseX, mouseY))
            return;
        Component reason = this.actionDisabledReason();
        if (reason != null)
            graphics.renderTooltip(this.font, reason, mouseX, mouseY);
    }

    /** Geometry-only hit test for disabled widgets; AbstractWidget.isMouseOver rejects inactive widgets. */
    static boolean isPointInside(int x, int y, int width, int height, double pointX, double pointY)
    {
        return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
    }

    private Component actionDisabledReason()
    {
        if (ClientPlanetState.isWarping())
            return Component.translatable("gui.starboundmc.warping");
        if (this.page == StarmapPage.GALAXY)
            return this.selectedGalaxySystem == null
                    ? Component.translatable("gui.starboundmc.starmap.action.select_star") : null;
        if (this.selectedEntry == null)
            return Component.translatable("gui.starboundmc.starmap.action.select_destination");
        if (!this.selectedEntry.isReachable())
            return Component.translatable("gui.starboundmc.starmap.locked");
        if (this.selectedEntry.getDestination() == ClientPlanetState.getCurrent())
            return Component.translatable("gui.starboundmc.starmap.current");
        int cost = ShipWarpManager.warpFuelCost(
                ClientPlanetState.getCurrentEntryId(), this.selectedEntry.getEntryId());
        if (ClientPlanetState.getFuel() < cost)
        {
            return Component.translatable("gui.starboundmc.starmap.action.insufficient_fuel_detail",
                    cost, ClientPlanetState.getFuel());
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        // Right-click anywhere in a system-context view steps back one level
        // (with the standard UI button click sound). Locked during a warp
        // (the view advances itself then).
        if (button == 1 && this.page.showsSystemContext() && !ClientPlanetState.isWarping())
        {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.backOneLevel();
            return true;
        }
        if (button == 0 && this.layout.compact() && this.detailDrawerOpen)
        {
            StarmapLayout.Bounds detail = this.layout.detail();
            double localX = mouseX - this.leftPos;
            double localY = mouseY - this.topPos;
            if (localX >= detail.x() && localX < detail.right()
                    && localY >= detail.y() && localY < detail.bottom())
            {
                if (this.closeDetailButton != null
                        && this.closeDetailButton.mouseClicked(mouseX, mouseY, button))
                    return true;
                SciFiButton action = this.page == StarmapPage.GALAXY ? this.enterButton : this.warpButton;
                if (action != null && action.mouseClicked(mouseX, mouseY, button))
                    return true;
                return true;
            }

            // Clicking the exposed map dismisses the modal drawer. Consume
            // this click so closing it cannot also select a body underneath;
            // the player can make a new selection with the next click.
            if (this.layout.viewport().contains(localX, localY))
            {
                // In the galaxy view the second click on the selected star
                // still means "enter system". It takes precedence over the
                // generic click-on-map dismissal below.
                if (this.page == StarmapPage.GALAXY && this.selectedGalaxySystem != null)
                {
                    for (GalaxyStar star : this.galaxyStars)
                    {
                        if (star.system == this.selectedGalaxySystem
                                && star.containsPoint(mouseX, mouseY))
                        {
                            this.enterSystem(this.selectedGalaxySystem);
                            return true;
                        }
                    }
                }
                if (this.page == StarmapPage.SYSTEM && this.selectedEntry != null
                        && this.focusArmedEntry == this.selectedEntry)
                {
                    for (StarmapBody body : this.bodyButtons)
                    {
                        if (body.entry == this.selectedEntry
                                && body.containsPoint(mouseX, mouseY))
                        {
                            this.enterBodyFocus(this.selectedEntry);
                            return true;
                        }
                    }
                }
                this.closeDetailDrawer();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        StarmapLayout.Bounds canvasBounds = this.layout.canvas();
        int canvasX = this.leftPos + canvasBounds.x();
        int canvasY = this.topPos + canvasBounds.y();
        int canvasWidth = canvasBounds.width();
        int canvasHeight = canvasBounds.height();
        float fuelRatio = Math.max(0.0F, Math.min(1.0F,
                (float) ClientPlanetState.getFuel() / Math.max(1, ClientPlanetState.getMaxFuel())));
        this.chromeRenderer.renderBase(graphics, this.width, this.height,
                this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                this.layout, fuelRatio);
        // The static scenery (background, stars, orbits, star glow, radiation
        // ring, planet disks) comes from a supersampled texture rendered by
        // StarMapCanvas — bilinear-filtered, so it stays crisp at any GUI scale.
        if (this.page == StarmapPage.GALAXY)
        {
            // Outer view: the deep-space starfield with a dim star per system.
            StarMapCanvas.CanvasTexture galaxy = StarMapCanvas.galaxy(canvasWidth, canvasHeight);
            graphics.blit(galaxy.location(), canvasX, canvasY, canvasWidth, canvasHeight,
                    0, 0, galaxy.width(), galaxy.height(), galaxy.width(), galaxy.height());
        }
        else if (this.page == StarmapPage.BODY_FOCUS
                && this.selectedSystem != null && this.focusedEntry != null)
        {
            StarMapCanvas.CanvasTexture canvas = StarMapCanvas.focus(
                    this.selectedSystem, this.focusedEntry, canvasWidth, canvasHeight);
            graphics.blit(canvas.location(), canvasX, canvasY, canvasWidth, canvasHeight,
                    0, 0, canvas.width(), canvas.height(), canvas.width(), canvas.height());
        }
        else if (this.selectedSystem != null)
        {
            StarMapCanvas.CanvasTexture canvas = StarMapCanvas.get(
                    this.selectedSystem, canvasWidth, canvasHeight);
            graphics.blit(canvas.location(), canvasX, canvasY, canvasWidth, canvasHeight,
                    0, 0, canvas.width(), canvas.height(), canvas.width(), canvas.height());
        }
        else
        {
            graphics.fillGradient(canvasX, canvasY, canvasX + canvasWidth, canvasY + canvasHeight,
                    StarmapVisualTheme.DISPLAY_TOP, StarmapVisualTheme.DISPLAY_BOTTOM);
        }
        this.chromeRenderer.renderCanvasOverlay(graphics,
                canvasX, canvasY, canvasWidth, canvasHeight);

        // Radiation label, drawn over the texture (absolute coords).
        if (this.page == StarmapPage.SYSTEM && this.selectedSystem != null
                && this.selectedSystem.getRadiationRadius() > 0)
        {
            int rr = Math.round(this.selectedSystem.getRadiationRadius() * this.layout.viewport().scale());
            int cx = this.screenCanvasCenterX();
            int cy = this.screenCanvasCenterY();
            Component rad = Component.translatable("gui.starboundmc.starmap.radiation");
            graphics.drawString(this.font, rad, cx + rr + 3, cy - 4,
                    StarmapVisualTheme.STATUS_RADIATION, false);
        }

    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        // Title plate is drawn by the chrome renderer; only the label belongs here.
        Component title = Component.translatable("gui.starboundmc.starmap.title");
        graphics.drawString(this.font, title, this.titleLabelX, this.titleLabelY,
                StarmapVisualTheme.TEXT_STANDARD, true);

        // Fuel readout, centered above the fuel bar.
        Component fuelText = Component.translatable("gui.starboundmc.fuel",
                ClientPlanetState.getFuel(), ClientPlanetState.getMaxFuel());
        graphics.drawString(this.font, fuelText,
                this.layout.canvas().x() + (this.layout.canvas().width() - this.font.width(fuelText)) / 2,
                6, StarmapVisualTheme.STATUS_FUEL, true);

        // View label above the canvas.
        int viewLabelX = Math.max(this.layout.canvas().x(), this.layout.backButton().right() + 6);
        int viewLabelY = this.layout.canvas().y() - 10;
        if (this.page == StarmapPage.GALAXY)
        {
            graphics.drawString(this.font, Component.translatable("gui.starboundmc.starmap.starfield"),
                    viewLabelX, viewLabelY, StarmapVisualTheme.TEXT_SECONDARY, true);
        }
        else if (this.page == StarmapPage.BODY_FOCUS
                && this.selectedSystem != null && this.focusedEntry != null)
        {
            Component breadcrumb = Component.literal(
                    Component.translatable(this.selectedSystem.getNameKey()).getString()
                            + " > "
                            + Component.translatable(this.focusedEntry.getNameKey()).getString());
            graphics.drawString(this.font, breadcrumb,
                    viewLabelX, viewLabelY, StarmapVisualTheme.TEXT_SECONDARY, true);
        }
        else if (this.selectedSystem != null)
        {
            graphics.drawString(this.font, Component.translatable(this.selectedSystem.getNameKey()),
                    viewLabelX, viewLabelY, StarmapVisualTheme.TEXT_SECONDARY, true);
        }

        if (!this.layout.compact())
            this.renderSelectedDetails(graphics);

        if (ClientPlanetState.isWarping())
        {
            Component warping = Component.translatable("gui.starboundmc.warping");
            graphics.drawString(this.font, warping,
                    (this.imageWidth - this.font.width(warping)) / 2,
                    this.imageHeight - 14, StarmapVisualTheme.STATUS_FUEL, true);
            int progressY = this.imageHeight - 4;
            int progressWidth = Math.min(this.layout.canvas().width(), this.imageWidth - 16);
            int progressX = (this.imageWidth - progressWidth) / 2;
            float progress = Math.max(0.0F, Math.min(1.0F, ClientPlanetState.warpProgress()));
            this.overlayRenderer.drawProgressBar(graphics,
                    progressX, progressY, progressWidth, 2, progress,
                    StarmapVisualTheme.FUEL_TRACK, StarmapVisualTheme.ACCENT);
        }
    }

    private void renderSelectedDetails(GuiGraphics graphics)
    {
        if (this.page == StarmapPage.GALAXY)
            this.detailRenderer.render(graphics,
                    this.detailContentFactory.galaxy(this.selectedGalaxySystem));
        else if (this.selectedStar)
            this.detailRenderer.render(graphics,
                    this.detailContentFactory.star(this.selectedSystem));
        else if (this.selectedEntry != null)
            this.detailRenderer.render(graphics,
                    this.detailContentFactory.entry(this.selectedEntry));
    }

    private void renderDetailPanelBackground(GuiGraphics graphics, boolean opaque)
    {
        StarmapLayout.Bounds detail = this.layout.detail();
        int x = this.leftPos + detail.x();
        int y = this.topPos + detail.y();
        this.chromeRenderer.renderDetailPanel(graphics,
                x, y, detail.width(), detail.height(), opaque);
    }

    private void renderCompactDetailDrawer(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        this.renderDetailPanelBackground(graphics, true);
        StarmapLayout.Bounds detail = this.layout.detail();
        StarmapLayout.Bounds action = this.layout.actionButton();
        graphics.enableScissor(this.leftPos + detail.x(), this.topPos + detail.y(),
                this.leftPos + detail.right(), this.topPos + Math.max(detail.y(), action.y() - 2));
        graphics.pose().pushPose();
        graphics.pose().translate(this.leftPos, this.topPos, 0.0F);
        this.renderSelectedDetails(graphics);
        graphics.pose().popPose();
        graphics.disableScissor();

        if (this.page == StarmapPage.GALAXY && this.enterButton != null && this.enterButton.visible)
            this.enterButton.render(graphics, mouseX, mouseY, partialTick);
        if (this.page.showsSystemContext() && this.warpButton != null && this.warpButton.visible)
            this.warpButton.render(graphics, mouseX, mouseY, partialTick);
        if (this.closeDetailButton != null && this.closeDetailButton.visible)
            this.closeDetailButton.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    private static int selectionReticleColor()
    {
        float pulse = 0.5F + 0.5F
                * (float) Math.sin(System.currentTimeMillis() / 260.0D);
        int alpha = Math.round(StarmapVisualTheme.SELECTION_ALPHA_MIN
                + (StarmapVisualTheme.SELECTION_ALPHA_MAX
                        - StarmapVisualTheme.SELECTION_ALPHA_MIN) * pulse);
        return (alpha << 24) | (StarmapVisualTheme.SELECTION & 0xFFFFFF);
    }

    /**
     * A star on the outer starfield view. Rendered with a core that is bright
     * when the system has been visited and dim when unexplored, a small ship
     * arrow when the ship is docked in that system, a cyan targeting reticle when
     * selected, and the system name / body count labels.
     */
    private class GalaxyStar extends Button
    {
        private final StarSystem system;
        private final int centerX;
        private final int centerY;
        private final int coreRadius;
        private final int ringRadius;

        GalaxyStar(int centerX, int centerY, StarSystem system, OnPress onPress)
        {
            super(centerX - Math.max(16, ShipConsoleScreen.this.scaledCanvasSize(16)),
                    centerY - Math.max(16, ShipConsoleScreen.this.scaledCanvasSize(16)),
                    Math.max(32, ShipConsoleScreen.this.scaledCanvasSize(32)),
                    Math.max(32, ShipConsoleScreen.this.scaledCanvasSize(32)),
                    Component.translatable(system.getNameKey()), onPress, DEFAULT_NARRATION);
            this.system = system;
            this.centerX = centerX;
            this.centerY = centerY;
            this.coreRadius = ShipConsoleScreen.this.scaledCanvasSize(4);
            this.ringRadius = ShipConsoleScreen.this.scaledCanvasSize(13);
        }

        private boolean containsPoint(double mouseX, double mouseY)
        {
            return mouseX >= this.getX() && mouseX < this.getX() + this.getWidth()
                    && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        {
            boolean selected = ShipConsoleScreen.this.selectedGalaxySystem == this.system;
            PlanetEntry current = StarSystems.entryById(ClientPlanetState.getCurrentEntryId());
            boolean docked = current != null && this.system.getEntries().contains(current);
            boolean visited = this.system.getEntries().stream()
                    .anyMatch(entry -> entry.isReachable() && ClientPlanetState.isVisited(entry.getEntryId()));

            // Core: bright when visited, dim when unexplored.
            int coreAlpha = visited ? 255 : 80;
            ShipConsoleScreen.this.overlayRenderer.drawDisk(
                    graphics, this.centerX, this.centerY, this.coreRadius,
                    (coreAlpha << 24) | (this.system.getStarColor() & 0xFFFFFF));
            if (visited)
            {
                ShipConsoleScreen.this.overlayRenderer.drawDisk(
                        graphics, this.centerX - 1, this.centerY - 1,
                        Math.max(1, this.coreRadius / 4), 0xFFFFFFFF);
            }

            // Selection / hover ring.
            if (selected)
            {
                ShipConsoleScreen.this.overlayRenderer.drawSelectionReticle(
                        graphics, this.centerX, this.centerY, this.ringRadius,
                        selectionReticleColor());
            }
            else if (this.isHoveredOrFocused())
            {
                ShipConsoleScreen.this.overlayRenderer.drawRing(
                        graphics, this.centerX, this.centerY, this.ringRadius,
                        StarmapVisualTheme.HOVER_RING);
            }

            // Docked: a small ship arrow parked on top identifies the current system.
            if (docked)
            {
                ShipConsoleScreen.this.overlayRenderer.drawShip(graphics,
                        this.centerX, this.centerY - this.ringRadius - 3,
                        0.0, 0.0, 255);
            }

            // Labels: name above, body count below.
            Component name = Component.translatable(this.system.getNameKey());
            int nameColor = selected || this.isHoveredOrFocused()
                    ? StarmapVisualTheme.TEXT_PRIMARY : StarmapVisualTheme.TEXT_STANDARD;
            graphics.drawCenteredString(ShipConsoleScreen.this.font, name,
                    this.centerX, this.centerY - this.ringRadius - 17, nameColor);
            graphics.drawCenteredString(ShipConsoleScreen.this.font,
                    Component.translatable("gui.starboundmc.starmap.bodies", this.system.getEntries().size()),
                    this.centerX, this.centerY + this.ringRadius + 7,
                    StarmapVisualTheme.TEXT_SECONDARY);
        }
    }

    /** Clickable star at the canvas center; shows only the selection ring (the glow is drawn in renderBg). */
    private class StarmapStar extends Button
    {
        private final int centerX;
        private final int centerY;
        private final int ringRadius;

        StarmapStar(int centerX, int centerY, OnPress onPress)
        {
            super(centerX - Math.max(12, ShipConsoleScreen.this.scaledCanvasSize(12)),
                    centerY - Math.max(12, ShipConsoleScreen.this.scaledCanvasSize(12)),
                    Math.max(24, ShipConsoleScreen.this.scaledCanvasSize(24)),
                    Math.max(24, ShipConsoleScreen.this.scaledCanvasSize(24)),
                    Component.translatable("gui.starboundmc.starmap.star"),
                    onPress, DEFAULT_NARRATION);
            this.centerX = centerX;
            this.centerY = centerY;
            this.ringRadius = ShipConsoleScreen.this.scaledCanvasSize(8);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        {
            boolean selected = ShipConsoleScreen.this.selectedStar;
            if (selected)
            {
                ShipConsoleScreen.this.overlayRenderer.drawSelectionReticle(
                        graphics, this.centerX, this.centerY, this.ringRadius,
                        selectionReticleColor());
            }
            else if (this.isHoveredOrFocused())
            {
                ShipConsoleScreen.this.overlayRenderer.drawRing(
                        graphics, this.centerX, this.centerY, this.ringRadius,
                        StarmapVisualTheme.HOVER_RING);
            }
        }
    }

    /** Click target for the enlarged body in the focus composition. */
    private class FocusedBody extends Button
    {
        private final PlanetEntry entry;
        private final int centerX;
        private final int centerY;
        private final int markerRadius;

        FocusedBody(int centerX, int centerY, PlanetEntry entry,
                    int markerDiameter, OnPress onPress)
        {
            super(centerX - Math.max(4, ShipConsoleScreen.this.scaledCanvasSize(
                            markerDiameter / 2)) - 2,
                    centerY - Math.max(4, ShipConsoleScreen.this.scaledCanvasSize(
                            markerDiameter / 2)) - 2,
                    Math.max(4, ShipConsoleScreen.this.scaledCanvasSize(
                            markerDiameter / 2)) * 2 + 4,
                    Math.max(4, ShipConsoleScreen.this.scaledCanvasSize(
                            markerDiameter / 2)) * 2 + 4,
                    Component.translatable(entry.getNameKey()), onPress, DEFAULT_NARRATION);
            this.entry = entry;
            this.centerX = centerX;
            this.centerY = centerY;
            this.markerRadius = Math.max(4, ShipConsoleScreen.this.scaledCanvasSize(
                    markerDiameter / 2));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        {
            int r = this.markerRadius;
            boolean selected = ShipConsoleScreen.this.selectedEntry == this.entry;
            if (selected)
            {
                ShipConsoleScreen.this.overlayRenderer.drawSelectionReticle(
                        graphics, this.centerX, this.centerY, r + 3,
                        selectionReticleColor());
            }
            else if (this.isHoveredOrFocused())
                ShipConsoleScreen.this.overlayRenderer.drawRing(
                        graphics, this.centerX, this.centerY, r + 3,
                        StarmapVisualTheme.HOVER_RING);

            if (!this.entry.isReachable())
            {
                ShipConsoleScreen.this.overlayRenderer.drawLockedCross(
                        graphics, this.centerX, this.centerY, r,
                        StarmapVisualTheme.STATUS_DANGER);
            }

            int nameColor = this.entry.isReachable()
                    ? (this.isHoveredOrFocused() || selected
                            ? StarmapVisualTheme.TEXT_PRIMARY : StarmapVisualTheme.TEXT_STANDARD)
                    : StarmapVisualTheme.TEXT_DISABLED;
            graphics.drawCenteredString(ShipConsoleScreen.this.font,
                    Component.translatable(this.entry.getNameKey()),
                    this.centerX, this.centerY + r + 5, nameColor);
        }
    }

    /**
     * A clickable circular body marker on the system view: shaded disk with a
     * highlight and name above; selected bodies use a cyan targeting reticle, while
     * locked bodies are grey with a red cross.
     */
    private class StarmapBody extends Button
    {
        private final PlanetEntry entry;
        private final int centerX;
        private final int centerY;
        private final int markerRadius;

        StarmapBody(int centerX, int centerY, PlanetEntry entry, OnPress onPress)
        {
            super(centerX - Math.max(3, ShipConsoleScreen.this.scaledCanvasSize(entry.getMarkerSize() / 2)) - 2,
                    centerY - Math.max(3, ShipConsoleScreen.this.scaledCanvasSize(entry.getMarkerSize() / 2)) - 2,
                    Math.max(3, ShipConsoleScreen.this.scaledCanvasSize(entry.getMarkerSize() / 2)) * 2 + 4,
                    Math.max(3, ShipConsoleScreen.this.scaledCanvasSize(entry.getMarkerSize() / 2)) * 2 + 4,
                    Component.translatable(entry.getNameKey()), onPress, DEFAULT_NARRATION);
            this.entry = entry;
            this.centerX = centerX;
            this.centerY = centerY;
            this.markerRadius = Math.max(3,
                    ShipConsoleScreen.this.scaledCanvasSize(entry.getMarkerSize() / 2));
        }

        private boolean containsPoint(double mouseX, double mouseY)
        {
            return mouseX >= this.getX() && mouseX < this.getX() + this.getWidth()
                    && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        {
            int r = this.markerRadius;
            boolean selected = ShipConsoleScreen.this.selectedEntry == this.entry;

            // Selection/hover rings (the disk itself is part of the canvas texture).
            if (selected)
            {
                ShipConsoleScreen.this.overlayRenderer.drawSelectionReticle(
                        graphics, this.centerX, this.centerY, r + 2,
                        selectionReticleColor());
            }
            else if (this.isHoveredOrFocused())
            {
                ShipConsoleScreen.this.overlayRenderer.drawRing(
                        graphics, this.centerX, this.centerY, r + 2,
                        StarmapVisualTheme.HOVER_RING);
            }

            // Locked: red cross over the disk.
            if (!this.entry.isReachable())
            {
                ShipConsoleScreen.this.overlayRenderer.drawLockedCross(
                        graphics, this.centerX, this.centerY, r,
                        StarmapVisualTheme.STATUS_DANGER);
            }

            // Docked: the ship triangle sits above the body (see renderShip).
            // Name label below the body, leaving the space above free for the ship.
            Component name = Component.translatable(this.entry.getNameKey());
            int nameColor = this.entry.isReachable()
                    ? (this.isHoveredOrFocused() || selected
                            ? StarmapVisualTheme.TEXT_PRIMARY : StarmapVisualTheme.TEXT_STANDARD)
                    : StarmapVisualTheme.TEXT_DISABLED;
            graphics.drawCenteredString(ShipConsoleScreen.this.font, name,
                    this.centerX, this.centerY + r + 3, nameColor);
        }
    }
}
