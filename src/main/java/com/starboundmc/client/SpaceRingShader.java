package com.starboundmc.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

final class SpaceRingShader {
    private static ShaderInstance shader;
    private static ShaderInstance pointShader;
    private SpaceRingShader() {}
    static void register(RegisterShadersEvent event) {
        RingRenderer.release();
        shader = pointShader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "space_ring"),
                    DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> shader = loaded);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "space_point"),
                    DefaultVertexFormat.POSITION_COLOR), loaded -> pointShader = loaded);
        } catch (java.io.IOException | RuntimeException failure) {
            LogUtils.getLogger().warn("Space ring shader unavailable; using direct pipeline", failure);
        }
    }
    static boolean ready() { return shader != null && pointShader != null; }
    static ShaderInstance current() { return shader; }
    static ShaderInstance point() { return pointShader; }
}
