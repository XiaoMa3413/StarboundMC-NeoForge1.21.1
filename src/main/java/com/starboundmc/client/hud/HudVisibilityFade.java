package com.starboundmc.client.hud;

/** No safety hold: navigation follows entering and leaving EVA immediately. */
final class HudVisibilityFade {
    private float opacity;

    float update(float seconds, boolean visible) {
        seconds = Math.clamp(seconds, 0, .1f);
        opacity = visible ? Math.min(1, opacity + seconds / .25f)
                : Math.max(0, opacity - seconds / .4f);
        return opacity;
    }
}
