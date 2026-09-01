package com.starboundmc.client.shipai;

/** Pure timing curves for N.O.V.A.'s pixel-aligned eye motion. */
final class NovaEyeMotion {
    static final int BLINK_DURATION_TICKS = 4;
    static final int GAZE_TRANSITION_TICKS = 8;
    static final int MIN_BLINK_DELAY_TICKS = 60;
    static final int MAX_BLINK_DELAY_TICKS = 120;
    static final int MIN_GAZE_DELAY_TICKS = 44;
    static final int MAX_GAZE_DELAY_TICKS = 84;

    private static final float MIN_BLINK_SCALE = 0.12F;
    private static final int[] GAZE_X = {1, 0, 0, -1, 0, 1, 0, -1};
    private static final int[] GAZE_Y = {0, 0, -1, 0, 1, -1, 0, 1};

    private NovaEyeMotion() {
    }

    static int blinkDelayTicks(int sequence) {
        return MIN_BLINK_DELAY_TICKS
                + Math.floorMod(sequence * 37 + 17,
                MAX_BLINK_DELAY_TICKS - MIN_BLINK_DELAY_TICKS + 1);
    }

    static int gazeDelayTicks(int sequence) {
        return MIN_GAZE_DELAY_TICKS
                + Math.floorMod(sequence * 23 + 11,
                MAX_GAZE_DELAY_TICKS - MIN_GAZE_DELAY_TICKS + 1);
    }

    static int gazeX(int sequence) {
        return GAZE_X[Math.floorMod(sequence, GAZE_X.length)];
    }

    static int gazeY(int sequence) {
        return GAZE_Y[Math.floorMod(sequence, GAZE_Y.length)];
    }

    static float blinkScale(float time, int blinkStartTick) {
        float phase = time - blinkStartTick;
        if (blinkStartTick < 0 || phase < 0F || phase >= BLINK_DURATION_TICKS)
            return 1F;
        if (phase < 1F)
            return lerp(1F, 0.55F, phase);
        if (phase < 2F)
            return lerp(0.55F, MIN_BLINK_SCALE, phase - 1F);
        if (phase < 3F)
            return lerp(MIN_BLINK_SCALE, 0.55F, phase - 2F);
        return lerp(0.55F, 1F, phase - 3F);
    }

    static float gazeOffset(float from, float to, float time, int transitionStartTick) {
        float progress = clamp((time - transitionStartTick) / GAZE_TRANSITION_TICKS, 0F, 1F);
        float eased = progress * progress * (3F - 2F * progress);
        return lerp(from, to, eased);
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
