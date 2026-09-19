// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvaMotionTest {
    @Test void movementFollowsViewWhileRiseUsesWorldUp() {
        var forward = EvaMotion.step(Vec3.ZERO, EvaMotion.FORWARD, 90, -45, true);
        assertTrue(forward.x < 0 && forward.y > 0);
        assertEquals(0, forward.z, 1e-8);
        var rise = EvaMotion.step(Vec3.ZERO, EvaMotion.UP, 180, 90, true);
        assertEquals(new Vec3(0, 0.03, 0), rise);
        assertEquals(Vec3.ZERO, EvaMotion.step(Vec3.ZERO, EvaMotion.UP | EvaMotion.DOWN, 0, 0, true));
    }
    @Test void diagonalsAndLongAccelerationCannotExceedSpeedLimit() {
        var straight = EvaMotion.step(Vec3.ZERO, EvaMotion.FORWARD, 0, 0, true);
        var diagonal = EvaMotion.step(Vec3.ZERO, EvaMotion.FORWARD | EvaMotion.LEFT | EvaMotion.UP, 0, 0, true);
        assertEquals(straight.length(), diagonal.length(), 1e-8);
        var v = Vec3.ZERO;
        for (int i = 0; i < 600; i++) {
            v = EvaMotion.step(v, 1 | 4 | 16, i, i % 90, true);
            assertTrue(v.length() <= EvaMotion.MAX_SPEED + 1e-8);
        }
        assertEquals(EvaMotion.MAX_SPEED, EvaMotion.step(new Vec3(100, 100, 0), 0, 0, 0, true).length(), 1e-8);
    }
    @Test void releaseBrakesWithoutAnAbruptStopAndNoPackCannotThrust() {
        var v = new Vec3(0, 0, .2);
        var released = EvaMotion.step(v, 0, 0, 0, true);
        assertTrue(released.z > 0 && released.z < v.z);
        for (int i = 0; i < 80; i++) v = EvaMotion.step(v, 0, 0, 0, true);
        assertEquals(Vec3.ZERO, v);
        assertEquals(Vec3.ZERO, EvaMotion.step(Vec3.ZERO, 1 | 16, 0, 0, false));
        assertEquals(Vec3.ZERO, EvaMotion.step(Vec3.ZERO, 255, 0, 0, true));
    }
    @Test void missedReleaseExpiresAndResetCannotRetainInput() {
        var state = new EvaState(); state.input = 1; state.lastInputTick = 40;
        assertEquals(1, state.freshInput(50));
        assertEquals(0, state.freshInput(51));
        assertEquals(0, state.freshInput(0));
        state.clearInput();
        assertEquals(0, state.freshInput(40));
    }
    @Test void zeroGravityDoesNotImplyBreathableOrPressurized() {
        assertEquals(0, EnvironmentState.SPACE.gravityScale());
        assertFalse(EnvironmentState.SPACE.breathable());
        assertFalse(EnvironmentState.SPACE.pressurized());
        assertEquals(1, EnvironmentState.SHIP_INTERIOR.gravityScale());
    }
}
