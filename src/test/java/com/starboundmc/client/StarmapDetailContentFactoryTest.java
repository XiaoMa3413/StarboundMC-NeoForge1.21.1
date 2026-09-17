package com.starboundmc.client;

import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.StarSystemDefinition;
import com.starboundmc.world.universe.UniverseTestSupport;
import com.starboundmc.world.universe.BuiltInUniverse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class StarmapDetailContentFactoryTest
{
    @Test
    void galaxyOnlyAddsStatusWhenShipIsInThatSystem()
    {
        StarSystemDefinition system = UniverseTestSupport.system(BuiltInUniverse.MAIN_SYSTEM_ID);

        StarmapDetailContent remote = StarmapDetailContentFactory.buildGalaxy(system, false);
        StarmapDetailContent docked = StarmapDetailContentFactory.buildGalaxy(system, true);

        assertEquals(List.of("system"), sectionIds(remote));
        assertEquals(List.of("system", "status"), sectionIds(docked));
        assertEquals(StarmapDetailLine.Tone.CURRENT,
                docked.sections().get(1).lines().get(0).tone());
    }

    @Test
    void starUsesOnlyItsRealScanCategory()
    {
        StarSystemDefinition system = UniverseTestSupport.system(BuiltInUniverse.MAIN_SYSTEM_ID);
        StarmapDetailContent content = StarmapDetailContentFactory.buildStar(system);

        assertEquals(List.of("scan"), sectionIds(content));
        assertEquals(StarmapDetailLine.Tone.ATTENTION,
                content.sections().get(0).lines().get(0).tone());

        StarmapDetailContentFactory factory = new StarmapDetailContentFactory();
        assertSame(factory.star(system), factory.star(system));
    }

    @Test
    void reachableEntryContainsNavigationAndPrioritizesCurrentStatus()
    {
        CelestialBodyDefinition entry = UniverseTestSupport.body("sys1:lush");

        StarmapDetailContent content = StarmapDetailContentFactory.buildEntry(
                entry, entry.entryId(), true, ShipWarpManager.WARP_FUEL_COST);

        assertEquals(List.of("scan", "navigation", "status"), sectionIds(content));
        assertEquals(StarmapDetailLine.Tone.FUEL,
                content.sections().get(1).lines().get(0).tone());
        assertEquals(StarmapDetailLine.Tone.CURRENT,
                content.sections().get(2).lines().get(0).tone());
    }

    @Test
    void lockedEntryHasNoEmptyNavigationCategory()
    {
        CelestialBodyDefinition entry = UniverseTestSupport.body("sys1:gasgiant");

        StarmapDetailContent content = StarmapDetailContentFactory.buildEntry(
                entry, "sys1:lush", false, 0);

        assertEquals(List.of("scan", "status"), sectionIds(content));
        assertFalse(content.sections().stream()
                .anyMatch(section -> section.id().equals("navigation")));
        assertEquals(StarmapDetailLine.Tone.DANGER,
                content.sections().get(1).lines().get(0).tone());
    }

    @Test
    void unvisitedReachableEntryDoesNotCreateAnEmptyStatusCategory()
    {
        CelestialBodyDefinition entry = UniverseTestSupport.body("sys1:barren");

        StarmapDetailContent content = StarmapDetailContentFactory.buildEntry(
                entry, "sys1:lush", false, ShipWarpManager.WARP_FUEL_COST);

        assertEquals(List.of("scan", "navigation"), sectionIds(content));
    }

    private static List<String> sectionIds(StarmapDetailContent content)
    {
        return content.sections().stream().map(StarmapDetailSection::id).toList();
    }
}
