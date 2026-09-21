// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

/** Clip-space to flat HUD coordinates, including off-screen targets and targets behind the camera. */
public final class ArTargetProjection {
    private ArTargetProjection() { }

    public record Point(float x, float y, boolean edge, boolean behind) { }

    public static Point project(float x, float y, float w, int width, int height) {
        boolean behind = w <= 0;
        float divisor = Math.max(.001F, Math.abs(w));
        float dx = x / divisor * width / 2F;
        float dy = -y / divisor * height / 2F;
        float rx = Math.max(10, width / 2F - 28);
        float ry = Math.max(10, height / 2F - 65);
        boolean edge = behind || Math.abs(dx) > rx || Math.abs(dy) > ry;
        if (edge) {
            if (Math.abs(dx) + Math.abs(dy) < .001F)
                dx = rx;
            float factor = Math.max(Math.abs(dx) / rx, Math.abs(dy) / ry);
            dx /= factor;
            dy /= factor;
        }
        return new Point(width / 2F + dx, height / 2F + dy, edge, behind);
    }
}
