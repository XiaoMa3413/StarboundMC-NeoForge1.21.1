package com.starboundmc.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Coordinate conversions for the shared ship-space planet surface mesh. */
final class PlanetSurfaceLighting
{
    private PlanetSurfaceLighting()
    {
    }

    /**
     * Appends the existing body orientation and animation spin to the planet model matrix.
     * The order matches the old CPU-oriented sphere vertices: body yaw, body tilt, then spin.
     */
    static Matrix4f appendBodyOrientation(Matrix4f model, float spinDegrees,
                                          float orientationTiltDegrees, float orientationYawDegrees)
    {
        return model.rotateY((float) Math.toRadians(spinDegrees))
                .rotateX((float) Math.toRadians(orientationTiltDegrees))
                .rotateY((float) Math.toRadians(orientationYawDegrees));
    }

    /**
     * Converts the fixed virtual-space sun direction into the unrotated sphere mesh frame.
     * The inverse body orientation and spin keep normals and sunlight in the same space.
     */
    static Vector3f toMeshSpaceSun(Vector3f worldSunDirection, float spinDegrees,
                                   float orientationTiltDegrees, float orientationYawDegrees)
    {
        return new Vector3f(worldSunDirection)
                .rotateY((float) Math.toRadians(-spinDegrees))
                .rotateX((float) Math.toRadians(-orientationTiltDegrees))
                .rotateY((float) Math.toRadians(-orientationYawDegrees));
    }
}
