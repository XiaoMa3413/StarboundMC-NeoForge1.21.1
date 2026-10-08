package com.starboundmc.client.space;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StarfieldOpticsTest {
    @Test void ordinaryFovChoicesAndWideSprintViewRetainTheExistingStarSize() {
        for (double fov : new double[] {30, 70, 110}) {
            assertEquals(1, StarfieldOptics.magnification(projection(fov), fov), 1e-6);
            assertEquals(1, StarfieldOptics.magnification(projection(fov*1.1), fov), 1e-6);
        }
    }

    @Test void spyglassFocalScaleFollowsTheActiveProjectionThroughItsTransition() {
        float zoom = StarfieldOptics.magnification(projection(7), 70);
        assertEquals(11.448, zoom, .002);
        float halfway = StarfieldOptics.magnification(projection(35), 70);
        assertTrue(halfway > 1 && halfway < zoom);
        Matrix4f hurtCamera = projection(7).rotateZ(.2F).rotateX(.1F);
        assertEquals(zoom, StarfieldOptics.magnification(hurtCamera, 70), 1e-5);
    }

    @Test void extremeOrInvalidProjectionDoesNotCreateUnboundedStarQuads() {
        assertEquals(32, StarfieldOptics.magnification(projection(.01), 70));
        assertEquals(1, StarfieldOptics.magnification(new Matrix4f().zero(), 70));
        assertEquals(1, StarfieldOptics.magnification(new Matrix4f().m11(Float.NaN), 70));
        assertEquals(1, StarfieldOptics.magnification(projection(70), Double.NaN));
    }

    private static Matrix4f projection(double fov) {
        return new Matrix4f().perspective((float)Math.toRadians(fov), 16F/9, .05F, 1000);
    }
}
