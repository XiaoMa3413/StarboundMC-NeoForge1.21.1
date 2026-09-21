// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

import org.junit.jupiter.api.Test;

import static com.starboundmc.client.hud.visor.HudVisorGeometry.*;
import static com.starboundmc.client.hud.visor.HudVisorGeometry.Profile.*;
import static org.junit.jupiter.api.Assertions.*;

class HudVisorGeometryTest {
    private static final float[][] VIEWPORTS = {
            {320, 180}, {480, 270}, {640, 480}, {960, 540}, {1280, 360}
    };

    @Test
    void sharedBranchesAreSymmetricAndStayGentleAcrossAspectRatios() {
        for (float[] viewport : VIEWPORTS) {
            float w = viewport[0], h = viewport[1];
            assertEquals(0, curveOffset(SURVIVAL, w / 2, w, h), .00001);
            assertEquals(0, curveSlope(COMPASS, w / 2, w, h), .00001);
            for (int x = 0; x <= w; x++) {
                float lower = curveOffset(SURVIVAL, x, w, h);
                assertTrue(lower >= 0);
                assertEquals(lower, curveOffset(SURVIVAL, w - x, w, h), .00001);
                assertEquals(-lower, curveOffset(COMPASS, x, w, h), .00001);
                assertEquals(-lower, curveOffset(EVA_CONTROLS, x, w, h), .00001);
                assertTrue(Math.abs(Math.toDegrees(Math.atan(curveSlope(SURVIVAL, x, w, h)))) < 6);
            }
            assertTrue(curveOffset(SURVIVAL, 0, w, h) < h * .048);
        }
    }

    @Test
    void splittingArtworkIntoAdjacentComponentsDoesNotRestartTheCurve() {
        // Glyph normals must join as well as the centerline.
        for (var profile : new Profile[]{COMPASS, SURVIVAL}) {
            var whole = new Placement(480, 270, 240, 1, 1);
            for (int part = 0; part < 2; part++) {
                var half = new Placement(480, 270, 120 + part * 240, 1, 1);
                for (int u = 0; u <= 240; u += 4) {
                    for (int v = 0; v <= 48; v += 4) {
                        var a = project(profile, u + part * 240, v, 480, 48, whole);
                        var b = project(profile, u, v, 240, 48, half);
                        assertEquals(whole.centerX() + a.x(), half.centerX() + b.x(), .0001);
                        assertEquals(a.y(), b.y(), .0001);
                    }
                }
            }
        }
    }

    @Test
    void compassAndEvaShareTheSameScreenSectionRegardlessOfArtworkWidth() {
        var placement = new Placement(480, 270, 240, 1, 1);
        for (int offset = -136; offset <= 136; offset++) {
            for (int v : new int[]{0, 12, 24, 48}) {
                assertEquals(project(COMPASS, 136 + offset, v, 272, 48, placement),
                        project(EVA_CONTROLS, 180 + offset, v, 360, 48, placement));
            }
        }
    }

    @Test
    void horizontalPlacementSelectsTheCorrespondingCurveSection() {
        var left = new Placement(480, 270, 80, 1, 1);
        var center = new Placement(480, 270, 240, 1, 1);
        var right = new Placement(480, 270, 400, 1, 1);
        assertTrue(project(SURVIVAL, 122, 24, 128, 48, left).y()
                < project(SURVIVAL, 6, 24, 128, 48, left).y());
        assertEquals(project(SURVIVAL, 6, 24, 128, 48, center).y(),
                project(SURVIVAL, 122, 24, 128, 48, center).y(), .00001);
        float drop = project(SURVIVAL, 122, 24, 128, 48, right).y()
                - project(SURVIVAL, 6, 24, 128, 48, right).y();
        // About 8 px across the bar; the rejected local curve dropped 16.4 px.
        assertTrue(drop > 7 && drop < 9);
        for (int u = 6; u <= 122; u++) {
            double angle = Math.toDegrees(Math.atan(curveSlope(SURVIVAL, 336 + u, 480, 270)));
            assertTrue(angle > 2 && angle < 5.5);
        }
    }

    @Test
    void scalingHappensBeforeSamplingAndGlyphNormalThicknessIsPreserved() {
        var scaled = new Placement(480, 270, 400, .75F, .6F);
        var nativeSize = new Placement(480, 270, 400, 1, 1);
        for (int u = 0; u <= 128; u++) {
            var top = project(SURVIVAL, u, 0, 128, 48, scaled);
            var bottom = project(SURVIVAL, u, 48, 128, 48, scaled);
            assertEquals(48 * .6, Math.hypot(bottom.x() - top.x(), bottom.y() - top.y()), .00002);
            var resized = project(SURVIVAL, u, 24, 128, 48, scaled);
            var sameScreenPoint = project(SURVIVAL, 64 + (u - 64) * .75F, 24, 128, 48, nativeSize);
            assertEquals(sameScreenPoint, resized);
        }
        assertTrue(project(SURVIVAL, 64, 9, 128, 48, scaled).x() > 0);
    }

    @Test
    void paddedMeshHasNoCollapsedOrFlippedTriangles() {
        for (float[] viewport : VIEWPORTS) {
            for (var profile : Profile.values()) {
                int width = profile == COMPASS ? 272 : profile == EVA_CONTROLS ? 360 : 128;
                float scale = Math.min(1, (viewport[0] - 16) / width);
                var placement = new Placement(viewport[0], viewport[1],
                        profile == SURVIVAL ? viewport[0] - 80 : viewport[0] / 2, scale, scale);
                for (int u = -2; u < width + 2; u += 2) {
                    for (int v = -2; v < 50; v += 2) {
                        var a = project(profile, u, v, width, 48, placement);
                        var b = project(profile, u, v + 2, width, 48, placement);
                        var c = project(profile, u + 2, v + 2, width, 48, placement);
                        var d = project(profile, u + 2, v, width, 48, placement);
                        assertTrue(cross(a, b, c) < -1, profile + " first triangle");
                        assertTrue(cross(a, c, d) < -1, profile + " second triangle");
                    }
                }
            }
        }
    }

    @Test
    void survivalRowsPreserveReadingHeightAndClearViewportEdges() {
        for (float[] viewport : VIEWPORTS) {
            float width = viewport[0], height = viewport[1];
            for (var profile : new Profile[]{SURVIVAL, FLAT}) {
                for (int rows = 1; rows <= 3; rows++) {
                    for (int row = 0; row < rows; row++) {
                        var anchor = survivalAnchor(width, height, rows, row, profile);
                        var placement = new Placement(width, height, anchor.x(), 1, 1);
                        var text = project(profile, 64, 9, 128, 48, placement);
                        float oldReadingY = height - 101.33F - Math.max(0, rows - 2) * 40 + row * 40;
                        assertEquals(oldReadingY, anchor.y() + text.y(), .2);
                        for (int u = -2; u <= 130; u += 2) {
                            for (int v = -2; v <= 50; v += 2) {
                                var point = project(profile, u, v, 128, 48, placement);
                                // Include padding, max camera lag and halo radius.
                                assertTrue(anchor.x() + point.x() - 1.8 - .7 > 0);
                                assertTrue(anchor.x() + point.x() + 1.8 + .7 < width);
                                assertTrue(anchor.y() + point.y() - 1.2 - .7 > 0);
                                assertTrue(anchor.y() + point.y() + 1.2 + .7 < height);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void onlySurvivalHasAdditionalEdgeFade() {
        assertEquals(.72, opacity(SURVIVAL, 6), .00001);
        assertEquals(1, opacity(SURVIVAL, 64), .00001);
        assertEquals(.72, opacity(SURVIVAL, 122), .00001);
        for (int u = -2; u <= 362; u++) {
            assertEquals(1, opacity(COMPASS, u));
            assertEquals(1, opacity(EVA_CONTROLS, u));
            assertEquals(1, opacity(FLAT, u));
        }
    }

    @Test
    void flatComparisonIsPlanarAtEveryPlacementAndScale() {
        for (var profile : Profile.values()) {
            assertEquals(profile, comparison(profile, "screen"));
            assertEquals(FLAT, comparison(profile, "flat"));
        }
        for (int center : new int[]{80, 240, 400}) {
            var placement = new Placement(480, 270, center, .75F, .6F);
            for (int u = 0; u <= 128; u += 4) {
                for (int v = 0; v <= 48; v += 4) {
                    var point = project(FLAT, u, v, 128, 48, placement);
                    assertEquals((u - 64) * .75F, point.x(), .00001);
                    assertEquals((v - 24) * .6F, point.y(), .00001);
                }
            }
        }
    }

    private static double cross(Point a, Point b, Point c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }
}
