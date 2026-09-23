// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SurvivalFeedbackTest {
    @Test
    void worseningIsImmediateWhileRecoveryIsSmoothAndBounded() {
        for (var kind : SurvivalFeedback.Kind.values()) {
            var state = new SurvivalFeedback(kind);
            float safe = kind == SurvivalFeedback.Kind.OXYGEN ? 1 : 0;
            float danger = 1 - safe;
            state.update(0, safe, 0);
            assertEquals(safe, state.displayed());
            state.update(0, danger, 3);
            assertEquals(danger, state.displayed());
            state.update(.1, safe, 0);
            assertTrue(state.displayed() > 0 && state.displayed() < 1);
            float previousError = Math.abs(safe - state.displayed());
            for (int frame = 0; frame < 300; frame++) {
                state.update(1D / 60, safe, 0);
                float error = Math.abs(safe - state.displayed());
                assertTrue(error <= previousError);
                previousError = error;
            }
            assertEquals(safe, state.displayed(), .001);
            assertEquals(0, state.pulse());
        }
    }

    @Test
    void repeatedWarningsLeaveLongQuietIntervalsAndDoNotRestartOnSnapshots() {
        var state = new SurvivalFeedback(SurvivalFeedback.Kind.OXYGEN);
        state.update(0, .1F, 2);
        int activeFrames = 0;
        for (int frame = 0; frame < 600; frame++) {
            state.update(1D / 60, .1F, 2);
            if (state.pulse() > .01) activeFrames++;
        }
        assertTrue(activeFrames > 40 && activeFrames < 150);
        state.update(0, .04F, 3);
        state.update(.12, .04F, 3);
        assertTrue(state.pulse() > .9);
        state.settle();
        assertEquals(0, state.pulse());
    }

    @Test
    void cautionOnlyPromptsOnceAndPauseDoesNotAdvanceTheCue() {
        var state = new SurvivalFeedback(SurvivalFeedback.Kind.COLD);
        state.update(.1, .25F, 1);
        float before = state.pulse();
        for (int i = 0; i < 500; i++) state.update(0, .25F, 1);
        assertEquals(before, state.pulse());
        for (int i = 0; i < 600; i++) state.update(1D / 60, .25F, 1);
        assertEquals(0, state.pulse());
    }

    @Test
    void recoveryAndReminderTimingAgreeAt30_60_And144Fps() {
        for (int fps : new int[]{30, 60, 144}) {
            var state = new SurvivalFeedback(SurvivalFeedback.Kind.OXYGEN);
            state.update(0, 0, 3);
            for (int frame = 0; frame < fps / 2; frame++) state.update(1D / fps, 1, 3);
            assertEquals(1 - Math.exp(-.5 / .28), state.displayed(), .00001);
            assertTrue(state.pulse() < .1);
        }
    }
}
