package com.starboundmc.client;

import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.IOException;

/** Minimal day/night lighting for the shared ship-space planet mesh. */
final class PlanetSurfaceShader
{
    static final float CLOUD_SHADOW_STRENGTH = 0.24F;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "planet_surface");
    private static ShaderInstance shader;

    private PlanetSurfaceShader()
    {
    }

    static void register(RegisterShadersEvent event)
    {
        shader = null;
        try
        {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), SHADER_ID,
                    PlanetRenderer.PLANET_SURFACE_FORMAT), registered -> shader = registered);
        }
        catch (IOException | RuntimeException exception)
        {
            LOGGER.warn("Could not load the ship-space planet surface shader; using the unlit static mesh",
                    exception);
        }
    }

    static ShaderInstance current()
    {
        return shader;
    }

    static void setLighting(ShaderInstance target, Vector3f sunDirection, Vector3f cameraPositionMesh,
                            float terminatorWidth, float nightFloor,
                            float specularStrength, float roughness, float fresnelStrength,
                            boolean hasMaterialMask, float emissiveStrength,
                            float alpha, float brightness)
    {
        target.safeGetUniform("SunDirection").set(sunDirection.x, sunDirection.y, sunDirection.z);
        target.safeGetUniform("CameraPositionMesh").set(
                cameraPositionMesh.x, cameraPositionMesh.y, cameraPositionMesh.z);
        target.safeGetUniform("TerminatorWidth").set(terminatorWidth);
        target.safeGetUniform("NightFloor").set(nightFloor);
        target.safeGetUniform("SpecularStrength").set(specularStrength);
        target.safeGetUniform("Roughness").set(roughness);
        target.safeGetUniform("FresnelStrength").set(fresnelStrength);
        target.safeGetUniform("MaterialMaskEnabled").set(hasMaterialMask ? 1.0F : 0.0F);
        target.safeGetUniform("EmissiveStrength").set(emissiveStrength);
        target.safeGetUniform("SurfaceAlpha").set(alpha);
        target.safeGetUniform("Brightness").set(brightness);
    }

    static void setCloudShadow(ShaderInstance target, boolean enabled, float cloudShellScale,
                               float cloudOpacity, float cloudFade, Matrix3f surfaceToCloudRotation)
    {
        target.safeGetUniform("CloudShadowEnabled").set(enabled ? 1.0F : 0.0F);
        target.safeGetUniform("CloudShellScale").set(cloudShellScale);
        target.safeGetUniform("CloudOpacity").set(cloudOpacity * cloudFade);
        target.safeGetUniform("CloudShadowStrength").set(CLOUD_SHADOW_STRENGTH);
        if (enabled)
        {
            Vector3f xAxis = surfaceToCloudRotation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
            Vector3f yAxis = surfaceToCloudRotation.transform(new Vector3f(0.0F, 1.0F, 0.0F));
            Vector3f zAxis = surfaceToCloudRotation.transform(new Vector3f(0.0F, 0.0F, 1.0F));
            target.safeGetUniform("CloudRelativeRotationX").set(xAxis.x, xAxis.y, xAxis.z);
            target.safeGetUniform("CloudRelativeRotationY").set(yAxis.x, yAxis.y, yAxis.z);
            target.safeGetUniform("CloudRelativeRotationZ").set(zAxis.x, zAxis.y, zAxis.z);
        }
    }

    static void disableAfterFailure(ShaderInstance failedShader, RuntimeException exception)
    {
        if (shader == failedShader)
        {
            shader = null;
            LOGGER.warn("Ship-space planet surface shader failed while drawing; using the unlit static mesh",
                    exception);
        }
    }
}
