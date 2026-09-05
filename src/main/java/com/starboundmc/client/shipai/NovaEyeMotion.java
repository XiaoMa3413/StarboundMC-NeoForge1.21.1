package com.starboundmc.client.shipai;

/** Pure timing curves for N.O.V.A.'s pixel-aligned eye motion. */
final class NovaEyeMotion {
    static final int BLINK_DURATION_TICKS = 4;
    static final int GAZE_TRANSITION_TICKS = 8;
    static final int MIN_BLINK_DELAY_TICKS = 60;
    static final int MAX_BLINK_DELAY_TICKS = 120;
    static final int MIN_GAZE_DELAY_TICKS = 44;
    static final int MAX_GAZE_DELAY_TICKS = 84;
    static final int ACTIVITY_PULSE_DURATION_TICKS = 22;
    static final int SCANNING_PANEL_IDLE_DELAY_TICKS = 12;
    static final int SCANNING_PANEL_WIDTH_REVEAL_TICKS = 8;
    static final int SCANNING_PANEL_HEIGHT_REVEAL_DELAY_TICKS = 6;
    static final int SCANNING_PANEL_HEIGHT_REVEAL_TICKS = 10;
    static final int SCANNING_PANEL_DATA_REVEAL_DELAY_TICKS = 13;
    static final int SCANNING_PANEL_DATA_REVEAL_TICKS = 7;
    static final int FAULT_GLITCH_CYCLE_TICKS = 72;

    private static final float MIN_BLINK_SCALE = 0.12F;
    private static final int ACTIVITY_PULSE_RISE_TICKS = 4;
    private static final float SCANNING_GAZE_PERIOD_TICKS = 36F;
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
        float eased = smoothStep(progress);
        return lerp(from, to, eased);
    }

    static float activityPulse(float time, int activityStartTick) {
        float phase = time - activityStartTick;
        if (activityStartTick < 0 || phase < 0F || phase >= ACTIVITY_PULSE_DURATION_TICKS)
            return 0F;
        if (phase < ACTIVITY_PULSE_RISE_TICKS)
            return smoothStep(phase / ACTIVITY_PULSE_RISE_TICKS);
        float fallProgress = (phase - ACTIVITY_PULSE_RISE_TICKS)
                / (ACTIVITY_PULSE_DURATION_TICKS - ACTIVITY_PULSE_RISE_TICKS);
        return 1F - smoothStep(fallProgress);
    }

    static float scanningGazeX(float time, int activityStartTick) {
        if (!scanningPanelActive(time, activityStartTick))
            return 0F;
        float phase = positiveModulo(time - activityStartTick - SCANNING_PANEL_IDLE_DELAY_TICKS,
                SCANNING_GAZE_PERIOD_TICKS);
        float halfPeriod = SCANNING_GAZE_PERIOD_TICKS * 0.5F;
        if (phase < halfPeriod)
            return lerp(-1F, 1F, smoothStep(phase / halfPeriod));
        return lerp(1F, -1F, smoothStep((phase - halfPeriod) / halfPeriod));
    }

    static float scanningPanelWidthReveal(float time, int activityStartTick) {
        return timedReveal(time, activityStartTick, SCANNING_PANEL_IDLE_DELAY_TICKS,
                SCANNING_PANEL_WIDTH_REVEAL_TICKS);
    }

    static float scanningPanelHeightReveal(float time, int activityStartTick) {
        return timedReveal(time, activityStartTick,
                SCANNING_PANEL_IDLE_DELAY_TICKS
                        + SCANNING_PANEL_HEIGHT_REVEAL_DELAY_TICKS,
                SCANNING_PANEL_HEIGHT_REVEAL_TICKS);
    }

    static float scanningPanelDataReveal(float time, int activityStartTick) {
        return timedReveal(time, activityStartTick,
                SCANNING_PANEL_IDLE_DELAY_TICKS
                        + SCANNING_PANEL_DATA_REVEAL_DELAY_TICKS,
                SCANNING_PANEL_DATA_REVEAL_TICKS);
    }

    static float faultGlitchIntensity(float time, int activityStartTick) {
        if (activityStartTick < 0)
            return 0F;
        float phase = positiveModulo(time - activityStartTick, FAULT_GLITCH_CYCLE_TICKS);
        if (phase < 6F)
            return 0.75F * (1F - smoothStep(phase / 6F));
        if (phase >= 24F && phase < 32F)
            return 0.82F * smoothStep((phase - 24F) / 4F)
                    * (1F - smoothStep((phase - 28F) / 4F));
        if (phase >= 52F && phase < 57F)
            return 0.62F * (1F - smoothStep((phase - 52F) / 5F));
        return 0F;
    }

    static boolean scanningPanelActive(float time, int activityStartTick) {
        return activityStartTick >= 0
                && time - activityStartTick >= SCANNING_PANEL_IDLE_DELAY_TICKS;
    }

    private static float timedReveal(float time, int startTick, int delayTicks,
                                     int durationTicks) {
        if (startTick < 0)
            return 0F;
        return smoothStep((time - startTick - delayTicks) / durationTicks);
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static float smoothStep(float progress) {
        float clamped = clamp(progress, 0F, 1F);
        return clamped * clamped * (3F - 2F * clamped);
    }

    private static float positiveModulo(float value, float modulus) {
        float result = value % modulus;
        return result < 0F ? result + modulus : result;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
