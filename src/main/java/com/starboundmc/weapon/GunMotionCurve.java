package com.starboundmc.weapon;

/** Time-based cosmetic envelopes, independent of render frame rate. */
public final class GunMotionCurve {
    private GunMotionCurve() {}
    public static double charge(double seconds) {
        double t = Math.clamp(seconds, 0, 1);
        return t * t * (3 - 2 * t);
    }
    public static double recoil(double seconds) {
        if (seconds < 0 || seconds >= .18) return 0;
        if (seconds < .025) return seconds / .025;
        double t = (seconds - .025) / .155;
        return (1 - t) * (1 - t);
    }
}
