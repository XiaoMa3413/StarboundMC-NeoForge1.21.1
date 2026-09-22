package com.starboundmc.client;

import com.starboundmc.client.fuel.FuelPreview;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FuelPreviewTest {
    private static int preview(int fuel, int... pairs) {
        return FuelPreview.accepted(fuel, 1000, pairs.length / 2,
                i -> pairs[i * 2], i -> pairs[i * 2 + 1]);
    }

    @Test void wholeItemsFitWithoutDiscardingSurplus() {
        assertEquals(340, preview(650, 10, 24, 50, 5));
        assertEquals(350, preview(650, 10, 35));
        assertEquals(340, preview(655, 10, 24, 50, 5));
    }

    @Test void earlierOversizedFuelBlocksLaterSmallerFuelLikeTheServer() {
        assertEquals(0, preview(980, 50, 1, 10, 2));
        assertEquals(20, preview(980, 10, 2, 50, 1));
    }

    @Test void emptyInvalidAndFullInputsCannotEnableRefill() {
        assertEquals(0, preview(0));
        assertEquals(0, preview(0, 50, 0, 0, 64));
        assertEquals(0, preview(1000, 5, 64));
        assertEquals(20, preview(0, 0, 64, 10, 2));
    }

    @Test void previewMatchesOrderedSingleItemConsumptionAcrossTankLevels() {
        int[] values = {50, 10, 5, 20, 0};
        int[] counts = {3, 7, 9, 2, 64};
        for (int initial = 0; initial <= 1000; initial++) {
            int expected = initial;
            outer: for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                for (int j = 0; j < counts[i]; j++) {
                    if (expected + values[i] > 1000) break outer;
                    expected += values[i];
                }
            }
            assertEquals(expected - initial,
                    FuelPreview.accepted(initial, 1000, values.length, i -> values[i], i -> counts[i]));
        }
    }
}
