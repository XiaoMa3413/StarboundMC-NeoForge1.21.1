package com.starboundmc.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/** Unlit, alpha-weighted additive emission. No lightmap, normals or world-light changes. */
public final class ThrusterRenderTypes {
    private static ShaderInstance flameShader;
    private static ShaderInstance haloShader;
    private static final ResourceLocation EXHAUST = id("textures/effect/shuttle_exhaust.png");
    public static final RenderType FLAME = create("thruster_flame", false);
    public static final RenderType HALO = create("thruster_halo", true);

    private ThrusterRenderTypes() {}

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("thruster_flame"),
                DefaultVertexFormat.POSITION_TEX_COLOR), shader -> flameShader = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("thruster_halo"),
                DefaultVertexFormat.POSITION_TEX_COLOR), shader -> haloShader = shader);
    }

    private static RenderType create(String name, boolean halo) {
        return RenderType.create(name, DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 8192, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> halo ? haloShader : flameShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(EXHAUST, true, false))
                        // SRC_ALPHA, ONE: transparent texels contribute no light and never darken the background.
                        .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("starboundmc", path);
    }
}
