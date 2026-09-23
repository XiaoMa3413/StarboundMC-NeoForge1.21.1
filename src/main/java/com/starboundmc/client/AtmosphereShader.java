package com.starboundmc.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.slf4j.Logger;

/**
 * Holds the atmosphere shader registered through {@link RegisterShadersEvent}.
 *
 * <p>Same lifecycle contract as {@link PlanetSurfaceShader}: the instance is
 * reassigned on every resource reload so a closed program is never drawn with,
 * and a failed load leaves the field null so the renderer keeps its CPU glow
 * shell instead of failing the frame.</p>
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AtmosphereShader {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Shader id: resolves to {@code assets/starboundmc/shaders/core/atmosphere.json}. */
    public static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "atmosphere");

    private static ShaderInstance instance;

    private AtmosphereShader() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(), SHADER_ID,
                            DefaultVertexFormat.POSITION),
                    loaded -> instance = loaded);
        } catch (Exception exception) {
            instance = null;
            LOGGER.error("Failed to load the atmosphere shader {}; the atmosphere "
                    + "falls back to the CPU glow shell until the next reload.",
                    SHADER_ID, exception);
        }
    }

    /** The live shader, or null when it failed to load and the fallback should run. */
    public static ShaderInstance instance() {
        return instance;
    }
}
