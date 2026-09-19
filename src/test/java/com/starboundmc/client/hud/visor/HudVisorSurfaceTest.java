// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudVisorSurfaceTest {
    @Test
    void centerIsPlanarAndEdgeCompressionStaysLight() {
        assertEquals(new HudVisorSurface.Point(0, 0), HudVisorSurface.project(0, 0));
        var horizontal = HudVisorSurface.project(.9F, 0);
        var vertical = HudVisorSurface.project(0, .9F);
        assertTrue(.9F - horizontal.x() > .9F - vertical.y());
        assertTrue(.9F - horizontal.x() < .012F);
        assertTrue(.9F - vertical.y() < .003F);
    }

    @Test
    void horizontalBaselinesDoNotBowOrTilt() {
        var compassCenter = HudVisorSurface.projectGui(427, 18, 854, 480);
        var compassEdge = HudVisorSurface.projectGui(563, 18, 854, 480);
        assertEquals(compassCenter.y(), compassEdge.y(), .0001F);

        var survivalLeft = HudVisorSurface.projectGui(714, 398, 854, 480);
        var survivalRight = HudVisorSurface.projectGui(842, 398, 854, 480);
        assertEquals(survivalLeft.y(), survivalRight.y(), .0001F);
        float projectedWidth = survivalRight.x() - survivalLeft.x();
        assertTrue(projectedWidth > 123F && projectedWidth < 127F);
    }

    @Test
    void globalEdgeSamplingRemainsViewportStable() {
        var direct = HudVisorSurface.project(.72F, .66F);
        var gui = HudVisorSurface.projectGui(688, 498, 800, 600);
        assertEquals((direct.x() + 1) * 400, gui.x(), .0001);
        assertEquals((direct.y() + 1) * 300, gui.y(), .0001);
    }

    @Test
    void normalizedShapeIsIndependentOfGuiDimensions() {
        var small = HudVisorSurface.projectGui(180, 90, 240, 120);
        var large = HudVisorSurface.projectGui(1440, 720, 1920, 960);
        assertEquals(small.x() / 240, large.x() / 1920, .0001);
        assertEquals(small.y() / 120, large.y() / 960, .0001);
    }

    @Test
    void deformationStaysMonotonicAcrossTheWholeViewport() {
        for (int yi = -10; yi < 10; yi++) {
            for (int xi = -10; xi < 10; xi++) {
                float x = xi / 10F;
                float y = yi / 10F;
                var a = HudVisorSurface.project(x, y);
                var right = HudVisorSurface.project(x + .1F, y);
                var down = HudVisorSurface.project(x, y + .1F);
                assertTrue(right.x() > a.x());
                assertTrue(down.y() > a.y());
                float determinant = (right.x() - a.x()) * (down.y() - a.y())
                        - (right.y() - a.y()) * (down.x() - a.x());
                assertTrue(determinant > 0);
            }
        }
    }

    @Test
    void edgeFadeRemainsSubtleAndSymmetric() {
        assertEquals(1F, HudVisorSurface.edgeFade(0, 0));
        assertEquals(HudVisorSurface.edgeFade(-.9F, .4F),
                HudVisorSurface.edgeFade(.9F, .4F), .0001);
        for (int y = -10; y <= 10; y++) {
            for (int x = -10; x <= 10; x++) {
                float fade = HudVisorSurface.edgeFade(x / 10F, y / 10F);
                assertTrue(fade >= .80F && fade <= 1F);
            }
        }
    }
}
