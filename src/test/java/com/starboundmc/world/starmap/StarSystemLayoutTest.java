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

        assertTrue(centreDistance >= 14_000.0 && centreDistance <= 16_000.0);
        assertTrue(centreDistance > visualOuterSum,
                "stellar visual influence shells must not overlap");
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
}
