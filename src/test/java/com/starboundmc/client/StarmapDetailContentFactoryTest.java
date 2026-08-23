package com.starboundmc.client;

import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
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
        StarSystem system = StarSystems.byId(StarSystems.SYS_MAIN);

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
        StarSystem system = StarSystems.byId(StarSystems.SYS_MAIN);
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
        PlanetEntry entry = StarSystems.entryById("sys1:lush");

        StarmapDetailContent content = StarmapDetailContentFactory.buildEntry(
                entry, entry.getEntryId(), true, ShipWarpManager.WARP_FUEL_COST);

        assertEquals(List.of("scan", "navigation", "status"), sectionIds(content));
        assertEquals(StarmapDetailLine.Tone.FUEL,
                content.sections().get(1).lines().get(0).tone());
        assertEquals(StarmapDetailLine.Tone.CURRENT,
                content.sections().get(2).lines().get(0).tone());
    }

    @Test
    void lockedEntryHasNoEmptyNavigationCategory()
    {
        PlanetEntry entry = StarSystems.entryById("sys1:gasgiant");

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
        PlanetEntry entry = StarSystems.entryById("sys1:barren");

        StarmapDetailContent content = StarmapDetailContentFactory.buildEntry(
                entry, "sys1:lush", false, ShipWarpManager.WARP_FUEL_COST);

        assertEquals(List.of("scan", "navigation"), sectionIds(content));
    }

    private static List<String> sectionIds(StarmapDetailContent content)
    {
        return content.sections().stream().map(StarmapDetailSection::id).toList();
    }
}
