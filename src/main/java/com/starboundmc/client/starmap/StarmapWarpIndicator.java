package com.starboundmc.client.starmap;

/** Pure timing and color math for the starmap's warp-status pulse. */
final class StarmapWarpIndicator {
    private StarmapWarpIndicator() {
    }

    static float pulseIntensity(double renderClock) {
        float pulse = 0.775F + 0.225F * (float) Math.sin(renderClock * 0.34D);
        return Math.max(0.55F, Math.min(1.0F, pulse));
    }

    static int pulseColor(double renderClock, int color) {
        int alpha = Math.round(255.0F * pulseIntensity(renderClock));
        return alpha << 24 | color & 0x00FFFFFF;
    }
}
