package com.starboundmc.client.compat.stellarview;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.starboundmc.client.space.SpaceRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.povstalec.stellarview.api.client.ExternalStarField;
import net.povstalec.stellarview.api.common.space_objects.resourcepack.StarField;
import net.povstalec.stellarview.common.util.AxisRotation;
import net.povstalec.stellarview.common.util.SpaceCoords;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Loaded only after the compatibility boundary confirms Stellar View is present. */
final class StellarViewBackend
{
    private static final int BACKGROUND_STAR_COUNT = 12_000;
    private static final int FIELD_DIAMETER_LY = 1_000_000;
    private static final long FIELD_SEED = 0x5B4D434CL;

    private static StellarViewPositionAdapter position;
    private static ExternalStarField stars;

    private StellarViewBackend() {}

    static boolean render(ClientLevel level, Camera camera, float partialTick,
                          Matrix4f modelView, Matrix4f projection,
                          SpaceRenderContext space, float brightness,
                          float convergence, Vector3f convergenceForward)
    {
        if (stars == null)
        {
            position = new StellarViewPositionAdapter();
            stars = new ExternalStarField(new StarField(
                    Optional.empty(), Either.left(new SpaceCoords()), AxisRotation.NONE,
                    0, Optional.empty(), StarField.DEFAULT_DUST_CLOUD_TEXTURE,
                    false, StarField.Stretch.DEFAULT_STRETCH,
                    BACKGROUND_STAR_COUNT, Optional.empty(), StarField.DEFAULT_STAR_TEXTURE,
                    false, StarField.Stretch.DEFAULT_STRETCH,
                    FIELD_SEED, FIELD_DIAMETER_LY, List.of()), position);
        }
        position.setPosition(space.universePosition());
        return stars.render(level, camera, partialTick, modelView, projection,
                brightness, convergence, convergenceForward);
    }

    static void reset()
    {
        if (stars != null)
            stars.reset();
        stars = null;
        position = null;
    }
}
