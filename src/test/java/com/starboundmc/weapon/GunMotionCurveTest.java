package com.starboundmc.weapon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GunMotionCurveTest {
    @Test void chargePullsBackProgressivelyAndStopsAtFullCharge() {
        assertEquals(0, GunMotionCurve.charge(-1));
        assertEquals(0, GunMotionCurve.charge(0));
        assertEquals(.5, GunMotionCurve.charge(.5));
        assertEquals(1, GunMotionCurve.charge(1));
        assertEquals(1, GunMotionCurve.charge(60));
        for (int i = 1; i <= 100; i++)
            assertTrue(GunMotionCurve.charge(i / 100.0) > GunMotionCurve.charge((i - 1) / 100.0));
    }
    @Test void recoilKicksQuicklyThenRecoversWithoutAccumulating() {
        assertEquals(0, GunMotionCurve.recoil(-1));
        assertEquals(0, GunMotionCurve.recoil(0));
        assertEquals(1, GunMotionCurve.recoil(.025));
        assertTrue(GunMotionCurve.recoil(.05) > GunMotionCurve.recoil(.1));
        assertTrue(GunMotionCurve.recoil(.1) > GunMotionCurve.recoil(.15));
        assertEquals(0, GunMotionCurve.recoil(.18));
        assertEquals(0, GunMotionCurve.recoil(10));
    }
}
