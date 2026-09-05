package com.starboundmc.client;

/**
 * Monotonic client-side clock used to sample an authoritative flight curve.
 * Network snapshots may arrive slightly early or late; allowing their tick
 * value to jump directly into the renderer makes nearby bodies visibly hop.
 */
final class FlightVisualClock
{
    /** Match the server's 20 TPS flight clock; smoothing must not speed up arrival. */
    private static final double MAX_ADVANCE_PER_SECOND = 20.0;

    private boolean initialized;
    private double value;
    private long lastNanos;

    double sample(double target, double maximum, long nowNanos)
    {
        if (!Double.isFinite(target) || !Double.isFinite(maximum)
                || maximum < 0.0)
            throw new IllegalArgumentException("invalid visual clock sample");
        double clampedTarget = Math.max(0.0, Math.min(maximum, target));
        if (!initialized)
        {
            initialized = true;
            value = clampedTarget;
            lastNanos = nowNanos;
            return value;
        }

        long elapsedNanos = nowNanos - lastNanos;
        double elapsedSeconds = elapsedNanos <= 0L ? 0.0
                : Math.min(0.25, elapsedNanos / 1_000_000_000.0);
        // Never move the visual clock backwards when a late snapshot carries
        // an older elapsed tick than the one already shown on screen.
        clampedTarget = Math.max(clampedTarget, value);
        value = Math.min(clampedTarget,
                value + elapsedSeconds * MAX_ADVANCE_PER_SECOND);
        lastNanos = nowNanos;
        return value;
    }

    void reset()
    {
        initialized = false;
        value = 0.0;
        lastNanos = 0L;
    }
}
