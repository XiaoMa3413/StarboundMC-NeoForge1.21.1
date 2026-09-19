// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

/** Pure resource/exposure rules, shared by runtime and boundary tests. */
public final class OxygenRules {
    private OxygenRules() { }
    public record Step(int oxygen, int exposure, boolean damage) { }
    public static Step step(int oxygen, int capacity, int exposure, boolean equipped,
                            boolean breathable, boolean refill, int consumption, int refillRate, int grace) {
        int current = Math.clamp(oxygen, 0, capacity);
        boolean supplied = breathable || equipped && current >= consumption;
        if (equipped) current = breathable ? (refill ? Math.min(capacity, current + refillRate) : current)
                : Math.max(0, current - consumption);
        int nextExposure = supplied ? Math.max(0, exposure - 2) : Math.min(grace + 10, exposure + 1);
        return new Step(current, nextExposure, !supplied && nextExposure > grace);
    }
    public static boolean canAcceptCanister(int oxygen, int capacity, int units) {
        return units > 0 && oxygen >= 0 && (long) oxygen + units <= capacity;
    }
    public static int warning(int oxygen, int capacity) {
        if (capacity <= 0 || oxygen <= 0) return 4;
        double fraction = (double) oxygen / capacity;
        return fraction <= .05 ? 3 : fraction <= .15 ? 2 : fraction <= .30 ? 1 : 0;
    }
}
