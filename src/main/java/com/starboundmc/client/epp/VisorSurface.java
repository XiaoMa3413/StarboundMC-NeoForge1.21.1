// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

/** Continuous visor surface in GUI pixels, shared by every texel of the HUD. */
final class VisorSurface {
    static final int WIDTH = 128, HEIGHT = 48;
    static final float ARC_WIDTH = 116;
    private VisorSurface() { }
    record Point(float x, float y) { }
    /** Gentle symmetric top visor: preserve glyph aspect ratio and limit slope to 0.1. */
    static Point navigation(float u, float v, int width) {
        float t = (u - width / 2f) / (width / 2f);
        return new Point(u, v - width * .025f * t * t);
    }
    static Point project(float u, float v) {
        float t = (u - 6) / ARC_WIDTH;
        float offset = v - 24;
        // Cylindrical horizontal foreshortening plus an upward visor curvature.
        float angle = 0.55f;
        float sine = (float) Math.sin(angle);
        float x = ARC_WIDTH * (float) Math.sin(angle * t) / sine;
        float dx = angle * (float) Math.cos(angle * t) / sine;
        float dy = -36 * t / ARC_WIDTH;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        return new Point(x - offset * dy / length, -18 * t * t + offset * dx / length);
    }
    static float fade(float u) {
        float t = Math.clamp((u - 6) / ARC_WIDTH, 0f, 1f);
        return 0.55f + 0.45f * (float) Math.sin(Math.PI * t);
    }
}
