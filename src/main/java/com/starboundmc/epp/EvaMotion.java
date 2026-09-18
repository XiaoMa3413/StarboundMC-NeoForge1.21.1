// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import net.minecraft.world.phys.Vec3;

/** Tick-based motion shared by server and local prediction. No fuel/energy resource. */
public final class EvaMotion {
    public static final int FORWARD = 1, BACK = 2, LEFT = 4, RIGHT = 8, UP = 16, DOWN = 32;
    public static final double MAX_SPEED = 0.25; // 5 blocks/second
    private EvaMotion() { }
    public static boolean validInput(int input) { return input >= 0 && input < 64; }
    public static Vec3 step(Vec3 velocity, int input, float yaw, float pitch, boolean thrusters) {
        if (!validInput(input) || !thrusters) input = 0;
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        int forward = axis(input, FORWARD, BACK), left = axis(input, LEFT, RIGHT);
        Vec3 direction = new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p))
                .scale(forward).add(Math.cos(y) * left, axis(input, UP, DOWN), Math.sin(y) * left);
        if (direction.lengthSqr() > 1) direction = direction.normalize();
        // Equipped autopilot brakes gently; a lost pack leaves slow unpowered drift.
        Vec3 result = velocity.scale(thrusters ? 0.88 : 0.98).add(direction.scale(0.03));
        if (result.lengthSqr() > MAX_SPEED * MAX_SPEED) result = result.normalize().scale(MAX_SPEED);
        return result.lengthSqr() < 1.0e-7 ? Vec3.ZERO : result;
    }
    private static int axis(int input, int positive, int negative) {
        return ((input & positive) != 0 ? 1 : 0) - ((input & negative) != 0 ? 1 : 0);
    }
}
