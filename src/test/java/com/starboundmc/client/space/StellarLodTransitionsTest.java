package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StellarLodTransitionsTest
{
    @Test
    void crossfadesEachAdjacentLevelOverEighteenTicks()
    {
        StellarLodTransitions transitions = new StellarLodTransitions(4);
        assertEquals(0.0F, transitions.update("sys", StellarLod.POINT, 0.0F), 1.0E-6F);
        assertEquals(0.0F, transitions.update("sys", StellarLod.SIMPLIFIED, 0.0F), 1.0E-6F);

        float halfway = transitions.update("sys", StellarLod.SIMPLIFIED, 9.0F);
        assertEquals(0.50F, halfway, 1.0E-6F);
        assertEquals(0.50F, StellarLodTransitions.pointWeight(halfway), 1.0E-6F);
        assertEquals(0.50F, StellarLodTransitions.simplifiedWeight(halfway), 1.0E-6F);

        assertEquals(1.0F, transitions.update("sys", StellarLod.SIMPLIFIED, 18.0F), 1.0E-6F);
        float fullHalfway = transitions.update("sys", StellarLod.FULL, 27.0F);
        assertEquals(1.50F, fullHalfway, 1.0E-6F);
        assertEquals(0.50F, StellarLodTransitions.simplifiedWeight(fullHalfway), 1.0E-6F);
        assertEquals(0.50F, StellarLodTransitions.fullWeight(fullHalfway), 1.0E-6F);
        assertEquals(2.0F, transitions.update("sys", StellarLod.FULL, 36.0F), 1.0E-6F);
    }

    @Test
    void reversesDirectionWithoutAVisibleWeightJump()
    {
        StellarLodTransitions transitions = new StellarLodTransitions(4);
        transitions.update("sys", StellarLod.POINT, 0.0F);
        float outward = transitions.update("sys", StellarLod.SIMPLIFIED, 6.0F);
        float sameFrame = transitions.update("sys", StellarLod.POINT, 6.0F);

        assertEquals(outward, sameFrame, 1.0E-6F);
        assertEquals(0.0F, transitions.update("sys", StellarLod.POINT, 12.0F), 1.0E-6F);
    }

    @Test
    void crossfadeWeightsPreserveTotalBrightness()
    {
        for (int i = 0; i <= 40; i++)
        {
            float detail = i / 20.0F;
            float total = StellarLodTransitions.pointWeight(detail)
                    + StellarLodTransitions.simplifiedWeight(detail)
                    + StellarLodTransitions.fullWeight(detail);
            assertEquals(1.0F, total, 1.0E-6F);
        }
    }
}
