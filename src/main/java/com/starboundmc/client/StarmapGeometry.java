package com.starboundmc.client;

import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;

/** Shared base-space geometry used by both cached scenery and live overlays. */
public final class StarmapGeometry
{
    public static final int BASE_WIDTH = 250;
    public static final int BASE_HEIGHT = 220;

    private StarmapGeometry()
    {
    }

    /** Projects a base-pixel center onto an integer destination-pixel center. */
    public static int projectPixelCenter(int baseCoordinate, int destinationSize, int baseSize)
    {
        double normalizedCenter = (baseCoordinate + 0.5D) / baseSize;
        return (int) Math.round(normalizedCenter * destinationSize - 0.5D);
    }

    /** System position in the fixed authoring canvas. */
    public static int[] galaxyPosition(StarSystem system)
    {
        return new int[] {
                system.getGalaxyMapPosition().pixelX(BASE_WIDTH),
                system.getGalaxyMapPosition().pixelY(BASE_HEIGHT)
        };
    }

    /** Body position in the fixed authoring canvas, including recursive moon offsets. */
    public static int[] bodyPosition(PlanetEntry entry)
    {
        double radians = Math.toRadians(entry.getOrbitAngle());
        int dx = (int) (Math.cos(radians) * entry.getOrbitRadius());
        int dy = (int) (Math.sin(radians) * entry.getOrbitRadius());
        if (entry.getParentEntryId() != null)
        {
            PlanetEntry parent = StarSystems.entryById(entry.getParentEntryId());
            if (parent != null)
            {
                int[] parentPosition = bodyPosition(parent);
                return new int[] { parentPosition[0] + dx, parentPosition[1] + dy };
            }
        }
        return new int[] { BASE_WIDTH / 2 + dx, BASE_HEIGHT / 2 + dy };
    }

    /** View-specific marker diameter; overview moons are intentionally subordinate thumbnails. */
    public static int overviewDiameter(PlanetEntry entry)
    {
        return entry.isMoon()
                ? Math.max(6, Math.round(entry.getMarkerSize() * 0.65F))
                : entry.getMarkerSize();
    }

    /** Parent position used as the centre of a moon's local orbit. */
    public static int[] moonOrbitCenter(PlanetEntry moon)
    {
        if (moon == null || moon.getParentEntryId() == null)
            return null;
        PlanetEntry parent = StarSystems.entryById(moon.getParentEntryId());
        return parent == null ? null : bodyPosition(parent);
    }
}
