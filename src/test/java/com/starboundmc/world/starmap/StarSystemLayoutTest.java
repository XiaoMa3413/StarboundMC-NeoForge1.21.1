package com.starboundmc.world.starmap;

import com.starboundmc.warp.ShipSpace;
import net.minecraft.world.phys.Vec3;
import com.starboundmc.world.universe.BuiltInUniverse;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.UniverseTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.StarSystemDefinition;
class StarSystemLayoutTest
{
    /** Current resolver fade reaches zero at 0.72 + 0.55 influence radii. */
    private static final double VISUAL_OUTER_RATIO = 1.27;

    @Test
    void systemCentresLeaveADeepSpaceVisualGap()
    {
        StarSystemDefinition main = UniverseTestSupport.system(BuiltInUniverse.MAIN_SYSTEM_ID);
        StarSystemDefinition cold = UniverseTestSupport.system(BuiltInUniverse.COLD_SYSTEM_ID);
        double centreDistance = main.navigationCenter().toLocalVec3().distanceTo(cold.navigationCenter().toLocalVec3());
        double visualOuterSum = (main.influenceRadius() + cold.influenceRadius())
                * VISUAL_OUTER_RATIO;

        assertTrue(centreDistance >= 39_000.0 && centreDistance <= 41_000.0);
        assertTrue(centreDistance > visualOuterSum,
                "stellar visual influence shells must not overlap");
        assertTrue(centreDistance - visualOuterSum >= 27_000.0,
                "both systems need a substantial neutral deep-space interval");
    }

    @Test
    void coldSystemReadsAsASeparateDistanceLayerFromTheStarterOrbit()
    {
        Vec3 starterDock = UniverseNavigation.vDock("sys1:lush");
        double localStarDistance = UniverseTestSupport.system(BuiltInUniverse.MAIN_SYSTEM_ID)
                .stellarVisual().getVirtualPosition().distanceTo(starterDock);
        double coldStarDistance = UniverseTestSupport.system(BuiltInUniverse.COLD_SYSTEM_ID)
                .stellarVisual().getVirtualPosition().distanceTo(starterDock);

        assertTrue(coldStarDistance >= localStarDistance * 1.9,
                "the remote red dwarf must sit visibly behind the local star");
        assertTrue(coldStarDistance <= localStarDistance * 2.2,
                "the authored second system should remain readable from the starter region");
    }

    @Test
    void galaxyMapNodesUseTheExpandedComposition()
    {
        GalaxyMapPosition main = UniverseTestSupport.system(BuiltInUniverse.MAIN_SYSTEM_ID).galaxyMapPosition();
        GalaxyMapPosition cold = UniverseTestSupport.system(BuiltInUniverse.COLD_SYSTEM_ID).galaxyMapPosition();
        int mainX = main.pixelX(250);
        int mainY = main.pixelY(220);
        int coldX = cold.pixelX(250);
        int coldY = cold.pixelY(220);
        double separation = Math.hypot(coldX - mainX, coldY - mainY);

        assertEquals(62, mainX);
        assertEquals(84, mainY);
        assertEquals(208, coldX);
        assertEquals(152, coldY);
        assertTrue(separation >= 160.0,
                "the deep-space overview should not visually crowd both systems together");
    }

    @Test
    void frozenDockAndRedDwarfMoveWithTheColdSystem()
    {
        StarSystemDefinition cold = UniverseTestSupport.system(BuiltInUniverse.COLD_SYSTEM_ID);
        Vec3 centre = cold.navigationCenter().toLocalVec3();
        Vec3 star = cold.stellarVisual().getVirtualPosition();

        assertEquals(centre, UniverseNavigation.vDock("sys2:frozen"));
        assertEquals(new Vec3(6000.0, 6898.0, 11000.0), star.subtract(centre));
        assertEquals(cold.stellarVisual().getDistanceResponse().referenceDistance(),
                star.distanceTo(UniverseNavigation.vDock("sys2:frozen")), 0.1);
    }

    @Test
    void distantStarsConvergeOnTheBatchPointCoreRadius()
    {
        assertEquals(1.45F, UniverseTestSupport.system(BuiltInUniverse.MAIN_SYSTEM_ID).stellarVisual()
                .getDistanceResponse().remotePointRadius(), 1.0E-6F);
        assertEquals(1.45F, UniverseTestSupport.system(BuiltInUniverse.COLD_SYSTEM_ID).stellarVisual()
                .getDistanceResponse().remotePointRadius(), 1.0E-6F);
    }

    @Test
    void countsOnlyDirectSatellitesOfAPlanet()
    {
        StarSystemDefinition main = UniverseTestSupport.system(BuiltInUniverse.MAIN_SYSTEM_ID);
        CelestialBodyDefinition lush = UniverseTestSupport.body("sys1:lush");
        CelestialBodyDefinition gasGiant = UniverseTestSupport.body("sys1:gasgiant");
        CelestialBodyDefinition barren = UniverseTestSupport.body("sys1:barren");
        CelestialBodyDefinition moon = UniverseTestSupport.body("sys1:molten");

        assertEquals(1, main.moonCount(lush.entryId()));
        assertEquals(1, main.moonCount(gasGiant.entryId()));
        assertEquals(0, main.moonCount(barren.entryId()));
        assertEquals(0, main.moonCount(moon.entryId()));
        assertEquals(0, main.moonCount(null));
    }
}
