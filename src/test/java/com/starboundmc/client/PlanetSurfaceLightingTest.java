package com.starboundmc.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanetSurfaceLightingTest
{
    @Test
    void meshSpaceSunPreservesLegacyPerVertexLightingDot()
    {
        Random random = new Random(20260925L);
        for (int sample = 0; sample < 640; sample++)
        {
            Vector3f normal = randomUnitVector(random);
            Vector3f worldSun = randomUnitVector(random);
            float spin = random.nextFloat() * 1440.0F - 720.0F;
            float tilt = random.nextFloat() * 360.0F - 180.0F;
            float yaw = random.nextFloat() * 720.0F - 360.0F;

            Vector3f oldBodyNormal = legacyRotateX(legacyRotateY(normal, yaw), tilt);
            Vector3f oldModelNormal = legacyRotateY(oldBodyNormal, spin);
            Vector3f matrixNormal = new Vector3f(normal);
            PlanetSurfaceLighting.appendBodyOrientation(new Matrix4f(), spin, tilt, yaw)
                    .transformDirection(matrixNormal);
            assertEquals(oldModelNormal.x, matrixNormal.x, 1.0E-4F, "matrix x, sample " + sample);
            assertEquals(oldModelNormal.y, matrixNormal.y, 1.0E-4F, "matrix y, sample " + sample);
            assertEquals(oldModelNormal.z, matrixNormal.z, 1.0E-4F, "matrix z, sample " + sample);

            Vector3f oldSpinCompensatedSun = legacyRotateY(worldSun, -spin);
            float expected = oldBodyNormal.dot(oldSpinCompensatedSun);

            Vector3f meshSpaceSun = PlanetSurfaceLighting.toMeshSpaceSun(
                    worldSun, spin, tilt, yaw);
            float actual = normal.dot(meshSpaceSun);

            assertEquals(expected, actual, 1.0E-4F, "sample " + sample);
        }
    }

    private static Vector3f randomUnitVector(Random random)
    {
        Vector3f vector;
        do
        {
            vector = new Vector3f(random.nextFloat() * 2.0F - 1.0F,
                    random.nextFloat() * 2.0F - 1.0F,
                    random.nextFloat() * 2.0F - 1.0F);
        }
        while (vector.lengthSquared() < 1.0E-5F);
        return vector.normalize();
    }

    private static Vector3f legacyRotateY(Vector3f vector, float degrees)
    {
        float radians = (float) Math.toRadians(degrees);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        return new Vector3f(vector.x * cosine + vector.z * sine, vector.y,
                -vector.x * sine + vector.z * cosine);
    }

    private static Vector3f legacyRotateX(Vector3f vector, float degrees)
    {
        float radians = (float) Math.toRadians(degrees);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        return new Vector3f(vector.x, vector.y * cosine - vector.z * sine,
                vector.y * sine + vector.z * cosine);
    }
}
