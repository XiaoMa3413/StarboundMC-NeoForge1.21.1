package com.starboundmc.client.starmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StarmapViewTransformTest {
    private static final float EPSILON = 0.001F;

    @Test
    void screenAndWorldCoordinatesRoundTrip() {
        StarmapViewTransform transform = new StarmapViewTransform();
        transform.zoomAt(3.0F, 420.0F, 180.0F, 800.0F, 500.0F);
        transform.panBy(-53.0F, 28.0F, 800.0F, 500.0F);

        var screen = transform.toScreen(123.5F, 321.25F, 800.0F, 500.0F);
        var world = transform.toWorld(screen.x(), screen.y(), 800.0F, 500.0F);

        assertEquals(123.5F, world.x(), EPSILON);
        assertEquals(321.25F, world.y(), EPSILON);
    }

    @Test
    void wheelZoomKeepsTheMouseAnchorStationary() {
        StarmapViewTransform transform = new StarmapViewTransform();
        float anchorX = 615.0F;
        float anchorY = 142.0F;
        var worldBefore = transform.toWorld(anchorX, anchorY, 800.0F, 500.0F);

        assertTrue(transform.zoomAt(2.0F, anchorX, anchorY, 800.0F, 500.0F));
        var screenAfter = transform.toScreen(worldBefore.x(), worldBefore.y(), 800.0F, 500.0F);

        assertEquals(anchorX, screenAfter.x(), EPSILON);
        assertEquals(anchorY, screenAfter.y(), EPSILON);
    }

    @Test
    void zoomIsClampedAtBothLimits() {
        StarmapViewTransform transform = new StarmapViewTransform();

        transform.zoomAt(100.0F, 400.0F, 250.0F, 800.0F, 500.0F);
        assertEquals(StarmapViewTransform.MAX_SCALE, transform.scale(), EPSILON);
        assertFalse(transform.zoomAt(1.0F, 400.0F, 250.0F, 800.0F, 500.0F));

        transform.zoomAt(-100.0F, 400.0F, 250.0F, 800.0F, 500.0F);
        assertEquals(StarmapViewTransform.MIN_SCALE, transform.scale(), EPSILON);
        assertFalse(transform.zoomAt(-1.0F, 400.0F, 250.0F, 800.0F, 500.0F));
    }

    @Test
    void panCannotEscapeTheViewportBound() {
        StarmapViewTransform transform = new StarmapViewTransform();

        transform.panBy(10_000.0F, -10_000.0F, 800.0F, 500.0F);

        assertTrue(transform.offsetX() <= 800.0F * 0.9F);
        assertTrue(transform.offsetY() >= -500.0F * 0.9F);
    }

    @Test
    void focusCentresAWorldPointAndResetRestoresDefaults() {
        StarmapViewTransform transform = new StarmapViewTransform();
        transform.zoomAt(2.0F, 400.0F, 250.0F, 800.0F, 500.0F);

        assertTrue(transform.focus(180.0F, 160.0F, 800.0F, 500.0F));
        var focused = transform.toScreen(180.0F, 160.0F, 800.0F, 500.0F);
        assertEquals(StarmapViewTransform.FOCUS_SCALE, transform.scale(), EPSILON);
        assertEquals(400.0F, focused.x(), EPSILON);
        assertEquals(250.0F, focused.y(), EPSILON);

        assertTrue(transform.reset());
        assertTrue(transform.isReset());
        assertFalse(transform.reset());
    }
}
