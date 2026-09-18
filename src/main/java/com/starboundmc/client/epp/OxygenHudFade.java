package com.starboundmc.client.epp;

/** Animation clock is supplied by the HUD so pauses do not consume the safety hold. */
final class OxygenHudFade {
    private float opacity, quietSeconds;
    float update(float seconds, boolean needed) {
        seconds = Math.clamp(seconds, 0, .1f);
        quietSeconds = needed ? 0 : quietSeconds + seconds;
        float target = needed || quietSeconds < 1.5f ? 1 : 0;
        opacity = target > opacity ? Math.min(target, opacity + seconds / .15f)
                : Math.max(target, opacity - seconds / .8f);
        return opacity;
    }
}
