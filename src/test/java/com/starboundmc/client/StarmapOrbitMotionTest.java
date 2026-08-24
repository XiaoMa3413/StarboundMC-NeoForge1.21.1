package com.starboundmc.client;

import com.starboundmc.client.starmap.StarmapOrbitMotion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapOrbitMotionTest
{
    @Test
    void keepsTheNewBaselineBelowTheFormerUniformSpeed()
    {
        assertEquals(0.0035F, StarmapOrbitMotion.speedForRadius(52.0F), 0.000001F);
        assertTrue(StarmapOrbitMotion.speedForRadius(52.0F) < 0.0125F);
    }

    @Test
    void slowsOuterOrbitsAndKeepsMoonsSlightlyFaster()
    {
        float inner = StarmapOrbitMotion.speedForRadius(22.0F);
        float outer = StarmapOrbitMotion.speedForRadius(116.0F);
        float moon = StarmapOrbitMotion.moonPhase(1.0F, 20.0F);

        assertTrue(outer < inner);
        assertTrue(moon > StarmapOrbitMotion.phase(1.0F, 20.0F));
        assertTrue(outer > 0.0F);
    }
}
