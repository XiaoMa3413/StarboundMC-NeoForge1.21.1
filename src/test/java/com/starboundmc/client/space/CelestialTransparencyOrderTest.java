package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CelestialTransparencyOrderTest {
    @Test void smallBodyBehindLargeLimbMustRenderFirstDespiteNearerCentre() {
        double smallDistance=Math.hypot(49,24);
        double rayDotLarge=100*24/smallDistance;
        double largeExit=rayDotLarge+Math.sqrt(rayDotLarge*rayDotLarge-(100*100-90*90));
        assertTrue(Math.hypot(49,76)>90+.2); // physically disjoint spheres
        assertTrue(largeExit<smallDistance-.2); // independent ray intersection reference
        assertTrue(smallDistance<100); // the former centre sort gives the wrong order
        assertTrue(CelestialTransparencyOrder.key(smallDistance*smallDistance,.2)
                >CelestialTransparencyOrder.key(100*100,90));
    }

    @Test void containingVolumeFollowsEveryDisjointExteriorVolume() {
        assertTrue(CelestialTransparencyOrder.key(1,2)<CelestialTransparencyOrder.key(100,1));
        assertEquals(0,CelestialTransparencyOrder.key(100,10));
    }

    @Test void boundIncludesAnExtendedRingWithoutChangingCentreOrLod() {
        double plain=CelestialTransparencyOrder.key(400,5);
        double ring=CelestialTransparencyOrder.key(400,5*2.27);
        assertTrue(ring<plain);
        assertEquals(plain*9,CelestialTransparencyOrder.key(400*9,5*3),1e-10);
    }
}
