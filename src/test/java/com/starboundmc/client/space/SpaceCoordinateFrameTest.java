package com.starboundmc.client.space;

import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Random;

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

    @Test
    void relativeTransformMatchesLegacyYawThenPitchMathForSeededInputs()
    {
        Random random = new Random(20260924L);
        for (int sample = 0; sample < 512; sample++)
        {
            double x = randomCoordinate(random, sample, 0);
            double y = randomCoordinate(random, sample, 1);
            double z = randomCoordinate(random, sample, 2);
            double yaw = random.nextDouble() * 720.0 - 360.0;
            double pitch = random.nextDouble() * 178.0 - 89.0;

            SpaceCoordinateFrame coordinateFrame = new SpaceCoordinateFrame(
                    UniversePosition.of(0.0, 0.0, 0.0), yaw, pitch);
            Vec3 expected = legacyToViewRelative(x, y, z, yaw, pitch);
            Vec3 actual = coordinateFrame.toViewRelative(x, y, z);

            assertClose(expected.x, actual.x, sample, "x");
            assertClose(expected.y, actual.y, sample, "y");
            assertClose(expected.z, actual.z, sample, "z");
        }
    }

    private static double randomCoordinate(Random random, int sample, int axis)
    {
        double magnitude = random.nextDouble();
        double sign = ((sample + axis) & 1) == 0 ? 1.0 : -1.0;
        double scale = switch (Math.floorMod(sample + axis * 3, 8))
        {
            case 0 -> 0.0;
            case 1 -> 1.0E-12;
            case 2 -> 1.0E-6;
            case 3 -> 1.0E-3;
            case 4 -> 1.0;
            case 5 -> 1.0E3;
            case 6 -> 1.0E6;
            default -> 1.0E9;
        };
        return sign * magnitude * scale;
    }

    private static Vec3 legacyToViewRelative(double x, double y, double z,
                                            double yaw, double pitch)
    {
        double yawRadians = Math.toRadians(-yaw);
        double yawCos = Math.cos(yawRadians);
        double yawSin = Math.sin(yawRadians);

        double pitchRadians = Math.toRadians(-pitch);
        double pitchCos = Math.cos(pitchRadians);
        double pitchSin = Math.sin(pitchRadians);

        double viewX = x * yawCos + z * yawSin;
        double yawZ = -x * yawSin + z * yawCos;
        double viewY = y * pitchCos - yawZ * pitchSin;
        double viewZ = y * pitchSin + yawZ * pitchCos;
        return new Vec3(viewX, viewY, viewZ);
    }

    private static void assertClose(double expected, double actual, int sample, String axis)
    {
        double tolerance = Math.max(1.0E-15, Math.abs(expected) * 1.0E-12);
        assertEquals(expected, actual, tolerance,
                "sample " + sample + " differs on " + axis);
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
