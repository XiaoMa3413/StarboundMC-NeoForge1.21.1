package com.starboundmc.client;

import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.StarSystemDefinition;
import com.starboundmc.world.universe.UniverseTestSupport;
import com.starboundmc.world.universe.BuiltInUniverse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapFocusGeometryTest
{
    @Test
    void focusTargetIsCenteredAndOnlyDirectMoonsAreIncluded()
    {
        StarSystemDefinition system = UniverseTestSupport.system(BuiltInUniverse.MAIN_SYSTEM_ID);
        CelestialBodyDefinition lush = UniverseTestSupport.body("sys1:lush");
        List<StarmapFocusGeometry.Placement> placements =
                StarmapFocusGeometry.placements(system, lush);

        assertEquals(2, placements.size());
        StarmapFocusGeometry.Placement target = placements.get(0);
        assertEquals(lush, target.entry());
        assertEquals(StarmapGeometry.BASE_WIDTH / 2, target.x());
        assertEquals(StarmapGeometry.BASE_HEIGHT / 2, target.y());
        assertEquals(StarmapFocusGeometry.TARGET_DIAMETER, target.diameter());
        assertEquals("sys1:molten", placements.get(1).entry().entryId());
        assertEquals(10, placements.get(1).diameter());
        assertTrue(placements.get(1).orbitRadius() > target.diameter() / 2);
    }

    @Test
    void bodyWithoutMoonsOnlyProducesTheFocusTarget()
    {
        StarSystemDefinition system = UniverseTestSupport.system(BuiltInUniverse.COLD_SYSTEM_ID);
        CelestialBodyDefinition frozen = UniverseTestSupport.body("sys2:frozen");

        assertEquals(1, StarmapFocusGeometry.placements(system, frozen).size());
    }
}
