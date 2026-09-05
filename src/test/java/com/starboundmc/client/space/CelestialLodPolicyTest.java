package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CelestialLodPolicyTest
{
    @Test
    void classifiesByAngularSizeRatherThanRawDistance()
    {
        assertEquals(CelestialLod.FULL, CelestialLodPolicy.classify(1.0, 30.0));
        assertEquals(CelestialLod.REDUCED, CelestialLodPolicy.classify(1.0, 100.0));
        assertEquals(CelestialLod.POINT, CelestialLodPolicy.classify(1.0, 500.0));
        assertEquals(CelestialLod.CULLED, CelestialLodPolicy.classify(1.0, 2_000.0));
    }

    @Test
    void largerBodiesRemainVisibleAtTheSameDistance()
    {
        CelestialLod large = CelestialLodPolicy.classify(10.0, 100.0);
        CelestialLod small = CelestialLodPolicy.classify(1.0, 100.0);

        assertTrue(large.atLeast(small));
        assertEquals(CelestialLod.FULL, large);
        assertEquals(CelestialLod.REDUCED, small);
    }

    @Test
    void hysteresisKeepsABodyInItsTierUntilTheExitThreshold()
    {
        assertEquals(CelestialLod.FULL,
                CelestialLodPolicy.hysteretic(2.30, CelestialLod.FULL, CelestialLod.CULLED));
        assertEquals(CelestialLod.REDUCED,
                CelestialLodPolicy.hysteretic(2.00, CelestialLod.FULL, CelestialLod.CULLED));
        assertEquals(CelestialLod.POINT,
                CelestialLodPolicy.hysteretic(0.40, CelestialLod.REDUCED, CelestialLod.CULLED));
        assertEquals(CelestialLod.POINT,
                CelestialLodPolicy.hysteretic(0.07, CelestialLod.POINT, CelestialLod.CULLED));
    }

    @Test
    void routePriorityPromotesAOtherwiseCulledBodyToPoint()
    {
        assertEquals(CelestialLod.POINT,
                CelestialLodPolicy.hysteretic(0.01, CelestialLod.CULLED, CelestialLod.POINT));
        assertEquals(CelestialLod.FULL,
                CelestialLodPolicy.hysteretic(8.0, CelestialLod.CULLED, CelestialLod.POINT));
    }
}
