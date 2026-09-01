package com.starboundmc.client.shipai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NovaTextPulseTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void textPulseFormsAGentleTwelveCharacterWave() {
        assertEquals(0.50F, NovaTextPulse.targetForStep(2), EPSILON);
        assertEquals(1.00F, NovaTextPulse.targetForStep(5), EPSILON);
        assertEquals(0.50F, NovaTextPulse.targetForStep(8), EPSILON);
        assertEquals(0.00F, NovaTextPulse.targetForStep(11), EPSILON);
        assertEquals(NovaTextPulse.targetForStep(0), NovaTextPulse.targetForStep(12), EPSILON);
    }

    @Test
    void stepAdvancesAndWrapsWithEachCharacter() {
        int step = -1;
        for (int expected = 0; expected < 12; expected++) {
            step = NovaTextPulse.nextStep(step);
            assertEquals(expected, step);
        }
        assertEquals(0, NovaTextPulse.nextStep(step));
    }

    @Test
    void textPulseIsInactiveBeforeTheFirstCharacter() {
        assertEquals(0.00F, NovaTextPulse.targetForStep(-1), EPSILON);
    }
}
