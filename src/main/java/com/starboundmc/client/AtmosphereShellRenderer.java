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
    private static final int STACKS = 64;
    private static final int SLICES = 128;
    private static final float OUTER_RADIUS = PlanetRenderer.PLANET_RADIUS;
    private static final float ATMOSPHERE_INNER_OVERLAP = 0.999F;
    private static VertexBuffer shellBuffer;
    private static boolean bufferUnavailable;

    private AtmosphereShellRenderer()
    {
    }

    static void release() {
        if (shellBuffer != null) shellBuffer.close();
        shellBuffer = null;
        bufferUnavailable = false;
    }

    static void render(Matrix4f model, Vector3f sunDirectionMesh, Vector3f cameraPositionMesh,
                       Vector3f color, float strength, float alpha,
                       float shellScale, float nightFraction, float twilightStrength)
    {
        renderLayer(model,sunDirectionMesh,cameraPositionMesh,color,strength,alpha,shellScale,
                nightFraction,twilightStrength,0F,0);
    }

    /** 0: whole volume; 1/2/3: behind, between, and in front of cloud intersections. */
    static void renderLayer(Matrix4f model, Vector3f sunDirectionMesh, Vector3f cameraPositionMesh,
                       Vector3f color, float strength, float alpha,
                       float shellScale, float nightFraction, float twilightStrength,
                       float cloudRadius, int layer)
    {
        renderLayer(model,sunDirectionMesh,cameraPositionMesh,color,strength,alpha,shellScale,
                nightFraction,twilightStrength,cloudRadius,layer,null,0F);
    }

    static void renderLayer(Matrix4f model, Vector3f sunDirectionMesh, Vector3f cameraPositionMesh,
                       Vector3f color, float strength, float alpha,
                       float shellScale, float nightFraction, float twilightStrength,
                       float cloudRadius, int layer,
                       com.starboundmc.world.universe.BodySpaceVisualProfile profile, float spin)
    {
        boolean scattering = SpaceSceneTarget.linear() && AtmosphereShader.scattering() != null;
        if (!scattering && layer > 1) return; // one glow if scattering becomes unavailable mid-frame
        ShaderInstance activeShader = scattering ? AtmosphereShader.scattering() : AtmosphereShader.current();
        if (activeShader == null || strength <= 0.0F || alpha <= 0.0F)
            return;

        VertexBuffer shell = getShellBuffer();
        if (shell == null)
            return;

        ShaderInstance previousShader = RenderSystem.getShader();
        try
        {
            FogRenderer.setupNoFog();
            if (scattering) {
                SpaceSceneTarget.prepareAtmosphere(activeShader);
                AtmosphereSolarLut.bind(activeShader);
                RenderSystem.disableBlend(); RenderSystem.disableDepthTest();
                // Exterior observers need only the front shell. Inside observers see the exit face.
                if (cameraPositionMesh.lengthSquared() < OUTER_RADIUS * OUTER_RADIUS)
                    RenderSystem.disableCull();
                else RenderSystem.enableCull();
            } else {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.enableDepthTest(); RenderSystem.enableCull();
            }
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(() -> activeShader);

            try
            {
                // Keep one shared sphere VBO. The profile scale only affects the
                // model transform and the shell-space radii sent to the shader.
                AtmosphereOpticalDepth.Radii radii = AtmosphereOpticalDepth.forShellWithInnerOverlap(
                        OUTER_RADIUS, shellScale, scattering ? 1F : ATMOSPHERE_INNER_OVERLAP);
                AtmosphereShader.setLighting(activeShader, sunDirectionMesh, cameraPositionMesh,
                        color, strength, alpha, radii.innerRadius(), radii.outerRadius(),
                        radii.maxOpticalDepth(),
                        nightFraction, twilightStrength);
                SpaceSceneTarget.configure(activeShader);
                RingRenderer.configureShadow(activeShader,profile,spin,alpha);
                if (scattering) {
                    activeShader.safeGetUniform("CloudRadius").set(cloudRadius);
                    activeShader.safeGetUniform("AtmosphereLayer").set(layer);
                    Vector3f scale = model.getScale(new Vector3f());
                    activeShader.safeGetUniform("MeshToUniverse").set(scale.x);
                    SpaceVisualQuality quality = StarfieldClientConfig.SPACE_VISUAL_QUALITY.get();
                    activeShader.safeGetUniform("ViewSamples").set(quality.atmosphereSamples());
                    activeShader.safeGetUniform("LightSamples").set(quality.atmosphereLightSamples());
                }
                shell.bind();
                shell.drawWithShader(model, RenderSystem.getProjectionMatrix(), activeShader);
            }
            catch (RuntimeException shaderFailure)
            {
                if (scattering) AtmosphereShader.disableScattering(shaderFailure);
                else AtmosphereShader.disableAfterFailure(activeShader, shaderFailure);
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
