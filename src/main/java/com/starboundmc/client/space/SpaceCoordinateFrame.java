package com.starboundmc.client.space;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.starboundmc.space.UniversePosition;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Ship-relative coordinate transforms used by the space renderers.
 *
 * <p>Universe deltas are evaluated before rotation, so large sector coordinates
 * never pass through a float. The virtual scene then uses the existing ship
 * heading convention: negative ship yaw about Y, followed by negative ship
 * pitch about X. Ship roll remains a camera-frame bank and is deliberately not
 * part of point-position transforms.</p>
 */
public final class SpaceCoordinateFrame
{
    private static final SpaceCoordinateFrame IDENTITY =
            new SpaceCoordinateFrame(UniversePosition.of(0.0, 0.0, 0.0), 0.0, 0.0);

    private final UniversePosition origin;
    private final double yawCos;
    private final double yawSin;
    private final double pitchCos;
    private final double pitchSin;
    private final float bankDegrees;
    private final float backgroundYawCos;
    private final float backgroundYawSin;
    private final float backgroundPitchCos;
    private final float backgroundPitchSin;

    public SpaceCoordinateFrame(SpaceRenderContext context)
    {
        Objects.requireNonNull(context, "context");
        origin = Objects.requireNonNull(context.universePosition(), "context.universePosition");
        bankDegrees = (float) -context.roll();
        Rotation rotation = new Rotation(context.yaw(), context.pitch());
        yawCos = rotation.yawCos;
        yawSin = rotation.yawSin;
        pitchCos = rotation.pitchCos;
        pitchSin = rotation.pitchSin;
        backgroundYawCos = rotation.backgroundYawCos;
        backgroundYawSin = rotation.backgroundYawSin;
        backgroundPitchCos = rotation.backgroundPitchCos;
        backgroundPitchSin = rotation.backgroundPitchSin;
    }

    /** A rotation-only frame for skies that are not anchored to a ship. */
    public SpaceCoordinateFrame(UniversePosition origin, double yaw, double pitch)
    {
        this.origin = Objects.requireNonNull(origin, "origin");
        bankDegrees = 0.0F;
        Rotation rotation = new Rotation(yaw, pitch);
        yawCos = rotation.yawCos;
        yawSin = rotation.yawSin;
        pitchCos = rotation.pitchCos;
        pitchSin = rotation.pitchSin;
        backgroundYawCos = rotation.backgroundYawCos;
        backgroundYawSin = rotation.backgroundYawSin;
        backgroundPitchCos = rotation.backgroundPitchCos;
        backgroundPitchSin = rotation.backgroundPitchSin;
    }

    public static SpaceCoordinateFrame identity()
    {
        return IDENTITY;
    }

    private record Rotation(double yawCos, double yawSin, double pitchCos, double pitchSin,
                            float backgroundYawCos, float backgroundYawSin,
                            float backgroundPitchCos, float backgroundPitchSin)
    {
        private Rotation(double yaw, double pitch)
        {
            this(Math.cos(Math.toRadians(-yaw)), Math.sin(Math.toRadians(-yaw)),
                    Math.cos(Math.toRadians(-pitch)), Math.sin(Math.toRadians(-pitch)),
                    (float) Math.cos(Math.toRadians((float) -yaw)),
                    (float) Math.sin(Math.toRadians((float) -yaw)),
                    (float) Math.cos(Math.toRadians((float) -pitch)),
                    (float) Math.sin(Math.toRadians((float) -pitch)));
        }
    }

    /** Converts an absolute universe point into ship-oriented view coordinates. */
    public Vec3 toView(UniversePosition point)
    {
        Objects.requireNonNull(point, "point");
        Vec3 relative = origin.deltaTo(point).toVec3();
        return toViewRelative(relative.x, relative.y, relative.z);
    }

    /**
     * Builds the bobbing-free AFTER_SKY camera frame and applies ship roll as
     * an outside-scene bank. The bank never changes universe point transforms.
     */
    public PoseStack stableCameraPose(Camera camera)
    {
        PoseStack pose = new PoseStack();
        pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        pose.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(bankDegrees));
        return pose;
    }

    /** Rotates an already ship-relative universe-space vector into view space. */
    public Vec3 toViewRelative(double x, double y, double z)
    {
        Vector3d result = toViewRelative(x, y, z, new Vector3d());
        return new Vec3(result.x, result.y, result.z);
    }

    /** Allocation-free form for renderers that transform several points. */
    public Vector3d toViewRelative(double x, double y, double z, Vector3d result)
    {
        double yawX = x * yawCos + z * yawSin;
        double yawZ = -x * yawSin + z * yawCos;
        double viewY = y * pitchCos - yawZ * pitchSin;
        double viewZ = y * pitchSin + yawZ * pitchCos;
        return result.set(yawX, viewY, viewZ);
    }

    /**
     * Float-preserving transform for the precomputed background star directions.
     * The caller supplies one reusable result to avoid per-star allocations.
     */
    public Vector3f toBackgroundDirection(float x, float y, float z, Vector3f result)
    {
        float yawX = x * backgroundYawCos + z * backgroundYawSin;
        float yawZ = -x * backgroundYawSin + z * backgroundYawCos;
        float viewY = y * backgroundPitchCos - yawZ * backgroundPitchSin;
        float viewZ = y * backgroundPitchSin + yawZ * backgroundPitchCos;
        return result.set(yawX, viewY, viewZ);
    }
}
