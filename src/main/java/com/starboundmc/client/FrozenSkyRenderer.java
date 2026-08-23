package com.starboundmc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.starboundmc.StarboundMC;
import com.starboundmc.world.FrozenPlanet;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Custom sky for the Frozen Planet. The dimension uses {@link FrozenDimensionEffects}
 * with SkyType.NONE, so this renderer draws the harsh cold sky and a much smaller
 * sun instead of the vanilla one.
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public class FrozenSkyRenderer
{
    private static final Vec3 LOCAL_STAR_DIRECTION = new Vec3(0.0, 1.0, 0.0);

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel))
            return;
        ClientLevel level = (ClientLevel) mc.level;
        if (!FrozenPlanet.FROZEN_LEVEL.equals(level.dimension()))
            return;

        renderSky(event.getPoseStack(), level,
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    private static void renderSky(PoseStack pose, ClientLevel level, float partialTick)
    {
        renderSkyDome(pose);
        renderRedDwarf(pose, level, partialTick);
    }

    private static void renderSkyDome(PoseStack pose)
    {
        Matrix4f matrix = pose.last().pose();
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float s = 500.0F;
        // A cube around the camera acts as a skybox: it covers every view direction
        // instead of a flat quad that can look like a wall from the side.
        addFace(bb, matrix, -s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s);
        addFace(bb, matrix, -s, -s, s, s, -s, s, s, s, s, -s, s, s);
        addFace(bb, matrix, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s);
        addFace(bb, matrix, s, -s, -s, s, -s, s, s, s, s, s, s, -s);
        addFace(bb, matrix, -s, s, -s, s, s, -s, s, s, s, -s, s, s);
        addFace(bb, matrix, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s);

        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void addFace(BufferBuilder bb, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4)
    {
        vertexColor(bb, matrix, x1, y1, z1, skyColor(y1));
        vertexColor(bb, matrix, x2, y2, z2, skyColor(y2));
        vertexColor(bb, matrix, x3, y3, z3, skyColor(y3));
        vertexColor(bb, matrix, x4, y4, z4, skyColor(y4));
    }

    private static float[] skyColor(float y)
    {
        float t = Mth.clamp((y + 500.0F) / 1000.0F, 0.0F, 1.0F);
        // Bottom: pale gray-blue; top: dark cold night blue.
        float r = Mth.lerp(t, 0.30F, 0.06F);
        float g = Mth.lerp(t, 0.36F, 0.10F);
        float b = Mth.lerp(t, 0.46F, 0.22F);
        return new float[] { r, g, b, 1.0F };
    }

    private static void renderRedDwarf(PoseStack pose, ClientLevel level, float partialTick)
    {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
        StellarRenderer.render(pose, StarSystems.byId(StarSystems.SYS_COLD).getStellarVisual(),
                LOCAL_STAR_DIRECTION, 100.0F, 0.90F,
                level.getGameTime() + partialTick, 0.92F);
        pose.popPose();
    }

    private static void vertexColor(BufferBuilder bb, Matrix4f matrix, float x, float y, float z,
                                    float r, float g, float b, float a)
    {
        bb.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }

    private static void vertexColor(BufferBuilder bb, Matrix4f matrix, float x, float y, float z, float[] rgba)
    {
        vertexColor(bb, matrix, x, y, z, rgba[0], rgba[1], rgba[2], rgba[3]);
    }
}
