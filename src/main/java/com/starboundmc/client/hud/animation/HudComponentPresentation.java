// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.animation;

/** One short establishment event per appearance. Opacity/safety holds remain owned by their existing fades. */
public final class HudComponentPresentation {
    public enum Kind {
        NAVIGATION(.52, -.8F), TELEMETRY(.24, .65F), CONTROLS(.3, -.45F);

        private final double duration;
        private final float offset;

        Kind(double duration, float offset) { this.duration = duration; this.offset = offset; }
    }

    private static final boolean DISABLED = Boolean.getBoolean("starboundmc.debug.hudNoTransitions");
    private final Kind kind;
    private double age;
    private boolean visible;

    public HudComponentPresentation(Kind kind) { this.kind = kind; }

    public void update(double seconds, boolean showing) {
        if (!showing) {
            visible = false;
            age = 0;
            return;
        }
        if (!visible) age = 0;
        visible = true;
        if (Double.isFinite(seconds) && seconds > 0)
            age = Math.min(kind.duration, age + seconds);
    }

    /** Finish transient establishment after an interrupted view; never replay a scan because of F1. */
    public void settle() { if (visible) age = kind.duration; }

    public float progress() { return DISABLED ? 1 : (float) (age / kind.duration); }

    public float offsetY() {
        float remaining = 1 - progress();
        return kind.offset * remaining * remaining * remaining;
    }

    public float glow() {
        float p = progress();
        return .08F + .035F * 4 * p * (1 - p);
    }

    /** Local reveal follows distance from the current heading, never the absolute compass bearing. */
    public static float compassReveal(float progress, float distanceFromCenter) {
        float delay = .12F + .5F * Math.clamp(distanceFromCenter, 0F, 1F);
        return smooth((progress - delay) / .38F);
    }

    public static float smooth(float value) {
        float p = Math.clamp(value, 0F, 1F);
        return p * p * (3 - 2 * p);
    }
}
