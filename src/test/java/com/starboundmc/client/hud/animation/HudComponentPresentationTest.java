// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudComponentPresentationTest {
    @Test
    void everyComponentSettlesAndStaysQuiet() {
        for (var kind : HudComponentPresentation.Kind.values()) {
            var state = new HudComponentPresentation(kind);
            for (int frame = 0; frame < 144; frame++) {
                state.update(1D / 144, true);
                assertTrue(Math.abs(state.offsetY()) < 1);
                assertTrue(state.glow() >= .07999 && state.glow() < .12);
            }
            assertEquals(1, state.progress());
            assertEquals(0, state.offsetY(), .00001);
            for (int frame = 0; frame < 1000; frame++) state.update(1D / 30, true);
            assertEquals(.08, state.glow(), .00001);
            state.update(0, false);
            state.update(0, true);
            assertEquals(0, state.progress());
        }
    }

    @Test
    void elapsedTimeProducesTheSameStateAtDifferentFrameRates() {
        for (int fps : new int[]{30, 60, 144}) {
            var state = new HudComponentPresentation(HudComponentPresentation.Kind.NAVIGATION);
            for (int frame = 0; frame < fps / 6; frame++) state.update(1D / fps, true);
            assertEquals((1D / 6) / .52, state.progress(), .00001);
            float before = state.progress();
            for (int frame = 0; frame < 500; frame++) state.update(0, true);
            assertEquals(before, state.progress());
            state.settle();
            state.update(0, true);
            assertEquals(1, state.progress());
        }
    }

    @Test
    void compassEstablishesTheCenterBeforeThePeriphery() {
        assertTrue(HudComponentPresentation.compassReveal(.35F, 0) > 0);
        assertEquals(0, HudComponentPresentation.compassReveal(.35F, 1));
        for (int i = 0; i <= 100; i++) {
            assertEquals(1, HudComponentPresentation.compassReveal(1, i / 100F), .00001);
            assertEquals(0, HudComponentPresentation.compassReveal(0, i / 100F));
        }
    }

    @Test
    void clockDoesNotCatchUpAfterHiddenFramesLongFramesOrCameraChanges() {
        var clock = new HudAnimationClock();
        assertEquals(0, clock.advance(1_000_000_000L, true));
        assertEquals(.05, clock.advance(1_050_000_000L, true), .00001);
        assertEquals(0, clock.advance(1_100_000_000L, false));
        assertEquals(0, clock.advance(100_000_000_000L, true));
        assertEquals(0, clock.advance(101_000_000_000L, true));
        assertEquals(.05, clock.advance(101_050_000_000L, true), .00001);
        clock.suspend();
        assertEquals(0, clock.advance(101_100_000_000L, true));
    }
}
