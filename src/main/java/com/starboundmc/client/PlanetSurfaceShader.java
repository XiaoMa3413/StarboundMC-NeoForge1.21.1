package com.starboundmc.client;

import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.IOException;

/** Minimal day/night lighting for the shared ship-space planet mesh. */
final class PlanetSurfaceShader
{
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

    static void setLighting(ShaderInstance target, Vector3f sunDirection, float terminatorWidth,
                            float nightFloor, float alpha, float brightness)
    {
        target.safeGetUniform("SunDirection").set(sunDirection.x, sunDirection.y, sunDirection.z);
        target.safeGetUniform("TerminatorWidth").set(terminatorWidth);
        target.safeGetUniform("NightFloor").set(nightFloor);
        target.safeGetUniform("SurfaceAlpha").set(alpha);
        target.safeGetUniform("Brightness").set(brightness);
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
