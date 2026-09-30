package com.starboundmc.client.compat.stellarview;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.StarfieldClientConfig;
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
    private static final int FIELD_DIAMETER_LY = 1_000_000;
    private static final long FIELD_SEED = 0x5B4D434CL;

    private static StellarViewPositionAdapter position;
    private static ExternalStarField stars;
    private static int starBudget;
    private static boolean checkedLinear;
    private static java.lang.reflect.Method linearMethod;

    private StellarViewBackend() {}

    static boolean render(ClientLevel level, Camera camera, float partialTick,
                          Matrix4f modelView, Matrix4f projection,
                          SpaceRenderContext space, float brightness,
                          float convergence, Vector3f convergenceForward, boolean linear)
    {
        int budget = StarfieldClientConfig.SPACE_VISUAL_QUALITY.get().backgroundStarBudget();
        if (stars != null && budget != starBudget) reset();
        if (stars == null)
        {
            starBudget = budget;
            position = new StellarViewPositionAdapter();
            stars = new ExternalStarField(new StarField(
                    Optional.empty(), Either.left(new SpaceCoords()), AxisRotation.NONE,
                    0, Optional.empty(), StarField.DEFAULT_DUST_CLOUD_TEXTURE,
                    false, StarField.Stretch.DEFAULT_STRETCH,
                    budget, Optional.of(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            "starboundmc", "cinematic_stars")), StarField.DEFAULT_STAR_TEXTURE,
                    false, StarField.Stretch.DEFAULT_STRETCH,
                    FIELD_SEED, FIELD_DIAMETER_LY, List.of()), position);
        }
        position.setPosition(space.universePosition());
        if (linear) {
            if (!supportsLinear()) return false;
            try {
                return (boolean) linearMethod.invoke(stars, level, camera, partialTick, modelView, projection,
                        brightness, convergence, convergenceForward, .7F);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Stellar View linear radiance failed", failure);
            }
        }
        return stars.render(level, camera, partialTick, modelView, projection,
                brightness, convergence, convergenceForward);
    }

    static boolean supportsLinear() {
        if (!checkedLinear) {
            checkedLinear = true;
            try {
                linearMethod = ExternalStarField.class.getMethod("renderLinear", ClientLevel.class, Camera.class, float.class,
                        Matrix4f.class, Matrix4f.class, float.class, float.class, Vector3f.class, float.class);
            } catch (NoSuchMethodException missing) { linearMethod = null; }
        }
        return linearMethod != null;
    }

    static void reset()
    {
        if (stars != null)
            stars.reset();
        stars = null;
        position = null;
    }
}
