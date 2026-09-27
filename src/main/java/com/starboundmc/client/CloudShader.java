package com.starboundmc.client;

import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.IOException;

/** Uniform and lifetime management for the independent ship-space cloud shader. */
final class CloudShader
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "cloud");
    private static ShaderInstance shader;
    private static boolean loadFailureLogged;
    private static boolean drawFailureLogged;

    private CloudShader()
    {
    }

    static void register(RegisterShadersEvent event)
    {
        shader = null;
        try
        {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), SHADER_ID,
                    CloudShellRenderer.VERTEX_FORMAT), registered -> shader = registered);
        }
        catch (IOException | RuntimeException exception)
        {
            if (!loadFailureLogged)
            {
                loadFailureLogged = true;
                LOGGER.warn("Could not load the ship-space cloud shader; cloud rendering is disabled", exception);
            }
        }
    }

    static ShaderInstance current()
    {
        return shader;
    }

    static void setLighting(ShaderInstance target, Vector3f sunDirectionMesh,
                            float cloudOpacity, float globalAlpha)
    {
        target.safeGetUniform("SunDirection").set(
                sunDirectionMesh.x, sunDirectionMesh.y, sunDirectionMesh.z);
        target.safeGetUniform("CloudOpacity").set(cloudOpacity);
        target.safeGetUniform("GlobalAlpha").set(globalAlpha);
    }

    static void disableAfterFailure(ShaderInstance failedShader, RuntimeException exception)
    {
        if (shader == failedShader)
        {
            shader = null;
            if (!drawFailureLogged)
            {
                drawFailureLogged = true;
                LOGGER.warn("Ship-space cloud shader failed while drawing; cloud rendering is disabled", exception);
            }
        }
    }
}
