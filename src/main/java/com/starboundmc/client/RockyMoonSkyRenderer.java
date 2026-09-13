package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.starboundmc.StarboundMC;
import com.starboundmc.world.Planet;
import com.starboundmc.world.RockyMoonPlanet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Custom sky for the Rocky Moon. The dimension runs with SkyType.NONE, so this
 * draws the deep-space view: the ship dimension's space dome and starfield
 * (renderSpaceDome/renderStarField) as the backdrop, a vanilla-style square
 * sun, and — the whole point of the outpost — the banded gas giant it orbits,
 * hanging opposite the sun with a phase, like an inverse of the Lush/Molten
 * pair seen from the overworld.
 *
 * <p>Uses the same celestial frame as vanilla sun/moon
 * ({@code Ry(-90) * Rx(timeOfDay*360)}), with the sun fixed at celestial +Y.</p>
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public class RockyMoonSkyRenderer
{
    private static final float GIANT_DISTANCE = 110.0F;
    /**
     * The giant orbits at 7 primary radii (ShipSpace compression), so its real
     * angular radius is 1/7 rad ≈ 8.2 degrees. Placed on the 110-unit sky shell
     * that reads as 110/7 ≈ 15.7 sky units — the giant now looms over the
     * horizon instead of hanging as a distant medal.
     */
    private static final float GIANT_ORBIT_RADII = 7.0F;
    private static final float GIANT_SCALE =
            GIANT_DISTANCE / (GIANT_ORBIT_RADII * PlanetRenderer.PLANET_RADIUS);
    /** Opens the ring ellipse toward the viewer, mirroring the berth-side tilt. */
    private static final float RING_TILT_DEGREES = 24.0F;
    /** The giant's disk is a shade under the sun-side rock brightness. */
    private static final float GIANT_BRIGHTNESS = 0.95F;

    /**
     * Vanilla sun texture and quad shape at 1/5 linear size: the outpost orbits
     * far out on the system's outer edge, so the sun reads as a small, distant
     * square (6 half-size at sky height 100) rather than the overworld's huge one.
     */
    private static final ResourceLocation VANILLA_SUN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/sun.png");
    private static final float SUN_DISTANCE = 100.0F;
    private static final float SUN_HALF_SIZE = 30.0F / 5.0F;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel))
            return;
        ClientLevel level = (ClientLevel) mc.level;
        if (!RockyMoonPlanet.ROCKY_MOON_LEVEL.equals(level.dimension()))
            return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        // AFTER_SKY hands out a fresh identity stack: the dispatch passes null
        // for the pose stack and the event constructor substitutes a new
        // PoseStack(). The camera rotation lives only in getModelViewMatrix()
        // (vanilla renderSky does the same mulPose on its own local stack), so
        // without this the whole sky is pinned to the screen instead of the
        // world. The global RenderSystem ModelView is still identity here too.
        pose.mulPose(event.getModelViewMatrix());
        PlanetRenderer.renderSpaceDome(pose);
        // Fixed world-anchored stars: the yaw/pitch parameters rotate the shell
        // with the ship heading, which the moon surface has none of.
        PlanetRenderer.renderStarField(pose, 0.0F, 0.0F, 1.0F, 0.0F, 0xFFFFFF, 0.0F);
        renderSunAndGiant(pose, level, partialTick);
    }

    private static void renderSunAndGiant(PoseStack pose, ClientLevel level, float partialTick)
    {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));

        renderVanillaSun(pose);

        // The parent rides opposite the sun and shows a phase, exactly like the
        // overworld's Molten moon rides opposite the sun for Lush. The ring is
        // drawn in two passes around the disc (far half, disc, near half) so it
        // reads as orbiting the body, same layering as the berth view.
        float phaseAngle = (level.getMoonPhase() & 7) * (float) Math.PI / 4.0F;
        Vector3f sunLocal = new Vector3f((float) Math.cos(phaseAngle), 0.0F, (float) Math.sin(phaseAngle));
        pose.pushPose();
        pose.translate(0.0F, -GIANT_DISTANCE, 0.0F);
        // Roll the equator toward the viewer so the bands read across the disk.
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        Matrix4f ringModel = new Matrix4f().scale(GIANT_SCALE);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(RING_TILT_DEGREES));
        PlanetRenderer.drawRingPass(pose, ringModel, 1.0F, false);
        pose.popPose();
        PlanetRenderer.drawPlanetSphere(pose, pose.last().pose(), Planet.GAS_GIANT.texture(),
                0.0F, 0.0F, 0.0F, GIANT_SCALE, sunLocal, GIANT_BRIGHTNESS, 1.0F);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(RING_TILT_DEGREES));
        PlanetRenderer.drawRingPass(pose, ringModel, 1.0F, true);
        pose.popPose();
        pose.popPose();
        pose.popPose();
    }

    /** Vanilla-style square sun: the same textured additive quad the overworld draws. */
    private static void renderVanillaSun(PoseStack pose)
    {
        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, VANILLA_SUN_TEXTURE);

        Matrix4f matrix = pose.last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.addVertex(matrix, -SUN_HALF_SIZE, SUN_DISTANCE, -SUN_HALF_SIZE).setUv(0.0F, 0.0F);
        bb.addVertex(matrix, SUN_HALF_SIZE, SUN_DISTANCE, -SUN_HALF_SIZE).setUv(1.0F, 0.0F);
        bb.addVertex(matrix, SUN_HALF_SIZE, SUN_DISTANCE, SUN_HALF_SIZE).setUv(1.0F, 1.0F);
        bb.addVertex(matrix, -SUN_HALF_SIZE, SUN_DISTANCE, SUN_HALF_SIZE).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }
}
