package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Shared static textured sphere and independent alpha-blended ship-space cloud pass. */
final class CloudShellRenderer
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int STACKS = 64;
    private static final int SLICES = 128;
    private static final float SPHERE_RADIUS = PlanetRenderer.PLANET_RADIUS;
    static final VertexFormat VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Normal", VertexFormatElement.NORMAL)
            .build();

    private static VertexBuffer shellBuffer;
    private static boolean bufferUnavailable;
    private static boolean invalidTextureLogged;

    private CloudShellRenderer()
    {
    }

    static void release() {
        if (shellBuffer != null) shellBuffer.close();
        shellBuffer = null;
        bufferUnavailable = invalidTextureLogged = false;
    }

    static void render(Matrix4f model, String textureId, Vector3f sunDirectionMesh,
                       float cloudOpacity, float globalAlpha)
    {
        if (textureId == null || textureId.isBlank() || cloudOpacity <= 0.0F || globalAlpha <= 0.0F)
            return;

        ShaderInstance activeShader = CloudShader.current();
        if (activeShader == null)
            return;

        ResourceLocation texture;
        try
        {
            texture = ResourceLocation.parse(textureId);
        }
        catch (RuntimeException invalidTexture)
        {
            if (!invalidTextureLogged)
            {
                invalidTextureLogged = true;
                LOGGER.warn("Invalid cloud texture id; cloud rendering is skipped", invalidTexture);
            }
            return;
        }

        VertexBuffer shell = getShellBuffer();
        if (shell == null)
            return;

        ShaderInstance previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        try
        {
            FogRenderer.setupNoFog();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(() -> activeShader);
            CloudShader.setLighting(activeShader, sunDirectionMesh, cloudOpacity, globalAlpha);
            SpaceSceneTarget.configure(activeShader);

            shell.bind();
            shell.drawWithShader(model, RenderSystem.getProjectionMatrix(), activeShader);
        }
        catch (RuntimeException shaderFailure)
        {
            CloudShader.disableAfterFailure(activeShader, shaderFailure);
        }
        finally
        {
            VertexBuffer.unbind();
            RenderSystem.setShaderTexture(0, previousTexture);
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

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, VERTEX_FORMAT);
        for (int stack = 0; stack < STACKS; stack++)
        {
            float phi0 = (float) (Math.PI * stack / STACKS);
            float phi1 = (float) (Math.PI * (stack + 1) / STACKS);
            float v0 = (float) stack / STACKS;
            float v1 = (float) (stack + 1) / STACKS;
            for (int slice = 0; slice < SLICES; slice++)
            {
                float theta0 = (float) (2.0 * Math.PI * slice / SLICES);
                float theta1 = (float) (2.0 * Math.PI * (slice + 1) / SLICES);
                float u0 = (float) slice / SLICES;
                float u1 = (float) (slice + 1) / SLICES;
                addVertex(builder, phi0, theta0, u0, v0);
                addVertex(builder, phi0, theta1, u1, v0);
                addVertex(builder, phi1, theta1, u1, v1);
                addVertex(builder, phi1, theta0, u0, v1);
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
            LOGGER.warn("Could not create the ship-space cloud shell; cloud rendering is disabled", failure);
            return null;
        }
        finally
        {
            VertexBuffer.unbind();
        }

        shellBuffer = buffer;
        return shellBuffer;
    }

    private static void addVertex(BufferBuilder builder, float phi, float theta, float u, float v)
    {
        float sinPhi = (float) Math.sin(phi);
        float normalX = sinPhi * (float) Math.cos(theta);
        float normalY = (float) Math.cos(phi);
        float normalZ = sinPhi * (float) Math.sin(theta);
        builder.addVertex(normalX * SPHERE_RADIUS, normalY * SPHERE_RADIUS, normalZ * SPHERE_RADIUS)
                .setUv(u, v)
                .setNormal(normalX, normalY, normalZ);
    }
}
