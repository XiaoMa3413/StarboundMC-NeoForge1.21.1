// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudVisorMotionTest {
    @Test
    void stationaryCameraNeverDrifts() {
        var motion = new HudVisorMotion();
        for (int frame = 0; frame < 120; frame++)
            motion.update(1D / 60, 137, -24, true);
        assertResting(motion);
    }

    @Test
    void followsTurnDirectionWithinSmallScreenSpaceBounds() {
        var motion = new HudVisorMotion();
        motion.update(1D / 60, 0, 0, true);
        for (int frame = 1; frame <= 120; frame++) {
            motion.update(1D / 60, frame * 30, frame * 20, true);
            assertTrue(motion.x() < 0 && motion.x() >= -1.80001F);
            assertTrue(motion.y() > 0 && motion.y() <= 1.20001F);
        }
        assertEquals(-1.8F, motion.x(), .001F);
        assertEquals(1.2F, motion.y(), .001F);
    }

    @Test
    void constantTurnHasTheSameResponseAt30_60_And144Fps() {
        float[] reference = sampleTurnAndStop(60);
        for (int fps : new int[]{30, 144}) {
            float[] actual = sampleTurnAndStop(fps);
            for (int sample = 0; sample < reference.length; sample++)
                assertEquals(reference[sample], actual[sample], .0001F);
        }
    }

    @Test
    void stoppedCameraSettlesQuicklyWithoutCrossingOrBouncing() {
        var motion = new HudVisorMotion();
        motion.update(1D / 60, 0, 0, true);
        for (int frame = 1; frame <= 60; frame++)
            motion.update(1D / 60, frame * 1.5F, frame * .5F, true);
        for (int frame = 1; frame <= 60; frame++) {
            float previousX = Math.abs(motion.x());
            float previousY = Math.abs(motion.y());
            motion.update(1D / 60, 90, 30, true);
            assertTrue(motion.x() <= 0 && motion.y() >= 0);
            assertTrue(Math.abs(motion.x()) <= previousX + .00001F);
            assertTrue(Math.abs(motion.y()) <= previousY + .00001F);
            if (frame == 24) {
                assertTrue(Math.abs(motion.x()) < .003F);
                assertTrue(Math.abs(motion.y()) < .003F);
            }
        }
        assertResting(motion);
    }

    @Test
    void crossingYawWrapIsASmallTurn() {
        var wrapped = new HudVisorMotion();
        var reference = new HudVisorMotion();
        wrapped.update(1D / 60, 179, 0, true);
        reference.update(1D / 60, 0, 0, true);
        wrapped.update(1D / 60, -179, 0, true);
        reference.update(1D / 60, 2, 0, true);
        assertEquals(reference.x(), wrapped.x(), .00001F);
        assertTrue(wrapped.x() < 0);
    }

    @Test
    void reversingTurnRemainsBoundedAndSettlesInTheNewDirection() {
        var motion = moving();
        for (int frame = 1; frame <= 60; frame++) {
            motion.update(1D / 60, 20 - frame * 4, 10 - frame, true);
            assertTrue(Math.abs(motion.x()) <= 1.80001F);
            assertTrue(Math.abs(motion.y()) <= 1.20001F);
        }
        assertTrue(motion.x() > 0 && motion.y() < 0);
    }

    @Test
    void hiddenCameraMotionDoesNotKickTheHudWhenItReturns() {
        var motion = moving();
        motion.update(1D / 60, 40, 20, false);
        assertResting(motion);
        motion.update(1D / 60, 75, 30, true);
        motion.update(1D / 60, 75, 30, true);
        assertResting(motion);
    }

    @Test
    void lifecycleResetDiscardsVelocityAndCameraHistory() {
        var motion = moving();
        motion.reset();
        motion.update(1D / 60, 40, 20, true);
        motion.update(1D / 60, 40, 20, true);
        assertResting(motion);
    }

    @Test
    void cameraCutsResetBothAxes() {
        for (boolean yawCut : new boolean[]{true, false}) {
            var motion = moving();
            float yaw = yawCut ? 90 : 20;
            float pitch = yawCut ? 10 : -50;
            motion.update(1D / 60, yaw, pitch, true);
            motion.update(1D / 60, yaw, pitch, true);
            assertResting(motion);
        }
    }

    @Test
    void longOrInvalidFramesResetInsteadOfAccumulatingMotion() {
        for (double seconds : new double[]{.3, 0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            var motion = moving();
            motion.update(seconds, 40, 20, true);
            motion.update(1D / 60, 60, 30, true);
            motion.update(1D / 60, 60, 30, true);
            assertResting(motion);
        }
    }

    private static HudVisorMotion moving() {
        var motion = new HudVisorMotion();
        motion.update(.1, 0, 0, true);
        motion.update(.1, 20, 10, true);
        assertTrue(motion.x() < 0 && motion.y() > 0);
        return motion;
    }

    private static float[] sampleTurnAndStop(int fps) {
        var motion = new HudVisorMotion();
        motion.update(1D / fps, 0, 0, true);
        for (int frame = 1; frame <= fps / 2; frame++)
            motion.update(1D / fps, 90F * frame / fps, 30F * frame / fps, true);
        float x = motion.x();
        float y = motion.y();
        for (int frame = 0; frame < fps / 2; frame++)
            motion.update(1D / fps, 45, 15, true);
        return new float[]{x, y, motion.x(), motion.y()};
    }

    private static void assertResting(HudVisorMotion motion) {
        assertEquals(0F, motion.x());
        assertEquals(0F, motion.y());
    }
}
