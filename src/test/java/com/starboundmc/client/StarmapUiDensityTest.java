package com.starboundmc.client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapUiDensityTest
{
    @ParameterizedTest
    @CsvSource({
            "1.0,1,false",
            "1.5,1,false",
            "2.0,2,true",
            "3.0,2,true",
            "4.0,2,true"
    })
    void densityUsesPhysicalPixelHeadroom(double guiScale, int expected, boolean enabled)
    {
        StarmapUiDensity density = StarmapUiDensity.forGuiScale(guiScale);

        assertEquals(expected, density.factor());
        assertEquals(enabled, density.enabled());
    }

    @ParameterizedTest
    @CsvSource({ "1.0", "2.0", "3.5", "12.25" })
    void coordinateConversionRoundTrips(double coordinate)
    {
        StarmapUiDensity density = StarmapUiDensity.forGuiScale(4.0D);

        assertEquals(coordinate, density.logical(density.virtual(coordinate)), 0.00001D);
        assertEquals(26, density.virtual(13));
        assertEquals(0.5F, density.inverseScale());
        assertTrue(density.enabled());
        assertFalse(StarmapUiDensity.forGuiScale(1.0D).enabled());
    }

    @ParameterizedTest
    @CsvSource({
            "1.0,39,81,100.5",
            "2.0,78,162,201.0"
    })
    void centeredSpritesShareTheProjectedRasterPixelCenter(
            double guiScale, int virtualSize, int expectedOrigin, double expectedCenter)
    {
        StarmapUiDensity density = StarmapUiDensity.forGuiScale(guiScale);
        int origin = density.centeredOrigin(100, virtualSize);

        assertEquals(expectedOrigin, origin);
        assertEquals(expectedCenter, origin + virtualSize / 2.0D, 0.00001D);
        assertEquals(expectedCenter, density.virtualPixelCenter(100), 0.00001D);
    }

    @ParameterizedTest
    @CsvSource({ "0.0", "-1.0" })
    void invalidGuiScalesAreRejected(double guiScale)
    {
        assertThrows(IllegalArgumentException.class,
                () -> StarmapUiDensity.forGuiScale(guiScale));
    }
}
