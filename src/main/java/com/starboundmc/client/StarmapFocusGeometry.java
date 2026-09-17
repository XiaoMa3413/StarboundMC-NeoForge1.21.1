package com.starboundmc.client;

import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.StarSystemDefinition;

import java.util.ArrayList;
import java.util.List;

/** Fixed authoring-space composition for the planet-focus page. */
public final class StarmapFocusGeometry
{
    public static final int TARGET_DIAMETER = 72;
    private static final int FIRST_MOON_ORBIT = 54;
    private static final int MOON_ORBIT_STEP = 16;

    private StarmapFocusGeometry()
    {
    }

    public static List<Placement> placements(StarSystemDefinition system, CelestialBodyDefinition focus)
    {
        if (system == null || focus == null || !system.bodies().contains(focus))
            return List.of();

        List<Placement> result = new ArrayList<>();
        result.add(new Placement(focus, StarmapGeometry.BASE_WIDTH / 2,
                StarmapGeometry.BASE_HEIGHT / 2, TARGET_DIAMETER, 0));
        int moonIndex = 0;
        for (CelestialBodyDefinition candidate : system.bodies())
        {
            if (!focus.entryId().equals(candidate.parentEntryId().orElse(null)))
                continue;
            int orbitRadius = FIRST_MOON_ORBIT + moonIndex * MOON_ORBIT_STEP;
            double radians = Math.toRadians(candidate.orbit().orbitAngle());
            int x = StarmapGeometry.BASE_WIDTH / 2
                    + (int) (Math.cos(radians) * orbitRadius);
            int y = StarmapGeometry.BASE_HEIGHT / 2
                    + (int) (Math.sin(radians) * orbitRadius);
            int diameter = Math.max(8, Math.min(12,
                    Math.round(candidate.starmapVisual().getMarkerSize() * 0.8F)));
            result.add(new Placement(candidate, x, y, diameter, orbitRadius));
            moonIndex++;
        }
        return List.copyOf(result);
    }

    public record Placement(CelestialBodyDefinition entry, int x, int y, int diameter, int orbitRadius) {}
}
