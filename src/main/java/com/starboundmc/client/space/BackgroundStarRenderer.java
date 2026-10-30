/* StellarView-derived star projection and radiance, Copyright (c) 2024 Povstalec, MIT.
 * Resource ownership, coordinate rebasing and vertex submission by StarboundMC.
 * See third_party/stellarview/LICENSE.txt and THIRD_PARTY_NOTICES.md. */
package com.starboundmc.client.space;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.starboundmc.space.UniversePosition;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Standalone cinematic stars; no StellarView classes, config, registries or lifecycle. */
public final class BackgroundStarRenderer {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final UniversePosition ORIGIN = UniversePosition.of(0, 0, 0);
    private record Field(VertexBuffer buffer, UniversePosition anchor) {}
    private static final Map<Integer, Field> FIELDS = new HashMap<>();
    private static List<BackgroundStarCatalog.StarType> types = List.of();
    private static ShaderInstance shader;
    private static boolean failed;
    private static long uploads, draws;
    private static float maxDistance;
    private BackgroundStarRenderer() {}

    public static void register(RegisterShadersEvent event) {
        release();
        shader = null;
        failed = false;
        try {
            var profile = ResourceLocation.fromNamespaceAndPath("starboundmc", BackgroundStarCatalog.PROFILE_RESOURCE_PATH);
            try (var reader = event.getResourceProvider().getResourceOrThrow(profile).openAsReader()) {
                types = BackgroundStarCatalog.readProfile(reader);
            }
            maxDistance = (float) types.stream().mapToDouble(BackgroundStarCatalog.StarType::maxDistance).max().orElseThrow();
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("starboundmc", "background_stars"),
                    DefaultVertexFormat.POSITION_TEX_COLOR), registered -> shader = registered);
        } catch (java.io.IOException | RuntimeException failure) {
            failed = true;
            LOGGER.warn("Cinematic star resources unavailable; using minimal stars until resource reload", failure);
        }
    }

    public static boolean ready() { return shader != null && !failed; }
    public static ShaderInstance shader() { return shader; }

    public static boolean render(Matrix4f modelView, Matrix4f projection, UniversePosition observer,
                                 Vector3f axis, float alpha, float convergence,
                                 int count, int width, int height, boolean linear) {
        if (!ready()) return false;
        if (ORIGIN.deltaTo(observer).length() > BackgroundStarCatalog.RADIUS + maxDistance) return true;
        var previous = RenderSystem.getShader();
        try {
            var field = FIELDS.get(count);
            if (field == null || field.buffer().isInvalid() || field.anchor().deltaTo(observer).lengthSqr() > 32768D*32768D) {
                if (field != null) field.buffer().close();
                var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                for (var star : BackgroundStarCatalog.generate(count, types)) {
                    var relative = observer.deltaTo(UniversePosition.of(star.x(), star.y(), star.z()));
                    for (int corner = 0; corner < 4; corner++)
                        builder.addVertex((float) relative.x(), (float) relative.y(), (float) relative.z())
                                .setUv(star.size()*4F, star.maxDistance())
                                .setColor(star.red(), star.green(), star.blue(), star.brightness());
                }
                var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                try { buffer.bind(); buffer.upload(builder.buildOrThrow()); }
                catch (RuntimeException failure) { buffer.close(); throw failure; }
                field = new Field(buffer, observer);
                FIELDS.put(count, field);
                uploads++;
            }
            var offset = field.anchor().deltaTo(observer);
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
            RenderSystem.disableCull(); RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(() -> shader);
            shader.safeGetUniform("ObserverOffset").set((float)offset.x(), (float)offset.y(), (float)offset.z());
            shader.safeGetUniform("ConvergenceAxis").set(axis.x, axis.y, axis.z);
            shader.safeGetUniform("Convergence").set(convergence);
            shader.safeGetUniform("StarAlpha").set(alpha);
            shader.safeGetUniform("ViewportSize").set((float)width, (float)height);
            shader.safeGetUniform("LinearColor").set(linear ? 1F : 0F);
            field.buffer().bind();
            field.buffer().drawWithShader(modelView, projection, shader);
            draws++;
            return true;
        } catch (RuntimeException failure) {
            failed = true;
            LOGGER.warn("Cinematic stars failed; using minimal stars until resource reload", failure);
            return false;
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShader(() -> previous);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); RenderSystem.enableCull();
            RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc();
        }
    }

    public static void release() {
        FIELDS.values().forEach(field -> field.buffer().close());
        FIELDS.clear();
    }

    public static String diagnostics() {
        return "starShader=" + (shader != null) + " starsFailed=" + failed
                + " starCore=stellarview-hdr2 starDraws=" + draws + " starUploads=" + uploads + " cachedStarBudgets=" + FIELDS.keySet();
    }
}
