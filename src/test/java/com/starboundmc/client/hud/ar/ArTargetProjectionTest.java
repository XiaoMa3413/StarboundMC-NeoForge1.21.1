// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArTargetProjectionTest {
    @Test
    void visibleTargetsKeepTheirScreenPositionAndAltitude() {
        var center = ArTargetProjection.project(0, 0, 1, 800, 600);
        assertEquals(400, center.x());
        assertEquals(300, center.y());
        assertFalse(center.edge());
        var rightAbove = ArTargetProjection.project(.5F, .5F, 1, 800, 600);
        assertEquals(600, rightAbove.x());
        assertEquals(150, rightAbove.y());
        assertFalse(rightAbove.edge());
    }

    @Test
    void offscreenAndBehindTargetsRemainOnTheCorrectEdge() {
        var right = ArTargetProjection.project(2, 0, 1, 800, 600);
        assertTrue(right.edge());
        assertEquals(772, right.x());
        var behindLeft = ArTargetProjection.project(-1, 0, -1, 800, 600);
        assertTrue(behindLeft.behind());
        assertEquals(28, behindLeft.x(), .001F);
        var directlyBehind = ArTargetProjection.project(0, 0, -1, 800, 600);
        assertTrue(Float.isFinite(directlyBehind.x()));
        assertTrue(directlyBehind.edge());
        var above = ArTargetProjection.project(0, 2, .00001F, 800, 600);
        assertEquals(65, above.y(), .001F);
        assertTrue(Float.isFinite(above.x()));
    }
}
