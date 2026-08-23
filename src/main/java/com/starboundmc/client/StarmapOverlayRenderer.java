package com.starboundmc.client;

import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.world.Planet;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.client.gui.GuiGraphics;

/** Draws live star-map markers and flight graphics in high-density space. */
public final class StarmapOverlayRenderer
{
    private final StarmapHiDpiGraphics hiDpi;

    public StarmapOverlayRenderer(StarmapHiDpiGraphics hiDpi)
    {
        if (hiDpi == null)
            throw new IllegalArgumentException("hiDpi must not be null");
        this.hiDpi = hiDpi;
    }

    /** Renders the docked ship or the system-side portion of an active warp. */
    public void renderSystemShip(GuiGraphics graphics, StarSystem viewedSystem,
                                 int panelX, int panelY, StarmapLayout layout)
    {
        if (viewedSystem == null)
            return;
        PlanetEntry current = resolveCurrentEntry();
        if (current == null)
            return;
        boolean warping = ClientPlanetState.isWarping();
        PlanetEntry target = null;
        if (warping)
        {
            target = StarSystems.entryById(ClientPlanetState.getWarpEntryId());
            if (target == null)
                return;
        }

        boolean startHere = viewedSystem.getEntries().contains(current);
        boolean targetHere = target != null && viewedSystem.getEntries().contains(target);
        if (!startHere && !targetHere)
            return;

        int[] start = dockPosition(current, panelX, panelY, layout);
        if (!warping)
        {
            drawShip(graphics, start[0], start[1], 0.0D, 0.0D, 255);
            return;
        }

        int[] destination = dockPosition(target, panelX, panelY, layout);
        float progress = ClientPlanetState.warpProgress();
        double turnEnd = turnFraction();
        double travel = Math.min(1.0D,
                Math.max(0.0D, (progress - turnEnd) / (1.0D - turnEnd)));
        double ease = smoothstep(travel);

        double deltaX = destination[0] - start[0];
        double deltaY = destination[1] - start[1];
        double length = Math.hypot(deltaX, deltaY);
        double directionX = length < 1.0D ? 0.0D : deltaX / length;
        double directionY = length < 1.0D ? 0.0D : deltaY / length;
        double flightRotation = length < 1.0D
                ? 0.0D : Math.atan2(directionX, -directionY);
        boolean crossSystem = !startHere || !targetHere;

        double rotation;
        double speed;
        double positionX = start[0];
        double positionY = start[1];
        int alpha = 255;

        if (progress < turnEnd)
        {
            if (!startHere)
                return;
            rotation = smoothstep(progress / turnEnd) * flightRotation;
            speed = 0.0D;
        }
        else if (!crossSystem)
        {
            positionX = start[0] + deltaX * ease;
            positionY = start[1] + deltaY * ease;
            if (progress >= PlanetRenderer.ARRIVAL_FADE_START)
            {
                double amount = smoothstep((progress - PlanetRenderer.ARRIVAL_FADE_START)
                        / (1.0D - PlanetRenderer.ARRIVAL_FADE_START));
                rotation = flightRotation * (1.0D - amount);
                speed = 1.0D - amount;
            }
            else
            {
                rotation = flightRotation;
                speed = warpSpeed(travel);
            }
        }
        else
        {
            if (progress >= PlanetRenderer.ARRIVAL_FADE_START)
            {
                double amount = smoothstep((progress - PlanetRenderer.ARRIVAL_FADE_START)
                        / (1.0D - PlanetRenderer.ARRIVAL_FADE_START));
                rotation = flightRotation * (1.0D - amount);
                speed = 1.0D - amount;
            }
            else
            {
                rotation = flightRotation;
                speed = warpSpeed(travel);
            }

            // 0..1 departure, 1..1.5 deep space, 1.5..2 arrival.
            double half = ease * 2.0D;
            if (startHere && half < 1.0D)
            {
                int[] exit = rayExit(start, directionX, directionY, panelX, panelY, layout);
                double segment = Math.max(0.0D, Math.min(1.0D, half));
                positionX = start[0] + (exit[0] - start[0]) * segment;
                positionY = start[1] + (exit[1] - start[1]) * segment;
                alpha = (int) (255.0D
                        * (1.0D - smoothstep((segment - 0.7D) / 0.3D)));
            }
            else if (targetHere && half >= 1.5D)
            {
                int[] entry = rayExit(destination, -directionX, -directionY,
                        panelX, panelY, layout);
                double segment = Math.max(0.0D,
                        Math.min(1.0D, (half - 1.5D) / 0.5D));
                positionX = entry[0] + (destination[0] - entry[0]) * segment;
                positionY = entry[1] + (destination[1] - entry[1]) * segment;
                alpha = (int) (255.0D * smoothstep(segment / 0.3D));
            }
            else
            {
                return;
            }
        }
        drawShip(graphics, positionX, positionY, rotation, speed, alpha);
    }

    /** Renders the dashed route and ship arrow during the deep-space warp leg. */
    public void renderGalaxyWarp(GuiGraphics graphics, int panelX, int panelY,
                                 StarmapLayout layout)
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

        double half = warpHalf();
        if (half < 1.0D || half >= 1.5D)
            return;

        int[] fromBase = StarmapGeometry.galaxyPosition(from);
        int[] toBase = StarmapGeometry.galaxyPosition(to);
        int fromX = screenCanvasX(fromBase[0], panelX, layout);
        int fromY = screenCanvasY(fromBase[1], panelY, layout);
        int toX = screenCanvasX(toBase[0], panelX, layout);
        int toY = screenCanvasY(toBase[1], panelY, layout);
        drawDashedLine(graphics, fromX, fromY, toX, toY,
                StarmapVisualTheme.ROUTE_LINE);

        double segment = Math.max(0.0D, Math.min(1.0D, (half - 1.0D) / 0.5D));
        double centerX = fromX + (toX - fromX) * segment;
        double centerY = fromY + (toY - fromY) * segment;
        double rotation = Math.atan2(toX - fromX, -(toY - fromY));
        drawRouteShip(graphics, centerX, centerY, rotation);
    }

    /** Resolves the initial client state before the entry id has been synced. */
    private static PlanetEntry resolveCurrentEntry()
    {
        PlanetEntry current = StarSystems.entryById(ClientPlanetState.getCurrentEntryId());
        if (current != null)
            return current;
        Planet currentPlanet = ClientPlanetState.getCurrent();
        for (StarSystem system : StarSystems.all())
        {
            for (PlanetEntry entry : system.getEntries())
            {
                if (entry.getDestination() == currentPlanet)
                    return entry;
            }
        }
        return null;
    }

    private static int[] dockPosition(PlanetEntry entry, int panelX, int panelY,
                                      StarmapLayout layout)
    {
        int[] base = StarmapGeometry.bodyPosition(entry);
        int x = screenCanvasX(base[0], panelX, layout);
        int y = screenCanvasY(base[1], panelY, layout);
        return new int[] { x, y - entry.getMarkerSize() / 2 - 12 };
    }

    private static int screenCanvasX(int baseX, int panelX, StarmapLayout layout)
    {
        return panelX + layout.viewport().projectX(baseX);
    }

    private static int screenCanvasY(int baseY, int panelY, StarmapLayout layout)
    {
        return panelY + layout.viewport().projectY(baseY);
    }

    private static int[] rayExit(int[] from, double deltaX, double deltaY,
                                 int panelX, int panelY, StarmapLayout layout)
    {
        StarmapLayout.Bounds canvas = layout.canvas();
        double minimumX = panelX + canvas.x() - 4.0D;
        double minimumY = panelY + canvas.y() - 4.0D;
        double maximumX = minimumX + canvas.width() + 8.0D;
        double maximumY = minimumY + canvas.height() + 8.0D;
        double amountX = Math.abs(deltaX) < 1.0E-6D ? Double.POSITIVE_INFINITY
                : (deltaX > 0.0D ? maximumX - from[0] : minimumX - from[0]) / deltaX;
        double amountY = Math.abs(deltaY) < 1.0E-6D ? Double.POSITIVE_INFINITY
                : (deltaY > 0.0D ? maximumY - from[1] : minimumY - from[1]) / deltaY;
        double amount = Math.min(amountX, amountY);
        return new int[] {
                (int) (from[0] + deltaX * amount),
                (int) (from[1] + deltaY * amount)
        };
    }

    private static double warpHalf()
    {
        float progress = ClientPlanetState.warpProgress();
        double turnEnd = turnFraction();
        double travel = Math.min(1.0D,
                Math.max(0.0D, (progress - turnEnd) / (1.0D - turnEnd)));
        return warpEase(travel) * 2.0D;
    }

    private static double turnFraction()
    {
        return (double) ShipFlightController.TURN_TICKS
                / Math.max(1, ClientPlanetState.getWarpDurationTicks());
    }

    private static double decelerationStart()
    {
        return (PlanetRenderer.ARRIVAL_FADE_START - turnFraction())
                / (1.0D - turnFraction());
    }

    private static double warpEase(double amount)
    {
        double accelerationEnd = 0.10D;
        double decelerationStart = decelerationStart();
        double area = accelerationEnd / 2.0D
                + (decelerationStart - accelerationEnd)
                + (1.0D - decelerationStart) / 2.0D;
        if (amount < accelerationEnd)
            return (amount * amount / (2.0D * accelerationEnd)) / area;
        if (amount < decelerationStart)
            return (accelerationEnd / 2.0D + (amount - accelerationEnd)) / area;
        double deceleration = (amount - decelerationStart) / (1.0D - decelerationStart);
        return (accelerationEnd / 2.0D + (decelerationStart - accelerationEnd)
                + (1.0D - decelerationStart)
                * (deceleration - deceleration * deceleration / 2.0D)) / area;
    }

    private static double warpSpeed(double amount)
    {
        double accelerationEnd = 0.10D;
        double decelerationStart = decelerationStart();
        if (amount < accelerationEnd)
            return amount / accelerationEnd;
        if (amount < decelerationStart)
            return 1.0D;
        double deceleration = (amount - decelerationStart) / (1.0D - decelerationStart);
        return 1.0D - deceleration;
    }

    private static double smoothstep(double amount)
    {
        double clamped = Math.max(0.0D, Math.min(1.0D, amount));
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    public void drawRing(GuiGraphics graphics, int centerX, int centerY,
                         int radius, int color)
    {
        if (radius <= 0)
            return;
        StarmapUiDensity uiDensity = hiDpi.density();
        int density = uiDensity.factor();
        int size = StarmapOverlaySprites.logicalSize(radius) * density;
        int x = uiDensity.centeredOrigin(centerX, size);
        int y = uiDensity.centeredOrigin(centerY, size);
        try (StarmapHiDpiGraphics.DrawScope ignored = hiDpi.begin(graphics))
        {
            StarmapOverlaySprites.drawRingAt(graphics, x, y, size, color);
        }
    }

    /** Selection ring with four targeting marks centred on the same raster pixel. */
    public void drawSelectionReticle(GuiGraphics graphics, int centerX, int centerY,
                                     int radius, int color)
    {
        if (radius <= 0)
            return;
        StarmapUiDensity uiDensity = hiDpi.density();
        int density = uiDensity.factor();
        int size = StarmapOverlaySprites.logicalSize(radius) * density;
        int ringX = uiDensity.centeredOrigin(centerX, size);
        int ringY = uiDensity.centeredOrigin(centerY, size);
        double cx = uiDensity.virtualPixelCenter(centerX);
        double cy = uiDensity.virtualPixelCenter(centerY);
        int ringRadius = radius * density;
        int gap = density;
        int markLength = density * 2;
        int stroke = density;
        int verticalX = (int) Math.round(cx - stroke / 2.0D);
        int horizontalY = (int) Math.round(cy - stroke / 2.0D);
        int topEnd = (int) Math.floor(cy - ringRadius - gap);
        int bottomStart = (int) Math.ceil(cy + ringRadius + gap);
        int leftEnd = (int) Math.floor(cx - ringRadius - gap);
        int rightStart = (int) Math.ceil(cx + ringRadius + gap);
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            StarmapOverlaySprites.drawRingAt(graphics, ringX, ringY, size, color);
            draw.fillVirtual(verticalX, topEnd - markLength,
                    verticalX + stroke, topEnd, color);
            draw.fillVirtual(verticalX, bottomStart,
                    verticalX + stroke, bottomStart + markLength, color);
            draw.fillVirtual(leftEnd - markLength, horizontalY,
                    leftEnd, horizontalY + stroke, color);
            draw.fillVirtual(rightStart, horizontalY,
                    rightStart + markLength, horizontalY + stroke, color);
        }
    }

    public void drawDisk(GuiGraphics graphics, int centerX, int centerY,
                         int radius, int color)
    {
        if (radius <= 0)
            return;
        StarmapUiDensity uiDensity = hiDpi.density();
        int density = uiDensity.factor();
        int size = StarmapOverlaySprites.logicalSize(radius) * density;
        int x = uiDensity.centeredOrigin(centerX, size);
        int y = uiDensity.centeredOrigin(centerY, size);
        try (StarmapHiDpiGraphics.DrawScope ignored = hiDpi.begin(graphics))
        {
            StarmapOverlaySprites.drawDiskAt(graphics, x, y, size, color);
        }
    }

    /** One-virtual-pixel diagonal cross, matching the body's logical radius. */
    public void drawLockedCross(GuiGraphics graphics, int centerX, int centerY,
                                int radius, int color)
    {
        int density = hiDpi.density().factor();
        int cx = centerX * density;
        int cy = centerY * density;
        int r = Math.max(1, radius * density);
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            for (int i = -r; i <= r; i++)
            {
                draw.fillVirtual(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
                draw.fillVirtual(cx - i, cy + i, cx - i + 1, cy + i + 1, color);
            }
        }
    }

    /** Dashed route with a one-virtual-pixel stroke (4 logical px dash and gap). */
    public void drawDashedLine(GuiGraphics graphics, int x0, int y0,
                               int x1, int y1, int color)
    {
        int density = hiDpi.density().factor();
        double startX = x0 * (double) density;
        double startY = y0 * (double) density;
        double dx = (x1 - x0) * (double) density;
        double dy = (y1 - y0) * (double) density;
        double length = Math.hypot(dx, dy);
        if (length < 1.0D)
            return;
        double unitX = dx / length;
        double unitY = dy / length;
        double dash = 4.0D * density;
        double gap = 4.0D * density;
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            for (double distance = 0.0D; distance < length; distance += dash + gap)
            {
                double end = Math.min(distance + dash, length);
                for (double point = distance; point < end; point += 1.0D)
                {
                    int x = (int) Math.round(startX + unitX * point);
                    int y = (int) Math.round(startY + unitY * point);
                    draw.fillVirtual(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    public void drawShip(GuiGraphics graphics, double centerX, double centerY,
                         double rotation, double speed, int alpha)
    {
        int density = hiDpi.density().factor();
        double cx = Math.round(centerX * density);
        double cy = Math.round(centerY * density);
        double height = 9.0D * density;
        double width = 4.0D * density;
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            fillTriangle(draw, cx, cy, rotation, height, width,
                    (alpha << 24) | StarmapVisualTheme.SHIP_RGB);
            if (speed > 0.01D)
            {
                double flameLength = (2.0D + 4.0D * speed) * density;
                double directionX = Math.sin(rotation);
                double directionY = -Math.cos(rotation);
                fillTriangle(draw,
                        cx - directionX * (height / 2.0D + flameLength / 2.0D),
                        cy - directionY * (height / 2.0D + flameLength / 2.0D),
                        rotation + Math.PI, flameLength, width * 0.5D,
                        (alpha << 24) | StarmapVisualTheme.SHIP_FLAME_RGB);
            }
        }
    }

    /** Deep-space arrow and its white nose highlight, rendered as one unit. */
    public void drawRouteShip(GuiGraphics graphics, double centerX, double centerY,
                              double rotation)
    {
        int density = hiDpi.density().factor();
        double cx = centerX * density;
        double cy = centerY * density;
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            fillTriangle(draw, cx, cy, rotation,
                    6.0D * density, 3.0D * density, StarmapVisualTheme.ROUTE_SHIP);
            fillTriangle(draw,
                    cx + Math.sin(rotation) * 2.0D * density,
                    cy - Math.cos(rotation) * 2.0D * density,
                    rotation, 2.0D * density, 1.0D * density,
                    StarmapVisualTheme.ROUTE_SHIP_HIGHLIGHT);
        }
    }

    public void drawProgressBar(GuiGraphics graphics, int x, int y,
                                int width, int height, float progress,
                                int trackColor, int fillColor)
    {
        int density = hiDpi.density().factor();
        int left = x * density;
        int top = y * density;
        int right = (x + width) * density;
        int bottom = (y + height) * density;
        int fillWidth = Math.round(Math.max(0.0F, Math.min(1.0F, progress))
                * Math.max(0, right - left));
        try (StarmapHiDpiGraphics.DrawScope draw = hiDpi.begin(graphics))
        {
            draw.fillVirtual(left, top, right, bottom, trackColor);
            draw.fillVirtual(left, top, left + fillWidth, bottom, fillColor);
        }
    }

    /** Scanline fill of a rotated isosceles triangle in virtual coordinates. */
    private static void fillTriangle(StarmapHiDpiGraphics.DrawScope draw,
                                     double centerX, double centerY, double rotation,
                                     double height, double width, int color)
    {
        double directionX = Math.sin(rotation);
        double directionY = -Math.cos(rotation);
        double perpendicularX = Math.cos(rotation);
        double perpendicularY = Math.sin(rotation);
        double tipX = centerX + directionX * height / 2.0D;
        double tipY = centerY + directionY * height / 2.0D;
        double baseX = centerX - directionX * height / 2.0D;
        double baseY = centerY - directionY * height / 2.0D;
        double leftX = baseX + perpendicularX * width;
        double leftY = baseY + perpendicularY * width;
        double rightX = baseX - perpendicularX * width;
        double rightY = baseY - perpendicularY * width;

        int minimumY = (int) Math.floor(Math.min(tipY, Math.min(leftY, rightY)));
        int maximumY = (int) Math.ceil(Math.max(tipY, Math.max(leftY, rightY)));
        double[][] edges = {
                { tipX, tipY, leftX, leftY },
                { leftX, leftY, rightX, rightY },
                { rightX, rightY, tipX, tipY }
        };
        for (int y = minimumY; y <= maximumY; y++)
        {
            double sampleY = y + 0.5D;
            double minimumX = Double.MAX_VALUE;
            double maximumX = -Double.MAX_VALUE;
            for (double[] edge : edges)
            {
                if ((edge[1] <= sampleY && sampleY < edge[3])
                        || (edge[3] <= sampleY && sampleY < edge[1]))
                {
                    double amount = (sampleY - edge[1]) / (edge[3] - edge[1]);
                    double intersectionX = edge[0] + amount * (edge[2] - edge[0]);
                    minimumX = Math.min(minimumX, intersectionX);
                    maximumX = Math.max(maximumX, intersectionX);
                }
            }
            if (maximumX >= minimumX)
            {
                draw.fillVirtual((int) Math.floor(minimumX), y,
                        (int) Math.ceil(maximumX) + 1, y + 1, color);
            }
        }
    }
}
