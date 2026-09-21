// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

/** Small, frame-rate-independent camera lag for player-attached HUD components. */
public final class HudVisorMotion {
    private static final double RESPONSE = 24;
    private static final double YAW_GAIN = .0075;
    private static final double PITCH_GAIN = .005;
    private final Axis horizontal = new Axis(1.8);
    private final Axis vertical = new Axis(1.2);
    private boolean initialized;
    private float lastYaw;
    private float lastPitch;

    public float x() { return (float) horizontal.position; }
    public float y() { return (float) vertical.position; }

    /** Angles are camera degrees; the first visible frame only establishes a baseline. */
    public void update(double seconds, float yaw, float pitch, boolean active) {
        if (!active || !Double.isFinite(seconds) || seconds <= 0 || seconds > .25
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            reset();
            return;
        }
        double turn = wrapDegrees((double) yaw - lastYaw);
        double tilt = (double) pitch - lastPitch;
        if (!initialized || Math.abs(turn) > 45 || Math.abs(tilt) > 45) {
            reset();
            initialized = true;
        } else {
            horizontal.follow(-turn / seconds * YAW_GAIN, seconds);
            vertical.follow(tilt / seconds * PITCH_GAIN, seconds);
        }
        lastYaw = yaw;
        lastPitch = pitch;
    }

    /** Discard both velocity and camera history across hidden frames and camera changes. */
    public void reset() {
        horizontal.clear();
        vertical.clear();
        initialized = false;
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360;
        if (wrapped >= 180) wrapped -= 360;
        if (wrapped < -180) wrapped += 360;
        return wrapped;
    }

    private static final class Axis {
        private final double limit;
        private double position;
        private double velocity;

        private Axis(double limit) { this.limit = limit; }

        private void follow(double target, double seconds) {
            target = Math.clamp(target, -limit, limit);
            // Exact critically damped step for a constant target during this frame.
            // Unlike Euler integration, this remains stable at low frame rates.
            double displacement = position - target;
            double impulse = velocity + RESPONSE * displacement;
            double decay = Math.exp(-RESPONSE * seconds);
            position = target + (displacement + impulse * seconds) * decay;
            velocity = (velocity - RESPONSE * impulse * seconds) * decay;
            position = Math.clamp(position, -limit, limit);
            if (Math.abs(position) >= limit && position * velocity > 0)
                velocity = 0;
            if (target == 0 && Math.abs(position) < .001 && Math.abs(velocity) < .01)
                clear();
        }

        private void clear() { position = velocity = 0; }
    }
}
