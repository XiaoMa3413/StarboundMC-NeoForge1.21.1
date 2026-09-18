// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExposureRulesTest {
    @Test void equalOrHigherProtectionRecoversInsteadOfDelayingDeath() {
        assertEquals(75, ExposureRules.step(80, 1, 1, 2, 5).exposure());
        assertEquals(75, ExposureRules.step(80, 1, 3, 2, 5).exposure());
        assertFalse(ExposureRules.step(100, 1, 1, 2, 5).damage());
    }
    @Test void deficitScalesAccumulationWithoutInstantDamage() {
        assertEquals(2, ExposureRules.step(0, 1, 0, 2, 5).exposure());
        assertEquals(4, ExposureRules.step(0, 3, 1, 2, 5).exposure());
        assertFalse(ExposureRules.step(0, 3, 0, 2, 5).damage());
    }
    @Test void graceSymptomsAndDamageHaveDistinctThresholds() {
        assertEquals(0, ExposureRules.step(22, 1, 0, 2, 5).warning());
        assertEquals(1, ExposureRules.step(23, 1, 0, 2, 5).warning());
        assertEquals(2, ExposureRules.step(48, 1, 0, 2, 5).warning());
        assertEquals(3, ExposureRules.step(73, 1, 0, 2, 5).warning());
        assertFalse(ExposureRules.step(97, 1, 0, 2, 5).damage());
        assertTrue(ExposureRules.step(98, 1, 0, 2, 5).damage());
    }
    @Test void exposureStaysBoundedAndLeavingHazardStopsDamageImmediately() {
        assertEquals(100, ExposureRules.step(99, 3, 0, 100, 5).exposure());
        var recovery = ExposureRules.step(100, 0, 0, 2, 5);
        assertEquals(95, recovery.exposure()); assertFalse(recovery.damage());
        assertEquals(0, ExposureRules.step(3, 0, 0, 2, 5).exposure());
    }
}
