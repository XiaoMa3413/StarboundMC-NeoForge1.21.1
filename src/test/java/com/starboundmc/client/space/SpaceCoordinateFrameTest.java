package com.starboundmc.client.space;

import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpaceCoordinateFrameTest
{
    private static final double EPSILON = 1.0E-12;

    @Test
    void yawUsesTheExistingNegativeHeadingRotation()
    {
        assertVector(frame(0.0, 0.0, 0.0).toView(UniversePosition.of(1.0, 0.0, 0.0)),
                1.0, 0.0, 0.0);
        assertVector(frame(90.0, 0.0, 0.0).toView(UniversePosition.of(1.0, 0.0, 0.0)),
                0.0, 0.0, 1.0);
        assertVector(frame(180.0, 0.0, 0.0).toView(UniversePosition.of(1.0, 0.0, 0.0)),
                -1.0, 0.0, 0.0);
    }

    @Test
    void pitchSupportsPositiveAndNegativeShipAttitudes()
    {
        assertVector(frame(0.0, 0.0, 0.0).toView(UniversePosition.of(0.0, 1.0, 0.0)),
                0.0, 1.0, 0.0);

        double half = Math.sqrt(0.75);
        assertVector(frame(0.0, 30.0, 0.0).toView(UniversePosition.of(0.0, 1.0, 0.0)),
                0.0, half, -0.5);
        assertVector(frame(0.0, -30.0, 0.0).toView(UniversePosition.of(0.0, 1.0, 0.0)),
                0.0, half, 0.5);
    }

    @Test
    void rollDoesNotChangeUniversePointTransforms()
    {
        UniversePosition point = UniversePosition.of(37.0, -12.0, 91.0);
        Vec3 unbanked = frame(47.0, -19.0, 0.0).toView(point);
        Vec3 banked = frame(47.0, -19.0, 63.0).toView(point);

        assertVector(banked, unbanked.x, unbanked.y, unbanked.z);
    }

    @Test
    void repeatedUniversePositionTransformsAreStable()
    {
        SpaceCoordinateFrame frame = frame(-125.0, 23.0, 31.0);
        UniversePosition point = UniversePosition.of(105_000.25, 480.0, -206_000.75);

        Vec3 first = frame.toView(point);
        Vec3 second = frame.toView(point);

        assertEquals(first, second);
    }

    private static SpaceCoordinateFrame frame(double yaw, double pitch, double roll)
    {
        SpaceRenderContext context = new SpaceRenderContext(Vec3.ZERO,
                UniversePosition.fromLegacy(Vec3.ZERO), Vec3.ZERO, yaw, pitch, roll,
                FlightPhase.DOCKED, false, 0.0F, 1, null, null,
                null, null, 0.0F);
        return new SpaceCoordinateFrame(context);
    }

    private static void assertVector(Vec3 actual, double x, double y, double z)
    {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }
}
