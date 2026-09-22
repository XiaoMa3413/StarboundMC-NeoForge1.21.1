// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.animation;

/** Presentation time only: hidden frames and camera discontinuities never accumulate catch-up time. */
public final class HudAnimationClock {
    private static final double SPEED = readSpeed();
    private long previousNanos;

    public double advance(long nowNanos, boolean active) {
        if (!active) {
            suspend();
            return 0;
        }
        long previous = previousNanos;
        previousNanos = nowNanos;
        if (previous == 0) return 0;
        double seconds = (nowNanos - previous) / 1_000_000_000D;
        return seconds > 0 && seconds <= .25 ? seconds * SPEED : 0;
    }

    public void suspend() { previousNanos = 0; }

    private static double readSpeed() {
        try {
            double speed = Double.parseDouble(System.getProperty("starboundmc.debug.hudAnimationSpeed", "1"));
            return Double.isFinite(speed) ? Math.clamp(speed, .05, 1) : 1;
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
