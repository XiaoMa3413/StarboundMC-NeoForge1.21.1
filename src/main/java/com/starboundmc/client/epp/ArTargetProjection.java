package com.starboundmc.client.epp;

/** Clip-space to HUD coordinates, including targets behind the camera. */
final class ArTargetProjection {
    record Point(float x, float y, boolean edge, boolean behind) { }
    static Point project(float x, float y, float w, int width, int height) {
        boolean behind = w <= 0;
        float divisor = Math.max(.001f, Math.abs(w));
        float dx = x / divisor * width / 2f;
        float dy = -y / divisor * height / 2f;
        float rx = Math.max(10, width / 2f - 28), ry = Math.max(10, height / 2f - 65);
        boolean edge = behind || Math.abs(dx) > rx || Math.abs(dy) > ry;
        if (edge) {
            if (Math.abs(dx) + Math.abs(dy) < .001f) dx = rx;
            float factor = Math.max(Math.abs(dx) / rx, Math.abs(dy) / ry);
            dx /= factor; dy /= factor;
        }
        return new Point(width / 2f + dx, height / 2f + dy, edge, behind);
    }
}
