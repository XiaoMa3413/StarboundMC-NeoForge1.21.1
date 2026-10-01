package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StarPhotometryTest {
    @Test void magnitudeRetainsPhotometricRatiosAndSubByteFlux() {
        assertEquals(.01,StarPhotometry.flux(6.2)/StarPhotometry.flux(1.2),1e-8);
        assertTrue(StarPhotometry.flux(10) < 1F/255);
        assertTrue(StarPhotometry.flux(10) > StarPhotometry.flux(10.01));
    }
    @Test void temperaturesProduceContinuousWarmNeutralAndBlueColours() {
        StarPhotometry.Color warm=StarPhotometry.color(3800),neutral=StarPhotometry.color(6500),blue=StarPhotometry.color(14000);
        assertTrue(warm.red() > warm.blue()*1.5);
        assertTrue(Math.abs(neutral.red()-neutral.blue()) < .12);
        assertTrue(blue.blue() > blue.red()*1.3);
        for (int k=3500;k<15000;k+=10) {
            StarPhotometry.Color a=StarPhotometry.color(k),b=StarPhotometry.color(k+1);
            assertTrue(Math.abs(a.red()-b.red()) < .001 && Math.abs(a.blue()-b.blue()) < .001);
        }
    }
    @Test void invalidColourAndMagnitudeInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->StarPhotometry.color(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->StarPhotometry.color(3499));
        assertThrows(IllegalArgumentException.class,()->StarPhotometry.color(15001));
        assertThrows(IllegalArgumentException.class,()->StarPhotometry.flux(Double.POSITIVE_INFINITY));
    }
}
