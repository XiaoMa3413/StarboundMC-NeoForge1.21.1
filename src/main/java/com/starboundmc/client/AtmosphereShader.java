package com.starboundmc.client;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.IOException;

/** Uniform and lifetime management for the independent ship-space atmosphere shader. */
final class AtmosphereShader
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "atmosphere");
    static final VertexFormat VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Normal", VertexFormatElement.NORMAL)
            .build();
    private static ShaderInstance shader;

    private AtmosphereShader()
    {
    }

    static void register(RegisterShadersEvent event)
    {
        shader = null;
        try
        {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), SHADER_ID,
                    VERTEX_FORMAT), registered -> shader = registered);
        }
        catch (IOException | RuntimeException exception)
        {
            LOGGER.warn("Could not load the ship-space atmosphere shader; atmosphere rendering is disabled",
                    exception);
        }
    }

    static ShaderInstance current()
    {
        return shader;
    }

    static void setLighting(ShaderInstance target, Vector3f sunDirectionMesh, Vector3f cameraPositionMesh,
                            Vector3f color, float strength, float alpha,
                            float innerRadius, float outerRadius, float opticalDepthMax,
                            float nightFraction, float twilightStrength)
    {
        target.safeGetUniform("SunDirection").set(
                sunDirectionMesh.x, sunDirectionMesh.y, sunDirectionMesh.z);
        target.safeGetUniform("CameraPositionMesh").set(
                cameraPositionMesh.x, cameraPositionMesh.y, cameraPositionMesh.z);
        target.safeGetUniform("AtmosphereColor").set(color.x, color.y, color.z);
        target.safeGetUniform("AtmosphereStrength").set(strength);
        target.safeGetUniform("GlobalAlpha").set(alpha);
        target.safeGetUniform("NightFraction").set(nightFraction);
        target.safeGetUniform("TwilightStrength").set(twilightStrength);
        target.safeGetUniform("InnerRadius").set(innerRadius);
        target.safeGetUniform("OuterRadius").set(outerRadius);
        target.safeGetUniform("OpticalDepthMax").set(opticalDepthMax);
    }

    static void disableAfterFailure(ShaderInstance failedShader, RuntimeException exception)
    {
        if (shader == failedShader)
        {
            shader = null;
            LOGGER.warn("Ship-space atmosphere shader failed while drawing; atmosphere rendering is disabled",
                    exception);
        }
    }
}
