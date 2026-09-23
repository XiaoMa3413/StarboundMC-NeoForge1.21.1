// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

/** Small reusable occupancy buffer. Only text moves; callers keep the projected marker untouched. */
final class ArLabelLayout {
    private final float[] xs = new float[ArTargetCollector.MAX_TARGETS];
    private final float[] ys = new float[ArTargetCollector.MAX_TARGETS];
    private final float[] halfWidths = new float[ArTargetCollector.MAX_TARGETS];
    private int count;
    private float x, y;

    void clear() { count = 0; }
    float x() { return x; }
    float y() { return y; }

    /** Labels arrive in priority order. If no readable slot fits, keep only the lower-priority marker. */
    boolean place(float anchorX, float anchorY, float halfWidth, int width, int height) {
        if (count == xs.length || width < halfWidth * 2 + 16 || height < 22) return false;
        x = Math.clamp(anchorX, halfWidth + 8, width - halfWidth - 8);
        float baseY = Math.clamp(anchorY + 10, 8, height - 14);
        for (int attempt = 0; attempt <= xs.length * 2; attempt++) {
            int step = (attempt + 1) / 2;
            float offset = attempt % 2 == 1 ? step * 11 : -step * 11;
            float candidate = baseY + offset;
            if (candidate < 8 || candidate > height - 14) continue;
            boolean occupied = false;
            for (int i = 0; i < count; i++) {
                if (Math.abs(x - xs[i]) < halfWidth + halfWidths[i] + 4
                        && Math.abs(candidate - ys[i]) < 10) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) continue;
            y = candidate;
            xs[count] = x; ys[count] = y; halfWidths[count++] = halfWidth;
            return true;
        }
        return false;
    }
}
