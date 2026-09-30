package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BackgroundStarCatalogTest {
    @Test
    void qualityChangesRetainExistingStarDirections() {
        assertArrayEquals(BackgroundStarCatalog.generate(2000),
                Arrays.copyOf(BackgroundStarCatalog.generate(6000), 2000));
    }

    @Test
    void largestBudgetHasFiniteUnitDirectionsAndBoundedVisuals() {
        for (var star : BackgroundStarCatalog.generate(32000)) {
            double length = Math.sqrt(star.x() * star.x() + star.y() * star.y() + star.z() * star.z());
            assertEquals(1, length, 2e-7);
            assertTrue(star.size() > 0 && star.size() < .5);
            assertTrue(star.brightness() >= .22F && star.brightness() <= 1);
            assertTrue(star.phase() >= 0 && star.phase() < Math.PI * 2);
        }
        assertThrows(IllegalArgumentException.class, () -> BackgroundStarCatalog.generate(0));
        assertThrows(IllegalArgumentException.class, () -> BackgroundStarCatalog.generate(32001));
    }

    @Test
    void GalacticBandDoesNotReplaceTheSurroundingStarfield() {
        double length = Math.sqrt(.24 * .24 + .87 * .87 + .43 * .43);
        int band = 0, outer = 0;
        for (var star : BackgroundStarCatalog.generate(10000)) {
            double latitude = Math.abs((star.x() * .24 + star.y() * .87 + star.z() * .43) / length);
            if (latitude < .2) band++;
            if (latitude > .5) outer++;
        }
        assertTrue(band > 4000 && band < 6000, "band=" + band);
        assertTrue(outer > 2500, "outer=" + outer);
    }
}
