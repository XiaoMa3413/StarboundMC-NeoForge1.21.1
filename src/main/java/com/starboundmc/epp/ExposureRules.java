// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

/** One-second environmental exposure, separate from oxygen and its grace period. */
public final class ExposureRules {
    public static final int MAX = 100;
    private ExposureRules() { }
    public record Step(int exposure, int warning, boolean damage) { }
    public static Step step(int exposure, int hazardTier, int protectionTier, int accumulation, int recovery) {
        int deficit = Math.max(0, hazardTier - protectionTier);
        int current = Math.clamp(exposure, 0, MAX);
        int next = deficit > 0 ? (int) Math.min(MAX, current + (long) deficit * accumulation)
                : Math.max(0, current - recovery);
        return new Step(next, next >= 75 ? 3 : next >= 50 ? 2 : next >= 25 ? 1 : 0,
                deficit > 0 && next == MAX);
    }
}
