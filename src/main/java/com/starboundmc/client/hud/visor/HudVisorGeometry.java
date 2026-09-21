// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

/** Two screen-wide reference curves, sampled by independently rendered HUD components. */
public final class HudVisorGeometry {
    public enum Profile {
        FLAT, COMPASS, EVA_CONTROLS, SURVIVAL
    }

    public record Point(float x, float y) { }

    /** Cache key deliberately excludes vertical stacking and camera lag. Coordinates are GUI pixels. */
    public record Placement(float guiWidth, float guiHeight, float centerX, float scaleX, float scaleY) { }

    private static final double REFERENCE_ASPECT = 16D / 9;
    private static final double HALF_HEIGHT = .4;

    private HudVisorGeometry() { }

    /** Vertical displacement from the reference branch's height at the screen center. */
    public static float curveOffset(Profile profile, float screenX, float guiWidth, float guiHeight) {
        if (profile == Profile.FLAT) return 0;
        double radius = Math.max(guiWidth, guiHeight * REFERENCE_ASPECT);
        double t = (screenX - guiWidth / 2D) / radius;
        double offset = guiHeight * HALF_HEIGHT * (Math.sqrt(1 + t * t) - 1);
        return (float) (profile == Profile.SURVIVAL ? offset : -offset);
    }

    public static float curveSlope(Profile profile, float screenX, float guiWidth, float guiHeight) {
        if (profile == Profile.FLAT) return 0;
        double radius = Math.max(guiWidth, guiHeight * REFERENCE_ASPECT);
        double t = (screenX - guiWidth / 2D) / radius;
        double slope = guiHeight * HALF_HEIGHT / radius * t / Math.sqrt(1 + t * t);
        return (float) (profile == Profile.SURVIVAL ? slope : -slope);
    }

    /**
     * Map native artwork onto its actual section of the screen-wide branch.
     * The returned X is relative to placement.centerX; Y is relative to the branch's central height.
     * Do not subtract the curve at the component center: adjacent components must share one curve.
     */
    public static Point project(Profile profile, float u, float v, float width, float height,
                                Placement placement) {
        float x = (u - width / 2) * placement.scaleX;
        float offset = (v - height / 2) * placement.scaleY;
        float screenX = placement.centerX + x;
        double slope = curveSlope(profile, screenX, placement.guiWidth, placement.guiHeight);
        double length = Math.hypot(1, slope);
        return new Point((float) (x - offset * slope / length),
                curveOffset(profile, screenX, placement.guiWidth, placement.guiHeight)
                        + (float) (offset / length));
    }

    public static float opacity(Profile profile, float u) {
        if (profile != Profile.SURVIVAL)
            return 1;
        float floor = .72F;
        double t = Math.clamp((u - 6) / 116D, 0, 1);
        return floor + (1 - floor) * (float) Math.sin(Math.PI * t);
    }

    /**
     * Layout X is the strip's center. Layout Y is the reference curve's central height.
     * Compensate here, once, to keep the right-hand reading height while using the full curve in rendering.
     */
    public static Point survivalAnchor(float guiWidth, float guiHeight, int rows, int row) {
        return survivalAnchor(guiWidth, guiHeight, rows, row, Profile.SURVIVAL);
    }

    public static Point survivalAnchor(float guiWidth, float guiHeight, int rows, int row, Profile profile) {
        if (rows < 1 || rows > 3 || row < 0 || row >= rows)
            throw new IllegalArgumentException("Expected 1-3 survival rows and a visible row index");
        float x = guiWidth - 80;
        return new Point(x, guiHeight - 86.5F - Math.max(0, rows - 2) * 40 + row * 40
                - curveOffset(profile, x, guiWidth, guiHeight));
    }

    /** Geometry-only comparisons. Existing artwork, lifecycle and movement are retained. */
    public static Profile comparison(Profile requested, String mode) {
        return "flat".equals(mode) ? Profile.FLAT : requested;
    }
}
