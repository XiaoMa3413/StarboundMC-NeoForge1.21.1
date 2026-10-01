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
            assertTrue(star.sigmaPixels() >= .42F && star.sigmaPixels() <= .48F);
            assertTrue(star.flux() > 0 && star.flux() <= 8);
            assertTrue(star.dustFraction() >= 0 && star.dustFraction() <= 1);
            assertTrue(star.red() > 0 && star.red() <= 1);
            assertTrue(star.green() > 0 && star.green() <= 1);
            assertTrue(star.blue() > 0 && star.blue() <= 1);
        }
        assertThrows(IllegalArgumentException.class, () -> BackgroundStarCatalog.generate(0));
        assertThrows(IllegalArgumentException.class, () -> BackgroundStarCatalog.generate(32001));
    }

    @Test
    void faintPopulationAndRareHighlightsRemainDistinct() {
        var stars = BackgroundStarCatalog.generate(32000);
        long faint = Arrays.stream(stars).filter(star -> star.flux() < .1).count();
        long bright = Arrays.stream(stars).filter(star -> star.flux() > 1).count();
        assertTrue(faint > stars.length * .9 && faint < stars.length * .99);
        assertTrue(bright > 5 && bright < stars.length * .01);
        double baseMean=Arrays.stream(stars).limit(2000).mapToDouble(BackgroundStarCatalog.Star::flux).average().orElseThrow();
        double detailMean=Arrays.stream(stars).skip(2000).mapToDouble(BackgroundStarCatalog.Star::flux).average().orElseThrow();
        assertTrue(detailMean < baseMean*.7, "Extra quality must add fine stars rather than many more bright sources");
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
