package com.starboundmc.client;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanetSurfaceLightingTest
{
    @Test
    void cloudRelativeRotationMapsSurfacePointsIntoTheInverseCloudFrame()
    {
        Random random = new Random(20260928L);
        for (int sample = 0; sample < 320; sample++)
        {
            float bodySpin = random.nextFloat() * 1440.0F - 720.0F;
            float cloudDelta = random.nextFloat() * 720.0F - 360.0F;
            float tilt = random.nextFloat() * 160.0F - 80.0F;
            float yaw = random.nextFloat() * 720.0F - 360.0F;
            Vector3f surfacePoint = randomUnitVector(random);

            Matrix4f surfaceFrame = PlanetSurfaceLighting.appendBodyOrientation(
                    new Matrix4f(), bodySpin, tilt, yaw);
            Matrix4f cloudFrame = PlanetSurfaceLighting.appendBodyOrientation(
                    new Matrix4f(), bodySpin + cloudDelta, tilt, yaw);
            Vector3f expected = cloudFrame.invert().transformDirection(
                    surfaceFrame.transformDirection(new Vector3f(surfacePoint)));
            Vector3f actual = PlanetSurfaceLighting.surfaceToCloudRelativeRotation(
                    cloudDelta, tilt, yaw).transform(new Vector3f(surfacePoint));

            assertEquals(expected.x, actual.x, 2.0E-4F, "cloud-local x, sample " + sample);
            assertEquals(expected.y, actual.y, 2.0E-4F, "cloud-local y, sample " + sample);
            assertEquals(expected.z, actual.z, 2.0E-4F, "cloud-local z, sample " + sample);
        }
    }

    @Test
    void positiveCloudSpinUsesTheInverseUvLongitudeDirection()
    {
        Vector3f surfaceLongitudeZero = new Vector3f(1.0F, 0.0F, 0.0F);
        Vector3f cloudLocal = PlanetSurfaceLighting.surfaceToCloudRelativeRotation(
                90.0F, 0.0F, 0.0F).transform(surfaceLongitudeZero);

        assertEquals(0.0F, cloudLocal.x, 1.0E-5F);
        assertEquals(0.0F, cloudLocal.y, 1.0E-5F);
        assertEquals(1.0F, cloudLocal.z, 1.0E-5F);
    }

    @Test
    void meshCameraAndSurfaceViewDirectionMatchTheFullModelViewFrame()
    {
        Matrix4f model = new Matrix4f()
                .rotateX((float) Math.toRadians(-16.0F))
                .rotateY((float) Math.toRadians(73.0F))
                .rotateZ((float) Math.toRadians(-28.0F))
                .translate(23.0F, -11.0F, 260.0F)
                .rotateX((float) Math.toRadians(-19.0F))
                .rotateY((float) Math.toRadians(-42.0F));
        PlanetSurfaceLighting.appendBodyOrientation(model, 117.0F, 31.0F, -86.0F).scale(0.72F);

        Vector3f cameraMesh = PlanetSurfaceLighting.cameraPositionMesh(model);
        Vector3f cameraInView = model.transformPosition(new Vector3f(cameraMesh));
        assertEquals(0.0F, cameraInView.x, 2.0E-4F);
        assertEquals(0.0F, cameraInView.y, 2.0E-4F);
        assertEquals(0.0F, cameraInView.z, 2.0E-4F);

        Vector3f meshPosition = new Vector3f(31.0F, -13.0F, 22.0F);
        Vector3f viewMesh = new Vector3f(cameraMesh).sub(meshPosition).normalize();
        Vector3f viewFromModel = model.transformDirection(new Vector3f(viewMesh)).normalize();
        Vector3f pointInView = model.transformPosition(new Vector3f(meshPosition));
        Vector3f expectedView = pointInView.negate().normalize();
        assertEquals(expectedView.x, viewFromModel.x, 2.0E-4F);
        assertEquals(expectedView.y, viewFromModel.y, 2.0E-4F);
        assertEquals(expectedView.z, viewFromModel.z, 2.0E-4F);
    }

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
