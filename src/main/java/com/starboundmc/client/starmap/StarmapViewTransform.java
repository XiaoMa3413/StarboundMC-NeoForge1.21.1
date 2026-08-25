package com.starboundmc.client.starmap;

/**
 * Shared world-to-screen transform for the interactive starmap scene.
 *
 * <p>The authored scene uses the unscaled viewport as its world space. Zoom
 * is performed around the viewport centre and {@code offsetX/Y} are screen
 * pixel offsets applied afterwards. Keeping this math outside rendering also
 * lets hit testing, selection brackets and floating panels use the exact same
 * coordinates.</p>
 */
final class StarmapViewTransform {
    static final float MIN_SCALE = 0.75F;
    static final float MAX_SCALE = 2.25F;
    static final float FOCUS_SCALE = 1.35F;
    private static final float ZOOM_FACTOR = 1.15F;
    private static final float EPSILON = 0.0001F;

    private float scale = 1.0F;
    private float offsetX;
    private float offsetY;

    Point toScreen(float worldX, float worldY, float viewportWidth, float viewportHeight) {
        float centerX = viewportWidth * 0.5F;
        float centerY = viewportHeight * 0.5F;
        return new Point(centerX + (worldX - centerX) * scale + offsetX,
                centerY + (worldY - centerY) * scale + offsetY);
    }

    Point toWorld(float screenX, float screenY, float viewportWidth, float viewportHeight) {
        float centerX = viewportWidth * 0.5F;
        float centerY = viewportHeight * 0.5F;
        return new Point(centerX + (screenX - centerX - offsetX) / scale,
                centerY + (screenY - centerY - offsetY) / scale);
    }

    /** Zooms while keeping the world point under the mouse stationary. */
    boolean zoomAt(float wheelDelta, float anchorX, float anchorY,
                   float viewportWidth, float viewportHeight) {
        if (Math.abs(wheelDelta) < EPSILON)
            return false;
        float nextScale = clamp((float) (scale * Math.pow(ZOOM_FACTOR, wheelDelta)),
                MIN_SCALE, MAX_SCALE);
        if (Math.abs(nextScale - scale) < EPSILON)
            return false;

        Point anchorWorld = toWorld(anchorX, anchorY, viewportWidth, viewportHeight);
        scale = nextScale;
        float centerX = viewportWidth * 0.5F;
        float centerY = viewportHeight * 0.5F;
        offsetX = anchorX - centerX - (anchorWorld.x - centerX) * scale;
        offsetY = anchorY - centerY - (anchorWorld.y - centerY) * scale;
        constrain(viewportWidth, viewportHeight);
        return true;
    }

    boolean panBy(float deltaX, float deltaY, float viewportWidth, float viewportHeight) {
        return setOffset(offsetX + deltaX, offsetY + deltaY, viewportWidth, viewportHeight);
    }

    boolean setOffset(float x, float y, float viewportWidth, float viewportHeight) {
        float previousX = offsetX;
        float previousY = offsetY;
        offsetX = x;
        offsetY = y;
        constrain(viewportWidth, viewportHeight);
        return Math.abs(previousX - offsetX) >= EPSILON
                || Math.abs(previousY - offsetY) >= EPSILON;
    }

    /** Centres an authored world point and applies a visible minimum focus zoom. */
    boolean focus(float worldX, float worldY, float viewportWidth, float viewportHeight) {
        boolean scaleChanged = scale < FOCUS_SCALE;
        if (scaleChanged)
            scale = FOCUS_SCALE;
        float centerX = viewportWidth * 0.5F;
        float centerY = viewportHeight * 0.5F;
        boolean offsetChanged = setOffset(-(worldX - centerX) * scale,
                -(worldY - centerY) * scale, viewportWidth, viewportHeight);
        return scaleChanged || offsetChanged;
    }

    void constrain(float viewportWidth, float viewportHeight) {
        float normalizedZoom = (scale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
        float limitRatio = 0.35F + clamp(normalizedZoom, 0.0F, 1.0F) * 0.55F;
        float limitX = Math.max(0.0F, viewportWidth) * limitRatio;
        float limitY = Math.max(0.0F, viewportHeight) * limitRatio;
        offsetX = clamp(offsetX, -limitX, limitX);
        offsetY = clamp(offsetY, -limitY, limitY);
    }

    boolean reset() {
        boolean changed = !isReset();
        scale = 1.0F;
        offsetX = 0.0F;
        offsetY = 0.0F;
        return changed;
    }

    boolean isReset() {
        return Math.abs(scale - 1.0F) < EPSILON
                && Math.abs(offsetX) < EPSILON
                && Math.abs(offsetY) < EPSILON;
    }

    float scale() {
        return scale;
    }

    float offsetX() {
        return offsetX;
    }

    float offsetY() {
        return offsetY;
    }

    float scaleLength(float length) {
        return length * scale;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    record Point(float x, float y) {}
}
