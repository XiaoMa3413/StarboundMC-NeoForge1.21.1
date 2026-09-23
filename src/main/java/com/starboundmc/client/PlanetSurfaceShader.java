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
 * Holds the planet surface shader registered through {@link RegisterShadersEvent}.
 *
 * <p>The event fires on the mod bus after the vanilla shaders load, and again on
 * every resource reload. GameRenderer closes the previous instances before
 * reposting it, so the load callback is the only safe place to keep the live
 * program: reassigning there means a stale reference is never drawn with. A
 * failed load leaves the field null, and the planet renderer falls back to its
 * CPU-baked sphere instead of failing the frame.</p>
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PlanetSurfaceShader {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Shader id: resolves to {@code assets/starboundmc/shaders/core/planet_surface.json}. */
    public static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "planet_surface");

    private static ShaderInstance instance;

    private PlanetSurfaceShader() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(), SHADER_ID,
                            DefaultVertexFormat.POSITION_TEX),
                    loaded -> instance = loaded);
        } catch (Exception exception) {
            instance = null;
            LOGGER.error("Failed to load the planet surface shader {}; planet lighting "
                    + "falls back to the CPU-baked sphere until the next reload.",
                    SHADER_ID, exception);
        }
    }

    /** The live shader, or null when it failed to load and the fallback should run. */
    public static ShaderInstance instance() {
        return instance;
    }
}
