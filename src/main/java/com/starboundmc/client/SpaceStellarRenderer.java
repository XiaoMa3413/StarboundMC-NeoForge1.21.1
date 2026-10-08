package com.starboundmc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.SpaceCoordinateFrame;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.space.StarSystemResolver;
import com.starboundmc.client.space.StellarProjection;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Analytic photosphere intersection and a separate, depth-tested additive corona. */
final class SpaceStellarRenderer {
    private static ShaderInstance shader;
    private static VertexBuffer quad;
    private SpaceStellarRenderer() {}

    static void register(RegisterShadersEvent event) {
        release();
        shader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "space_stellar"),
                    DefaultVertexFormat.POSITION), loaded -> shader = loaded);
        } catch (java.io.IOException | RuntimeException failure) {
            LogUtils.getLogger().warn("Space photosphere shader unavailable; using direct pipeline", failure);
        }
    }

    static boolean ready() { return shader != null; }
    static ShaderInstance current() { return shader; }

    static void render(Matrix4f skyView, SpaceRenderContext space, SpaceCoordinateFrame frame,
                       StarSystemResolver.ResolvedStarField stars, boolean corona) {
        if (quad == null || quad.isInvalid()) {
            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.addVertex(-1, -1, 0); builder.addVertex(1, -1, 0);
            builder.addVertex(1, 1, 0); builder.addVertex(-1, 1, 0);
            quad = new VertexBuffer(VertexBuffer.Usage.STATIC);
            quad.bind(); quad.upload(builder.buildOrThrow());
        }
        ShaderInstance previous = RenderSystem.getShader();
        try {
            RenderSystem.enableDepthTest(); RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.depthMask(!corona); RenderSystem.disableCull();
            if (corona) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            } else RenderSystem.disableBlend();
            RenderSystem.setShader(() -> shader);
            shader.safeGetUniform("CoronaPass").set(corona ? 1F : 0F);
            shader.safeGetUniform("LinearColor").set(SpaceSceneTarget.linear() ? 1F : 0F);
            shader.safeGetUniform("ViewToWorld").set(new Matrix4f(skyView)
                    .rotateX((float) Math.toRadians(-space.pitch()))
                    .rotateY((float) Math.toRadians(-space.yaw())).invert());
            var level = net.minecraft.client.Minecraft.getInstance().level;
            shader.safeGetUniform("TimePhase").set(level == null ? 0F
                    : com.starboundmc.client.space.SpaceRenderClock.twinklePhase(level.getGameTime(),
                    net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
            Vector3d view = new Vector3d();
            for (int i = 0; i < stars.count(); i++) {
                var star = stars.star(i);
                var profile = star.system().stellarVisual();
                if (star.stellarBrightness() <= .002F || star.distance() <= .001) continue;
                frame.toViewRelative(star.relativeX(), star.relativeY(), star.relativeZ(), view);
                Vector3f center = new Vector3f((float) (view.x / star.distance() * 320),
                        (float) (view.y / star.distance() * 320), (float) (view.z / star.distance() * 320));
                skyView.transformPosition(center);
                var response = profile.getDistanceResponse();
                float radius = Math.max(.35F, StellarProjection.skyRadius(response.baseSkyRadius(),
                        response.referenceDistance(), star.distance()));
                float detail = Math.max(0, Math.min(1, (radius - 2) / 10));
                shader.safeGetUniform("CenterView").set(center.x, center.y, center.z);
                shader.safeGetUniform("SphereRadius").set(320F * radius / (float) Math.hypot(320, radius));
                shader.safeGetUniform("QuadRadius").set(radius * (corona ? 3.6F : 1.02F));
                shader.safeGetUniform("DistanceScale").set((float) (star.distance() / 320));
                shader.safeGetUniform("Brightness").set(star.stellarBrightness());
                shader.safeGetUniform("Detail").set(detail);
                shader.safeGetUniform("CoronaDetail").set(detail);
                shader.safeGetUniform("FlareStrength").set(profile.getFlareStrength());
                setColor("SurfaceColor", profile.getSurfaceColor());
                setColor("CoronaColor", profile.getCoronaColor());
                quad.bind();
                quad.drawWithShader(new Matrix4f(), RenderSystem.getProjectionMatrix(), shader);
            }
        } finally {
            VertexBuffer.unbind(); RenderSystem.setShader(() -> previous);
            SpaceRenderPassState.restoreDefaults();
        }
    }

    private static void setColor(String uniform, int color) {
        shader.safeGetUniform(uniform).set(((color >> 16) & 255) / 255F,
                ((color >> 8) & 255) / 255F, (color & 255) / 255F);
    }

    static void release() {
        if (quad != null) quad.close();
        quad = null;
    }
}
