package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CelestialLodTransitionsTest
{
    @Test
    void crossfadesAdjacentBodyTiersWithoutAJump()
    {
        CelestialLodTransitions transitions = new CelestialLodTransitions(2);
        assertEquals(1.0F, transitions.update("lush", CelestialLod.POINT, 0.0F), 1.0E-6F);
        float halfway = transitions.update("lush", CelestialLod.REDUCED, 5.0F);

        assertEquals(1.5F, halfway, 1.0E-6F);
        assertEquals(0.50F, CelestialLodTransitions.pointWeight(halfway), 1.0E-6F);
        assertEquals(0.50F, CelestialLodTransitions.reducedWeight(halfway), 1.0E-6F);
        assertEquals(2.0F,
                transitions.update("lush", CelestialLod.REDUCED, 10.0F), 1.0E-6F);
    }

    @Test
    void weightsCoverTheFullBodyToPointRange()
    {
        for (int i = 20; i <= 60; i++)
        {
            float detail = i / 20.0F;
            float total = CelestialLodTransitions.pointWeight(detail)
                    + CelestialLodTransitions.reducedWeight(detail)
                    + CelestialLodTransitions.fullWeight(detail);
            assertEquals(1.0F, total, 1.0E-6F);
        }
    }

    @Test
    void aStaleBodyIsReinitializedAtItsNewTier()
    {
        CelestialLodTransitions transitions = new CelestialLodTransitions(2);
        transitions.update("frozen", CelestialLod.FULL, 0.0F);
        assertEquals(1.0F,
                transitions.update("frozen", CelestialLod.POINT, 100.0F), 1.0E-6F);
        assertEquals(CelestialLod.POINT, transitions.currentLod("frozen"));
    }
}
