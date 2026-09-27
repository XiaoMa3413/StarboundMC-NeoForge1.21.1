package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Shared static GPU shell and independent shader pass for ship-space atmospheres. */
final class AtmosphereShellRenderer
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int STACKS = 16;
    private static final int SLICES = 32;
    private static final float OUTER_RADIUS = PlanetRenderer.PLANET_RADIUS;
    private static VertexBuffer shellBuffer;
    private static boolean bufferUnavailable;

    private AtmosphereShellRenderer()
    {
    }

    static void render(Matrix4f model, Vector3f sunDirectionMesh, Vector3f cameraPositionMesh,
                       Vector3f color, float strength, float alpha)
    {
        ShaderInstance activeShader = AtmosphereShader.current();
        if (activeShader == null || strength <= 0.0F || alpha <= 0.0F)
            return;

        VertexBuffer shell = getShellBuffer();
        if (shell == null)
            return;

        ShaderInstance previousShader = RenderSystem.getShader();
        try
        {
            FogRenderer.setupNoFog();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(() -> activeShader);

            try
            {
                AtmosphereShader.setLighting(activeShader, sunDirectionMesh, cameraPositionMesh,
                        color, strength, alpha);
                shell.bind();
                shell.drawWithShader(model, RenderSystem.getProjectionMatrix(), activeShader);
            }
            catch (RuntimeException shaderFailure)
            {
                AtmosphereShader.disableAfterFailure(activeShader, shaderFailure);
            }
            finally
            {
                VertexBuffer.unbind();
            }
        }
        finally
        {
            RenderSystem.setShader(() -> previousShader);
            SpaceRenderPassState.restoreDefaults();
        }
    }

    private static VertexBuffer getShellBuffer()
    {
        if (bufferUnavailable)
            return null;
        if (shellBuffer != null && !shellBuffer.isInvalid())
            return shellBuffer;
        if (shellBuffer != null)
            shellBuffer.close();

        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, AtmosphereShader.VERTEX_FORMAT);
        for (int stack = 0; stack < STACKS; stack++)
        {
            float phi0 = (float) (Math.PI * stack / STACKS);
            float phi1 = (float) (Math.PI * (stack + 1) / STACKS);
            for (int slice = 0; slice < SLICES; slice++)
            {
                float theta0 = (float) (2.0 * Math.PI * slice / SLICES);
                float theta1 = (float) (2.0 * Math.PI * (slice + 1) / SLICES);
                addVertex(builder, phi0, theta0);
                addVertex(builder, phi0, theta1);
                addVertex(builder, phi1, theta1);
                addVertex(builder, phi1, theta0);
            }
        }

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try
        {
            buffer.bind();
            buffer.upload(builder.buildOrThrow());
        }
        catch (RuntimeException failure)
        {
            buffer.close();
            bufferUnavailable = true;
            LOGGER.warn("Could not create the ship-space atmosphere shell; atmosphere rendering is disabled",
                    failure);
            return null;
        }
        finally
        {
            VertexBuffer.unbind();
        }

        shellBuffer = buffer;
        return shellBuffer;
    }

    private static void addVertex(BufferBuilder builder, float phi, float theta)
    {
        float sinPhi = (float) Math.sin(phi);
        float normalX = sinPhi * (float) Math.cos(theta);
        float normalY = (float) Math.cos(phi);
        float normalZ = sinPhi * (float) Math.sin(theta);
        builder.addVertex(normalX * OUTER_RADIUS, normalY * OUTER_RADIUS, normalZ * OUTER_RADIUS)
                .setNormal(normalX, normalY, normalZ);
    }
}
