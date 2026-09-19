// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

/** One-second environmental exposure, separate from oxygen and its grace period. */
public final class ExposureRules {
    public static final int MAX = 100;
    private ExposureRules() { }
    public record Step(int exposure, int warning, boolean damage) { }
    public static Step step(int exposure, int hazardTier, int protectionTier, int accumulation, int recovery) {
        // Legacy numeric fields remain readable; temperature protection is binary.
        boolean unprotected = hazardTier > 0 && protectionTier <= 0;
        int current = Math.clamp(exposure, 0, MAX);
        int next = unprotected ? (int) Math.min(MAX, current + (long) accumulation)
                : Math.max(0, current - recovery);
        return new Step(next, next >= 75 ? 3 : next >= 50 ? 2 : next >= 25 ? 1 : 0,
                unprotected && next == MAX);
    }
}
