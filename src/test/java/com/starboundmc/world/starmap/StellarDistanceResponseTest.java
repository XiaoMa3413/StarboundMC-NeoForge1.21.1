package com.starboundmc.world.starmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StellarDistanceResponseTest
{
    private static final StellarDistanceResponse RESPONSE = new StellarDistanceResponse(
            1_000.0, 10.0F, 0.60F, 2.20F, 1.80F,
            1.20F, 0.20F, 0.50F);

    @Test
    void referenceDistancePreservesBaseRadius()
    {
        assertEquals(1.0F, RESPONSE.distanceScale(1_000.0), 1.0E-6F);
        assertEquals(10.0F, RESPONSE.localSkyRadius(1_000.0), 1.0E-6F);
        assertEquals(10.0F, RESPONSE.skyRadius(1_000.0, 1.0F), 1.0E-6F);
    }

    @Test
    void remotePointAndLocalDiscBlendContinuously()
    {
        assertEquals(1.20F, RESPONSE.skyRadius(1_000.0, 0.0F), 1.0E-6F);
        assertEquals(5.60F, RESPONSE.skyRadius(1_000.0, 0.5F), 1.0E-6F);
        assertEquals(10.0F, RESPONSE.skyRadius(1_000.0, 1.0F), 1.0E-6F);
    }

    @Test
    void artisticExponentAmplifiesMovementAndClampsExtremes()
    {
        float tenPercentCloser = RESPONSE.distanceScale(900.0);
        assertTrue(tenPercentCloser > 1.19F);
        assertEquals(2.20F, RESPONSE.distanceScale(1.0), 1.0E-6F);
        assertEquals(0.60F, RESPONSE.distanceScale(1_000_000.0), 1.0E-6F);
    }

    @Test
    void effectWeightsHaveIndependentActivationThresholds()
    {
        assertEquals(0.0F, RESPONSE.coronaWeight(0.20F), 1.0E-6F);
        assertTrue(RESPONSE.coronaWeight(0.50F) > 0.0F);
        assertEquals(0.0F, RESPONSE.effectWeight(0.50F), 1.0E-6F);
        assertTrue(RESPONSE.effectWeight(0.75F) > 0.0F);
        assertEquals(1.0F, RESPONSE.effectWeight(1.0F), 1.0E-6F);
    }
}
