package com.starboundmc.client;

import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapFocusGeometryTest
{
    @Test
    void focusTargetIsCenteredAndOnlyDirectMoonsAreIncluded()
    {
        StarSystem system = StarSystems.byId(StarSystems.SYS_MAIN);
        PlanetEntry lush = StarSystems.entryById("sys1:lush");
        List<StarmapFocusGeometry.Placement> placements =
                StarmapFocusGeometry.placements(system, lush);

        assertEquals(2, placements.size());
        StarmapFocusGeometry.Placement target = placements.get(0);
        assertEquals(lush, target.entry());
        assertEquals(StarmapGeometry.BASE_WIDTH / 2, target.x());
        assertEquals(StarmapGeometry.BASE_HEIGHT / 2, target.y());
        assertEquals(StarmapFocusGeometry.TARGET_DIAMETER, target.diameter());
        assertEquals("sys1:molten", placements.get(1).entry().getEntryId());
        assertEquals(10, placements.get(1).diameter());
        assertTrue(placements.get(1).orbitRadius() > target.diameter() / 2);
    }

    @Test
    void bodyWithoutMoonsOnlyProducesTheFocusTarget()
    {
        StarSystem system = StarSystems.byId(StarSystems.SYS_COLD);
        PlanetEntry frozen = StarSystems.entryById("sys2:frozen");

        assertEquals(1, StarmapFocusGeometry.placements(system, frozen).size());
    }
}
