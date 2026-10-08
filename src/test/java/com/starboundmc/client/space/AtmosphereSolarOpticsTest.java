package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AtmosphereSolarOpticsTest {
    @Test
    void verticalColumnsMatchExponentialDensityIntegral() {
        double[] column = AtmosphereSolarOptics.column(1,1);
        assertEquals(.0016*(1-Math.exp(-.08/.0016)),column[0],.0016*.002);
        assertEquals(.00032*(1-Math.exp(-.08/.00032)),column[1],.00032*.002);
    }

    @Test
    void horizonContainsMoreAirAndUpperOutwardRayApproachesVacuum() {
        double[] vertical = AtmosphereSolarOptics.column(1,1), horizon = AtmosphereSolarOptics.column(1,0);
        // Thin-atmosphere tangent limit: integral exp(-s*s/(2*R*H)).
        assertEquals(Math.sqrt(Math.PI*.0016/2),horizon[0],.0002);
        assertEquals(Math.sqrt(Math.PI*.00032/2),horizon[1],.0001);
        assertTrue(horizon[0] > vertical[0]*25);
        assertTrue(AtmosphereSolarOptics.column(1.04,1)[0] < 1e-12);
        assertArrayEquals(new double[] {0,0},AtmosphereSolarOptics.column(AtmosphereSolarOptics.TOP,1));
    }

    @Test
    void invalidAndGroundCrossingPathsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->AtmosphereSolarOptics.column(Double.NaN,0));
        assertThrows(IllegalArgumentException.class,()->AtmosphereSolarOptics.column(1,-.1));
        assertThrows(IllegalArgumentException.class,()->AtmosphereSolarOptics.column(.99,1));
    }
}
