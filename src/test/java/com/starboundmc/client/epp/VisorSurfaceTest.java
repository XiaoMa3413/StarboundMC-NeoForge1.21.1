// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VisorSurfaceTest {
    @Test void curvedBaselineHasExpectedAnchors() {
        var left = VisorSurface.project(6, 24);
        var right = VisorSurface.project(122, 24);
        assertEquals(0, left.x(), 0.0001);
        assertEquals(0, left.y(), 0.0001);
        assertEquals(116, right.x(), 0.0001);
        assertEquals(-18, right.y(), 0.0001);
    }

    @Test void deformationBendsInsideAGlyphInsteadOfOnlyRotatingIt() {
        var left = VisorSurface.project(58, 9);
        var middle = VisorSurface.project(64, 9);
        var right = VisorSurface.project(70, 9);
        double cross = (middle.x() - left.x()) * (right.y() - left.y())
                - (middle.y() - left.y()) * (right.x() - left.x());
        assertTrue(Math.abs(cross) > 0.1, "A straight glyph row must become curved");
    }

    @Test void meshDoesNotFoldOrFlipAcrossTexture() {
        for (int y = 0; y < VisorSurface.HEIGHT; y += 2) {
            for (int x = 0; x < VisorSurface.WIDTH; x += 2) {
                var a = VisorSurface.project(x, y);
                var b = VisorSurface.project(x + 2, y);
                var c = VisorSurface.project(x, y + 2);
                var d = VisorSurface.project(x + 2, y + 2);
                assertTrue(b.x() > a.x());
                assertTrue(c.y() > a.y());
                assertTrue((b.x()-a.x())*(c.y()-a.y())-(b.y()-a.y())*(c.x()-a.x()) > 0);
                assertTrue((c.x()-d.x())*(b.y()-d.y())-(c.y()-d.y())*(b.x()-d.x()) > 0);
            }
        }
    }

    @Test void fadingNeverErasesWarningAtEitherEdge() {
        for (int x = 0; x <= VisorSurface.WIDTH; x++) {
            assertTrue(VisorSurface.fade(x) >= 0.55f);
            assertTrue(VisorSurface.fade(x) <= 1f);
        }
    }
}
