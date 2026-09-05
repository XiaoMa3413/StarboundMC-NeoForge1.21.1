package com.starboundmc.world.starmap;

import com.starboundmc.warp.ShipSpace;
import com.starboundmc.world.Planet;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarSystemLayoutTest
{
    /** Current resolver fade reaches zero at 0.72 + 0.55 influence radii. */
    private static final double VISUAL_OUTER_RATIO = 1.27;

    @Test
    void systemCentresLeaveADeepSpaceVisualGap()
    {
        StarSystem main = StarSystems.byId(StarSystems.SYS_MAIN);
        StarSystem cold = StarSystems.byId(StarSystems.SYS_COLD);
        double centreDistance = main.getNavigationCenter().distanceTo(cold.getNavigationCenter());
        double visualOuterSum = (main.getInfluenceRadius() + cold.getInfluenceRadius())
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
        Vec3 starterDock = ShipSpace.vDock(Planet.LUSH);
        double localStarDistance = StarSystems.byId(StarSystems.SYS_MAIN)
                .getStellarVisual().getVirtualPosition().distanceTo(starterDock);
        double coldStarDistance = StarSystems.byId(StarSystems.SYS_COLD)
                .getStellarVisual().getVirtualPosition().distanceTo(starterDock);

        assertTrue(coldStarDistance >= localStarDistance * 1.9,
                "the remote red dwarf must sit visibly behind the local star");
        assertTrue(coldStarDistance <= localStarDistance * 2.2,
                "the authored second system should remain readable from the starter region");
    }

    @Test
    void galaxyMapNodesUseTheExpandedComposition()
    {
        GalaxyMapPosition main = StarSystems.byId(StarSystems.SYS_MAIN).getGalaxyMapPosition();
        GalaxyMapPosition cold = StarSystems.byId(StarSystems.SYS_COLD).getGalaxyMapPosition();
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
        StarSystem cold = StarSystems.byId(StarSystems.SYS_COLD);
        Vec3 centre = cold.getNavigationCenter();
        Vec3 star = cold.getStellarVisual().getVirtualPosition();

        assertEquals(centre, ShipSpace.vDock(Planet.FROZEN));
        assertEquals(new Vec3(6000.0, 6898.0, 11000.0), star.subtract(centre));
        assertEquals(cold.getStellarVisual().getDistanceResponse().referenceDistance(),
                star.distanceTo(ShipSpace.vDock(Planet.FROZEN)), 0.1);
    }

    @Test
    void distantStarsConvergeOnTheBatchPointCoreRadius()
    {
        assertEquals(1.45F, StarSystems.byId(StarSystems.SYS_MAIN).getStellarVisual()
                .getDistanceResponse().remotePointRadius(), 1.0E-6F);
        assertEquals(1.45F, StarSystems.byId(StarSystems.SYS_COLD).getStellarVisual()
                .getDistanceResponse().remotePointRadius(), 1.0E-6F);
    }

    @Test
    void countsOnlyDirectSatellitesOfAPlanet()
    {
        StarSystem main = StarSystems.byId(StarSystems.SYS_MAIN);
        PlanetEntry lush = StarSystems.entryById("sys1:lush");
        PlanetEntry gasGiant = StarSystems.entryById("sys1:gasgiant");
        PlanetEntry barren = StarSystems.entryById("sys1:barren");
        PlanetEntry moon = StarSystems.entryById("sys1:molten");

        assertEquals(1, main.getMoonCount(lush));
        assertEquals(1, main.getMoonCount(gasGiant));
        assertEquals(0, main.getMoonCount(barren));
        assertEquals(0, main.getMoonCount(moon));
        assertEquals(0, main.getMoonCount(null));
    }
}
