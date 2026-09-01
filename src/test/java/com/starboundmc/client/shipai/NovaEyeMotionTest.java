package com.starboundmc.client.shipai;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NovaEyeMotionTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void blinkUsesAShortFivePoseCurve() {
        int startTick = 10;
        assertEquals(1F, NovaEyeMotion.blinkScale(9F, startTick), EPSILON);
        assertEquals(1F, NovaEyeMotion.blinkScale(10F, startTick), EPSILON);
        assertEquals(0.55F, NovaEyeMotion.blinkScale(11F, startTick), EPSILON);
        assertEquals(0.12F, NovaEyeMotion.blinkScale(12F, startTick), EPSILON);
        assertEquals(0.55F, NovaEyeMotion.blinkScale(13F, startTick), EPSILON);
        assertEquals(1F, NovaEyeMotion.blinkScale(14F, startTick), EPSILON);
    }

    @Test
    void blinkAndGazeDelaysVaryWithinComfortableBounds() {
        Set<Integer> blinkDelays = new HashSet<>();
        Set<Integer> gazeDelays = new HashSet<>();
        for (int sequence = 0; sequence < 32; sequence++) {
            int blinkDelay = NovaEyeMotion.blinkDelayTicks(sequence);
            int gazeDelay = NovaEyeMotion.gazeDelayTicks(sequence);
            assertTrue(blinkDelay >= NovaEyeMotion.MIN_BLINK_DELAY_TICKS);
            assertTrue(blinkDelay <= NovaEyeMotion.MAX_BLINK_DELAY_TICKS);
            assertTrue(gazeDelay >= NovaEyeMotion.MIN_GAZE_DELAY_TICKS);
            assertTrue(gazeDelay <= NovaEyeMotion.MAX_GAZE_DELAY_TICKS);
            blinkDelays.add(blinkDelay);
            gazeDelays.add(gazeDelay);
        }
        assertTrue(blinkDelays.size() > 8);
        assertTrue(gazeDelays.size() > 8);
    }

    @Test
    void gazeTargetsStayInsideOnePixel() {
        boolean visitsNeutral = false;
        for (int sequence = 0; sequence < 16; sequence++) {
            int x = NovaEyeMotion.gazeX(sequence);
            int y = NovaEyeMotion.gazeY(sequence);
            assertTrue(Math.abs(x) <= 1);
            assertTrue(Math.abs(y) <= 1);
            visitsNeutral |= x == 0 && y == 0;
        }
        assertTrue(visitsNeutral);
    }

    @Test
    void gazeTransitionUsesSmoothEndpoints() {
        assertEquals(0F, NovaEyeMotion.gazeOffset(0F, 1F, 20F, 20), EPSILON);
        assertEquals(0.5F, NovaEyeMotion.gazeOffset(0F, 1F, 24F, 20), EPSILON);
        assertEquals(1F, NovaEyeMotion.gazeOffset(0F, 1F, 28F, 20), EPSILON);
    }

    @Test
    void activityPulseRisesOnceAndSettles() {
        int startTick = 40;
        assertEquals(0F, NovaEyeMotion.activityPulse(39F, startTick), EPSILON);
        assertEquals(0F, NovaEyeMotion.activityPulse(40F, startTick), EPSILON);
        assertEquals(0.5F, NovaEyeMotion.activityPulse(42F, startTick), EPSILON);
        assertEquals(1F, NovaEyeMotion.activityPulse(44F, startTick), EPSILON);
        assertTrue(NovaEyeMotion.activityPulse(50F, startTick) < 1F);
        assertEquals(0F, NovaEyeMotion.activityPulse(
                startTick + NovaEyeMotion.ACTIVITY_PULSE_DURATION_TICKS, startTick), EPSILON);
    }

    @Test
    void scanningGazeSweepsBetweenSinglePixelEndpoints() {
        int startTick = 60;
        assertEquals(0F, NovaEyeMotion.scanningGazeX(71F, startTick), EPSILON);
        assertEquals(-1F, NovaEyeMotion.scanningGazeX(72F, startTick), EPSILON);
        assertEquals(0F, NovaEyeMotion.scanningGazeX(81F, startTick), EPSILON);
        assertEquals(1F, NovaEyeMotion.scanningGazeX(90F, startTick), EPSILON);
        assertEquals(0F, NovaEyeMotion.scanningGazeX(99F, startTick), EPSILON);
        assertEquals(-1F, NovaEyeMotion.scanningGazeX(108F, startTick), EPSILON);
    }

    @Test
    void scanningPanelWaitsThenUnfoldsOverOneSecond() {
        int startTick = 30;
        assertEquals(false, NovaEyeMotion.scanningPanelActive(41F, startTick));
        assertEquals(true, NovaEyeMotion.scanningPanelActive(42F, startTick));
        assertEquals(0F, NovaEyeMotion.scanningPanelWidthReveal(42F, startTick), EPSILON);
        assertEquals(0.5F, NovaEyeMotion.scanningPanelWidthReveal(46F, startTick), EPSILON);
        assertEquals(1F, NovaEyeMotion.scanningPanelWidthReveal(50F, startTick), EPSILON);

        assertEquals(0F, NovaEyeMotion.scanningPanelHeightReveal(48F, startTick), EPSILON);
        assertEquals(0.5F, NovaEyeMotion.scanningPanelHeightReveal(53F, startTick), EPSILON);
        assertEquals(1F, NovaEyeMotion.scanningPanelHeightReveal(58F, startTick), EPSILON);

        assertEquals(0F, NovaEyeMotion.scanningPanelDataReveal(55F, startTick), EPSILON);
        assertEquals(0.5F, NovaEyeMotion.scanningPanelDataReveal(58.5F, startTick), EPSILON);
        assertEquals(1F, NovaEyeMotion.scanningPanelDataReveal(62F, startTick), EPSILON);
        assertEquals(1F, NovaEyeMotion.scanningPanelDataReveal(90F, startTick), EPSILON);
    }
}
