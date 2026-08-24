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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/** Full-screen LDLib2 canvas for the new starmap terminal. */
public final class StarmapTerminalRoot extends UIElement {
    private static final int BASE_WIDTH = 250;
    private static final int BASE_HEIGHT = 220;
    private static final int BACKGROUND = 0xFF050912;
    private static final int STAR = 0xFFB9D7E5;
    private static final int ACCENT = 0xFF63E2DF;
    private static final int MUTED = 0xFF8CA2B3;
    private static final int GRID = 0x243A6373;
    private static final int ORBIT = 0x665B91A5;
    private static final int ORBIT_SELECTED = 0xB563E2DF;
    private static final int PANEL = 0xD90A1420;
    private static final int BUTTON = 0xFF1A4B57;

    private StarmapLevel level = StarmapLevel.GALAXY;
    private StarSystem selectedSystem;
    private PlanetEntry selectedEntry;
    private PlanetEntry focusedPlanet;
    /** Accumulated reference-clock ticks; each orbit derives its own phase. */
    private float orbitClock;

    public StarmapTerminalRoot() {
        addClass("starmap-redraw-root");
        layout(layout -> layout.widthPercent(100).heightPercent(100));
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.TICK, event -> orbitClock += 1.0F);
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
        if (selectedTarget() != null && isInsideAction(localX, localY, width)) {
            performAction();
            event.stopPropagation();
            return;
        }
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
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        GuiGraphics graphics = context.graphics;
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY());
        int width = Math.max(1, Math.round(getSizeWidth()));
        int height = Math.max(1, Math.round(getSizeHeight()));
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
        drawStars(graphics, x, y, width, height);
        if (level == StarmapLevel.GALAXY)
            drawGalaxy(graphics, x, y, width, height);
        else if (level == StarmapLevel.SYSTEM)
            drawSystem(graphics, x, y, width, height);
        else
            drawPlanet(graphics, x, y, width, height);
        drawChrome(graphics, x, y, width, height);
        drawInfo(graphics, x, y, width, height);
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
        for (StarSystem system : StarSystems.all()) {
            int[] position = galaxyPoint(system, x, y, width, height);
            int radius = system == selectedSystem ? 10 : 7;
            drawStarNode(graphics, position[0], position[1], radius, system.getStarColor(),
                    system == selectedSystem);
        }
    }

    private void drawSystem(GuiGraphics graphics, int x, int y, int width, int height) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        drawStarNode(graphics, centerX, centerY, 12, selectedSystem.getStarColor(), false);
        for (PlanetEntry entry : selectedSystem.getEntries()) {
            if (entry.isMoon())
                continue;
            int radius = Math.max(18, entry.getOrbitRadius() * Math.min(width, height) / 220);
            drawOrbit(graphics, centerX, centerY, radius,
                    entry == selectedEntry ? ORBIT_SELECTED : ORBIT);
            int[] point = orbitPoint(centerX, centerY, radius, entry.getOrbitAngle(),
                    StarmapOrbitMotion.phase(orbitClock, entry.getOrbitRadius()));
            int bodySize = 6;
            drawBodyNode(graphics, point[0], point[1], bodySize,
                    entry == selectedEntry ? ACCENT : entry.getVisual().getPrimaryColor(),
                    entry == selectedEntry);
        }
        for (PlanetEntry moon : selectedSystem.getEntries()) {
            if (!moon.isMoon())
                continue;
            int[] point = systemPoint(moon, width, height);
            drawBodyNode(graphics, point[0], point[1], 2, moon.getVisual().getPrimaryColor(), false);
        }
    }

    private void drawPlanet(GuiGraphics graphics, int x, int y, int width, int height) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        if (focusedPlanet == null)
            return;
        drawBodyNode(graphics, centerX, centerY, 24,
                selectedEntry == focusedPlanet ? ACCENT : focusedPlanet.getVisual().getPrimaryColor(),
                selectedEntry == focusedPlanet);
        for (PlanetEntry entry : selectedSystem.getEntries()) {
            if (!entry.isMoon() || !entry.getParentEntryId().equals(focusedPlanet.getEntryId()))
                continue;
            int radius = Math.max(22, entry.getOrbitRadius() * Math.min(width, height) / 180);
            drawOrbit(graphics, centerX, centerY, radius,
                    entry == selectedEntry ? ORBIT_SELECTED : ORBIT);
            int[] point = orbitPoint(centerX, centerY, radius, entry.getOrbitAngle(),
                    StarmapOrbitMotion.moonPhase(orbitClock, entry.getOrbitRadius()));
            drawBodyNode(graphics, point[0], point[1], 5,
                    entry == selectedEntry ? ACCENT : entry.getVisual().getPrimaryColor(),
                    entry == selectedEntry);
        }
    }

    private void drawChrome(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.renderOutline(x + 8, y + 8, width - 16, height - 16, 0xFF24485B);
        graphics.renderOutline(x + 12, y + 12, width - 24, height - 24, 0x66305C70);
        drawCornerMarks(graphics, x + 8, y + 8, width - 16, height - 16);
        graphics.fill(x + 22, y + 31, x + width - 22, y + 32, 0x664B8EA0);
        graphics.drawString(Minecraft.getInstance().font, levelLabel(), x + 22, y + 18, ACCENT, false);
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("gui.starboundmc.starmap.redraw.back_hint"),
                x + width - 170, y + 18, MUTED, false);
    }

    private static void drawStarNode(GuiGraphics graphics, int centerX, int centerY, int radius,
                                     int color, boolean selected) {
        int halo = selected ? 7 : 4;
        drawOrbit(graphics, centerX, centerY, radius + halo, selected ? 0x995FCFD0 : 0x555B91A5);
        graphics.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius, color);
        graphics.fill(centerX - radius / 2, centerY - radius / 2,
                centerX + radius / 2, centerY + radius / 2, 0x66FFF4C2);
        graphics.fill(centerX - radius - 3, centerY, centerX + radius + 4, centerY + 1,
                selected ? 0xAA63E2DF : 0x664B7483);
        graphics.fill(centerX, centerY - radius - 3, centerX + 1, centerY + radius + 4,
                selected ? 0xAA63E2DF : 0x664B7483);
    }

    private static void drawBodyNode(GuiGraphics graphics, int centerX, int centerY, int radius,
                                     int color, boolean selected) {
        if (selected)
            drawOrbit(graphics, centerX, centerY, radius + 4, 0xC063E2DF);
        graphics.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius, color);
        graphics.fill(centerX - Math.max(1, radius / 2), centerY - Math.max(1, radius / 2),
                centerX + Math.max(1, radius / 2), centerY + Math.max(1, radius / 2), 0x55FFFFFF);
        graphics.renderOutline(centerX - radius - 1, centerY - radius - 1,
                radius * 2 + 2, radius * 2 + 2, selected ? ACCENT : 0x886B94A4);
    }

    private static void drawCornerMarks(GuiGraphics graphics, int x, int y, int width, int height) {
        int color = 0xFF63A5B0;
        int mark = 12;
        graphics.fill(x, y, x + mark, y + 2, color);
        graphics.fill(x, y, x + 2, y + mark, color);
        graphics.fill(x + width - mark, y, x + width, y + 2, color);
        graphics.fill(x + width - 2, y, x + width, y + mark, color);
        graphics.fill(x, y + height - 2, x + mark, y + height, color);
        graphics.fill(x, y + height - mark, x + 2, y + height, color);
        graphics.fill(x + width - mark, y + height - 2, x + width, y + height, color);
        graphics.fill(x + width - 2, y + height - mark, x + width, y + height, color);
    }

    private void drawInfo(GuiGraphics graphics, int x, int y, int width, int height) {
        if (selectedSystem == null && selectedEntry == null)
            return;
        Font font = Minecraft.getInstance().font;
        int panelWidth = Math.min(230, Math.max(170, width / 4));
        int panelX = Math.max(x + 14, x + width - panelWidth - 18);
        int panelY = y + 38;
        int panelHeight = selectedTarget() == null ? 82 : 122;
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
        Component title = selectedEntry == null
                ? Component.translatable(selectedSystem.getNameKey())
                : Component.translatable(selectedEntry.getNameKey());
        graphics.drawString(font, title, panelX + 8, panelY + 8, 0xFFEAF5F7, false);
        Component subtitle = selectedEntry == null
                ? Component.translatable(selectedSystem.getStarTypeKey())
                : Component.translatable(selectedEntry.getTypeKey());
        graphics.drawString(font, subtitle, panelX + 8, panelY + 22, ACCENT, false);
        if (selectedEntry == null) {
            graphics.drawString(font, Component.translatable(
                    "gui.starboundmc.starmap.redraw.body_count", selectedSystem.getEntries().size()),
                    panelX + 8, panelY + 38, MUTED, false);
            String description = selectedSystem.getDescriptionKey();
            graphics.drawString(font, font.plainSubstrByWidth(
                    Component.translatable(description).getString(), panelWidth - 16),
                    panelX + 8, panelY + 54, MUTED, false);
        } else {
            graphics.drawString(font, Component.translatable(
                    "gui.starboundmc.starmap.threat", selectedEntry.getThreatLevel()),
                    panelX + 8, panelY + 38, MUTED, false);
            graphics.drawString(font, font.plainSubstrByWidth(
                    Component.translatable(selectedEntry.getDescriptionKey()).getString(), panelWidth - 16),
                    panelX + 8, panelY + 54, MUTED, false);
            int buttonY = panelY + panelHeight - 25;
            boolean active = level == StarmapLevel.SYSTEM
                    ? !selectedEntry.isMoon()
                    : isWarpAvailable();
            graphics.fill(panelX + 8, buttonY, panelX + panelWidth - 8, buttonY + 17,
                    active ? BUTTON : 0xFF18232C);
            Component action = level == StarmapLevel.PLANET
                    ? Component.translatable("gui.starboundmc.starmap.warp")
                    : Component.translatable("gui.starboundmc.starmap.redraw.inspect");
            graphics.drawCenteredString(font, action, panelX + panelWidth / 2, buttonY + 4,
                    active ? 0xFFEAF5F7 : 0xFF6E7C86);
        }
    }

    private PlanetEntry selectedTarget() {
        return selectedEntry;
    }

    private boolean isInsideAction(float localX, float localY, float width) {
        int panelWidth = Math.min(230, Math.max(170, Math.round(width) / 4));
        int panelX = Math.max(14, Math.round(width) - panelWidth - 18);
        int panelY = 38;
        int buttonY = panelY + 122 - 25;
        return localX >= panelX + 8 && localX < panelX + panelWidth - 8
                && localY >= buttonY && localY < buttonY + 17;
    }

    private void performAction() {
        if (selectedEntry == null)
            return;
        if (level == StarmapLevel.SYSTEM && !selectedEntry.isMoon()) {
            focusedPlanet = selectedEntry;
            level = StarmapLevel.PLANET;
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

    private Component levelLabel() {
        return Component.translatable("gui.starboundmc.starmap.redraw.level." + level.name().toLowerCase());
    }

    private int[] galaxyPoint(StarSystem system, int x, int y, int width, int height) {
        int[] base = StarmapGeometry.galaxyPosition(system);
        return new int[] { x + base[0] * width / BASE_WIDTH, y + base[1] * height / BASE_HEIGHT };
    }

    private static int[] orbitPoint(int centerX, int centerY, int radius, float angle, float phase) {
        double radians = Math.toRadians(angle) + phase;
        return new int[] { centerX + Math.round((float) Math.cos(radians) * radius),
                centerY + Math.round((float) Math.sin(radians) * radius) };
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
            int[] point = systemPoint(entry, Math.round(width), Math.round(height));
            double current = Math.hypot(localX - point[0], localY - point[1]);
            if (current <= (entry.isMoon() ? 12 : 18) && current < distance) {
                nearest = entry.isMoon() ? StarSystems.entryById(entry.getParentEntryId()) : entry;
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
            int radius = Math.max(22, moon.getOrbitRadius() * Math.min(Math.round(width), Math.round(height)) / 180);
            int[] point = orbitPoint(centerX, centerY, radius, moon.getOrbitAngle(),
                    StarmapOrbitMotion.moonPhase(orbitClock, moon.getOrbitRadius()));
            double current = Math.hypot(localX - point[0], localY - point[1]);
            if (current <= 14 && current < distance) {
                nearest = moon;
                distance = current;
            }
        }
        return nearest;
    }

    private int[] systemPoint(PlanetEntry entry, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        if (!entry.isMoon()) {
            int radius = Math.max(18, entry.getOrbitRadius() * Math.min(width, height) / 220);
            return orbitPoint(centerX, centerY, radius, entry.getOrbitAngle(),
                    StarmapOrbitMotion.phase(orbitClock, entry.getOrbitRadius()));
        }
        PlanetEntry parent = StarSystems.entryById(entry.getParentEntryId());
        if (parent == null)
            return new int[] { centerX, centerY };
        int[] parentPoint = systemPoint(parent, width, height);
        int radius = Math.max(8, entry.getOrbitRadius() * Math.min(width, height) / 360);
        return orbitPoint(parentPoint[0], parentPoint[1], radius,
                entry.getOrbitAngle(), StarmapOrbitMotion.moonPhase(orbitClock, entry.getOrbitRadius()));
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
