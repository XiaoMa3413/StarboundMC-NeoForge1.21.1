package com.starboundmc.client.space;

import org.joml.Matrix4fc;

/** Optical enlargement relative to the user's ordinary field of view. */
public final class StarfieldOptics {
    private StarfieldOptics() {}

    public static float magnification(Matrix4fc projection, double ordinaryFovDegrees) {
        if (!Double.isFinite(ordinaryFovDegrees) || ordinaryFovDegrees <= 0 || ordinaryFovDegrees >= 180)
            return 1;
        // The row length also retains the focal scale after a hurt-camera rotation.
        double focalScale = Math.sqrt((double)projection.m01()*projection.m01()
                + (double)projection.m11()*projection.m11() + (double)projection.m21()*projection.m21());
        double zoom = focalScale*Math.tan(Math.toRadians(ordinaryFovDegrees)*.5);
        // Bound extreme third-party zoom without changing vanilla spyglass magnification.
        return Double.isFinite(zoom) ? (float)Math.clamp(zoom, 1, 32) : 1;
    }
}
