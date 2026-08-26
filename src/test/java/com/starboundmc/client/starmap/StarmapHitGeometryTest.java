package com.starboundmc.client.starmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StarmapHitGeometryTest {
    @Test
    void preservesExpandedHitRadiiAtDefaultScale() {
        assertEquals(20.0F, StarmapHitGeometry.radius(
                StarmapLevel.GALAXY, true, false, false, 1.0F));
        assertEquals(24.0F, StarmapHitGeometry.radius(
                StarmapLevel.SYSTEM, true, false, false, 1.0F));
        assertEquals(18.0F, StarmapHitGeometry.radius(
                StarmapLevel.SYSTEM, false, false, false, 1.0F));
        assertEquals(14.0F, StarmapHitGeometry.radius(
                StarmapLevel.SYSTEM, false, true, false, 1.0F));
        assertEquals(30.0F, StarmapHitGeometry.radius(
                StarmapLevel.PLANET, false, false, true, 1.0F));
        assertEquals(14.0F, StarmapHitGeometry.radius(
                StarmapLevel.PLANET, false, true, false, 1.0F));
    }

    @Test
    void hitCircleIncludesItsBoundaryWithoutUsingASecondSearchPath() {
        assertTrue(StarmapHitGeometry.contains(30.0D, 20.0D, 10.0F, 20.0F, 20.0F));
        assertFalse(StarmapHitGeometry.contains(30.01D, 20.0D, 10.0F, 20.0F, 20.0F));
    }
}
