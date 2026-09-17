package com.starboundmc.client;

import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.ClientUniverseCatalog;
import com.starboundmc.world.universe.StarSystemDefinition;
import com.starboundmc.world.universe.UniverseCatalog;

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
    public static int[] galaxyPosition(StarSystemDefinition system)
    {
        return new int[] {
                system.galaxyMapPosition().pixelX(BASE_WIDTH),
                system.galaxyMapPosition().pixelY(BASE_HEIGHT)
        };
    }

    /** Body position in the fixed authoring canvas, including recursive moon offsets. */
    public static int[] bodyPosition(CelestialBodyDefinition entry)
    {
        return bodyPosition(entry, ClientUniverseCatalog.current());
    }

    /**
     * Body position resolved against an explicit universe.
     *
     * <p>The parent is looked up in the catalog rather than the legacy registry,
     * so a moon whose parent belongs to a datapack-added system still resolves. A
     * body with no parent, or a parent that is not in this universe, falls back
     * to the system centre exactly as before.</p>
     */
    public static int[] bodyPosition(CelestialBodyDefinition entry, UniverseCatalog catalog)
    {
        double radians = Math.toRadians(entry.orbit().orbitAngle());
        int dx = (int) (Math.cos(radians) * entry.orbit().orbitRadius());
        int dy = (int) (Math.sin(radians) * entry.orbit().orbitRadius());
        String parentId = entry.parentEntryId().orElse(null);
        if (parentId != null)
        {
            CelestialBodyDefinition parent = catalog == null
                    ? null : catalog.body(parentId).orElse(null);
            if (parent != null)
            {
                int[] parentPosition = bodyPosition(parent, catalog);
                return new int[] { parentPosition[0] + dx, parentPosition[1] + dy };
            }
        }
        return new int[] { BASE_WIDTH / 2 + dx, BASE_HEIGHT / 2 + dy };
    }

    /** View-specific marker diameter; overview moons are intentionally subordinate thumbnails. */
    public static int overviewDiameter(CelestialBodyDefinition entry)
    {
        int markerSize = entry.starmapVisual().getMarkerSize();
        return entry.orbit().isMoon()
                ? Math.max(6, Math.round(markerSize * 0.65F))
                : markerSize;
    }

    /** Parent position used as the centre of a moon's local orbit. */
    public static int[] moonOrbitCenter(CelestialBodyDefinition moon)
    {
        if (moon == null || moon.parentEntryId().isEmpty())
            return null;
        CelestialBodyDefinition parent = ClientUniverseCatalog.current()
                .body(moon.parentEntryId().orElseThrow()).orElse(null);
        return parent == null ? null : bodyPosition(parent);
    }
}
