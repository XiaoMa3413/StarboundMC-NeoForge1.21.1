// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OxygenRulesTest {
    private OxygenRules.Step step(int oxygen, int exposure, boolean equipped, boolean air, boolean refill) {
        return OxygenRules.step(oxygen, 720, exposure, equipped, air, refill, 1, 24, 10);
    }
    @Test void tankProvidesTwelveMinutesThenGraceBegins() {
        int oxygen = 720;
        for (int i = 0; i < 720; i++) {
            var result = step(oxygen, 0, true, false, false);
            assertEquals(0, result.exposure()); assertFalse(result.damage()); oxygen = result.oxygen();
        }
        assertEquals(0, oxygen);
        assertEquals(1, step(oxygen, 0, true, false, false).exposure());
    }
    @Test void absentEquipmentCannotUseAnInventoryTank() {
        var result = step(720, 10, false, false, false);
        assertTrue(result.damage()); assertEquals(11, result.exposure());
    }
    @Test void tenSecondsOfGraceAreNotLethal() {
        for (int i = 0; i < 10; i++) assertFalse(step(0, i, true, false, false).damage());
        assertTrue(step(0, 10, true, false, false).damage());
    }
    @Test void removingEmptyEquipmentDoesNotResetExposure() {
        var equipped = step(0, 9, true, false, false);
        var removed = step(0, equipped.exposure(), false, false, false);
        assertTrue(removed.damage());
    }
    @Test void ordinaryAirStopsConsumptionWithoutRefill() {
        var result = step(300, 6, true, true, false);
        assertEquals(300, result.oxygen()); assertEquals(4, result.exposure()); assertFalse(result.damage());
    }
    @Test void controlledAirRefillsWithoutOverflow() {
        assertEquals(324, step(300, 0, true, true, true).oxygen());
        assertEquals(720, step(710, 0, true, true, true).oxygen());
        assertEquals(299, step(300, 0, true, false, true).oxygen(), "Vacuum cannot refill merely because a flag is set");
    }
    @Test void oxygenCanisterRequiresRoomForTheWholeAmount() {
        assertTrue(OxygenRules.canAcceptCanister(540, 720, 180));
        assertFalse(OxygenRules.canAcceptCanister(541, 720, 180));
        assertFalse(OxygenRules.canAcceptCanister(720, 720, 180));
        assertFalse(OxygenRules.canAcceptCanister(Integer.MAX_VALUE, 720, 180));
    }
    @Test void warningThresholdsAreInclusive() {
        assertEquals(0, OxygenRules.warning(217, 720));
        assertEquals(1, OxygenRules.warning(216, 720));
        assertEquals(2, OxygenRules.warning(108, 720));
        assertEquals(3, OxygenRules.warning(36, 720));
        assertEquals(4, OxygenRules.warning(0, 720));
    }
}
