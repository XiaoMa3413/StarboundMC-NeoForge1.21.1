package com.starboundmc.client.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StellarProjectionTest {
    @Test void authoredReferenceSizeIsPreserved() {
        assertEquals(11.5F, StellarProjection.skyRadius(11.5F, 24478, 24478), 1e-5);
    }
    @Test void stellarAngularSizeDependsOnStarDistance() {
        assertTrue(StellarProjection.skyRadius(11.5F, 24478, 1700) > 60);
        assertTrue(StellarProjection.skyRadius(11.5F, 24478, 1e7) < .04);
        assertEquals(72, StellarProjection.skyRadius(11.5F, 24478, 0));
        assertThrows(IllegalArgumentException.class, () -> StellarProjection.skyRadius(1, 1, -1));
    }
}
