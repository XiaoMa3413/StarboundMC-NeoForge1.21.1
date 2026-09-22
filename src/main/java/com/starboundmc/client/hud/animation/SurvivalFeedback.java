// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.animation;

/** Truth stays outside this visual state. Worsening is immediate; recovery is the only smoothed direction. */
public final class SurvivalFeedback {
    public enum Kind { OXYGEN, COLD, HEAT }
    private static final boolean DISABLED = Boolean.getBoolean("starboundmc.debug.hudNoTransitions");
    private final Kind kind;
    private boolean initialized;
    private float actual;
    private float displayed;
    private int severity;
    private double alertAge = 60;

    public SurvivalFeedback(Kind kind) { this.kind = kind; }
    public Kind kind() { return kind; }

    public void update(double seconds, float value, int newSeverity) {
        double dt = Double.isFinite(seconds) && seconds > 0 ? seconds : 0;
        actual = Float.isFinite(value) ? Math.clamp(value, 0F, 1F) : 0;
        boolean worsening = kind == Kind.OXYGEN ? actual <= displayed : actual >= displayed;
        if (!initialized || worsening || DISABLED) displayed = actual;
        else {
            double response = kind == Kind.COLD ? .45 : kind == Kind.HEAT ? .3 : .28;
            displayed += (actual - displayed) * (float) -Math.expm1(-dt / response);
            if (Math.abs(actual - displayed) < .0001) displayed = actual;
        }
        newSeverity = Math.clamp(newSeverity, 0, 3);
        if (newSeverity > severity) alertAge = 0;
        else if (newSeverity < severity) alertAge = duration() + .01;
        severity = newSeverity;
        initialized = true;
        alertAge += dt;
    }

    public float displayed() { return displayed; }
    public int severity() { return severity; }
    public float glow() { return .08F + .055F * pulse(); }

    public float pulse() {
        if (DISABLED || severity == 0) return 0;
        double age = severity >= 2 ? alertAge % (severity == 3 ? 2.8 : 4.5) : alertAge;
        double duration = duration();
        if (age >= duration) return 0;
        return HudComponentPresentation.smooth((float) (age / .12))
                * (1 - HudComponentPresentation.smooth((float) ((age - .12) / (duration - .12))));
    }

    /** Hidden/camera-change handling discards transient movement and the remainder of a cue. */
    public void settle() {
        if (!initialized) return;
        displayed = actual;
        alertAge = duration() + .01;
    }

    private double duration() { return kind == Kind.COLD ? .7 : kind == Kind.HEAT ? .5 : .55; }
}
