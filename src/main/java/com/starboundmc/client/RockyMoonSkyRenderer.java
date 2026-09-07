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
import com.starboundmc.world.Planet;
import com.starboundmc.world.RockyMoonPlanet;
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
import org.joml.Vector3f;

import java.util.Random;

/**
 * Custom sky for the Rocky Moon. The dimension runs with SkyType.NONE, so this
 * draws the airless view: a black dome with a persistent starfield (visible in
 * daylight too), the distant main star, and — the whole point of the outpost —
 * the banded gas giant it orbits, hanging opposite the sun with a phase, like
 * an inverse of the Lush/Molten pair seen from the overworld.
 *
 * <p>Uses the same celestial frame as vanilla sun/moon
 * ({@code Ry(-90) * Rx(timeOfDay*360)}), with the sun fixed at celestial +Y.</p>
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public class RockyMoonSkyRenderer
{
    private static final Vec3 LOCAL_STAR_DIRECTION = new Vec3(0.0, 1.0, 0.0);
    private static final float GIANT_DISTANCE = 110.0F;
    /** A 30 r giant at 420 units subtends ~4.1 degrees: 8-unit radius at sky distance 110. */
    private static final float GIANT_SCALE = 8.0F / PlanetRenderer.PLANET_RADIUS;
    /** The giant's disk is a shade under the sun-side rock brightness. */
    private static final float GIANT_BRIGHTNESS = 0.95F;

    private static final int STAR_COUNT = 900;
    private static final float STAR_SHELL = 460.0F;
    /** Per-star position plus an in-plane tangent basis, all baked once. */
    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final float[] STAR_Z = new float[STAR_COUNT];
    private static final float[] STAR_TX = new float[STAR_COUNT];
    private static final float[] STAR_TY = new float[STAR_COUNT];
    private static final float[] STAR_TZ = new float[STAR_COUNT];
    private static final float[] STAR_BX = new float[STAR_COUNT];
    private static final float[] STAR_BY = new float[STAR_COUNT];
    private static final float[] STAR_BZ = new float[STAR_COUNT];
    private static final float[] STAR_BRIGHT = new float[STAR_COUNT];

    static
    {
        Random rng = new Random(0x5731A5DEL);
        for (int i = 0; i < STAR_COUNT; i++)
        {
            double theta = rng.nextDouble() * Math.PI * 2.0;
            double y = rng.nextDouble() * 2.0 - 1.0;
            double radius = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            float nx = (float) (Math.cos(theta) * radius);
            float ny = (float) y;
            float nz = (float) (Math.sin(theta) * radius);
            STAR_X[i] = nx * STAR_SHELL;
            STAR_Y[i] = ny * STAR_SHELL;
            STAR_Z[i] = nz * STAR_SHELL;
            // Tangent basis perpendicular to the radial direction; the up-axis
            // singular at the poles is avoided by switching helper axes.
            float hx = 0.0F, hy = 1.0F, hz = 0.0F;
            if (Math.abs(ny) > 0.99F)
            {
                hx = 1.0F; hy = 0.0F; hz = 0.0F;
            }
            float tx = hy * nz - hz * ny;
            float ty = hz * nx - hx * nz;
            float tz = hx * ny - hy * nx;
            float tl = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            tx /= tl; ty /= tl; tz /= tl;
            STAR_TX[i] = tx; STAR_TY[i] = ty; STAR_TZ[i] = tz;
            STAR_BX[i] = ny * tz - nz * ty;
            STAR_BY[i] = nz * tx - nx * tz;
            STAR_BZ[i] = nx * ty - ny * tx;
            STAR_BRIGHT[i] = 0.35F + rng.nextFloat() * 0.65F;
        }
    }

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
        renderVoidDome(event.getPoseStack());
        renderStarfield(event.getPoseStack());
        renderSunAndGiant(event.getPoseStack(), level, partialTick);
    }

    /** Pure black cube shell: guarantees deep-space background under the stars. */
    private static void renderVoidDome(PoseStack pose)
    {
        Matrix4f matrix = pose.last().pose();
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float s = 490.0F;
        // Near-black with the faintest grey rising at the very bottom edge so
        // the rock horizon remains legible against the void.
        addDomeFace(bb, matrix, -s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s);
        addDomeFace(bb, matrix, -s, -s, s, s, -s, s, s, s, s, -s, s, s);
        addDomeFace(bb, matrix, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s);
        addDomeFace(bb, matrix, s, -s, -s, s, -s, s, s, s, s, s, s, -s);
        addDomeFace(bb, matrix, -s, s, -s, s, s, -s, s, s, s, -s, s, s);
        addDomeFace(bb, matrix, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void addDomeFace(BufferBuilder bb, Matrix4f matrix,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float x4, float y4, float z4)
    {
        vertexColor(bb, matrix, x1, y1, z1, domeColor(y1));
        vertexColor(bb, matrix, x2, y2, z2, domeColor(y2));
        vertexColor(bb, matrix, x3, y3, z3, domeColor(y3));
        vertexColor(bb, matrix, x4, y4, z4, domeColor(y4));
    }

    private static float[] domeColor(float y)
    {
        float t = Mth.clamp((y + 490.0F) / 980.0F, 0.0F, 1.0F);
        float v = 0.005F + 0.03F * (1.0F - t);
        return new float[] { v, v, v * 1.1F, 1.0F };
    }

    /** Airless rock shows the stars straight after the horizon, day or night. */
    private static void renderStarfield(PoseStack pose)
    {
        Matrix4f matrix = pose.last().pose();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < STAR_COUNT; i++)
        {
            float size = 0.6F + STAR_BRIGHT[i] * 0.8F;
            float a = STAR_BRIGHT[i];
            float x = STAR_X[i], y = STAR_Y[i], z = STAR_Z[i];
            float tx = STAR_TX[i] * size, ty = STAR_TY[i] * size, tz = STAR_TZ[i] * size;
            float bx = STAR_BX[i] * size, by = STAR_BY[i] * size, bz = STAR_BZ[i] * size;
            bb.addVertex(matrix, x - tx - bx, y - ty - by, z - tz - bz).setColor(1.0F, 1.0F, 1.0F, a);
            bb.addVertex(matrix, x - tx + bx, y - ty + by, z - tz + bz).setColor(1.0F, 1.0F, 1.0F, a);
            bb.addVertex(matrix, x + tx + bx, y + ty + by, z + tz + bz).setColor(1.0F, 1.0F, 1.0F, a);
            bb.addVertex(matrix, x + tx - bx, y + ty - by, z + tz - bz).setColor(1.0F, 1.0F, 1.0F, a);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void renderSunAndGiant(PoseStack pose, ClientLevel level, float partialTick)
    {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));

        StellarRenderer.render(pose, StarSystems.byId(StarSystems.SYS_MAIN).getStellarVisual(),
                LOCAL_STAR_DIRECTION, 100.0F, 0.95F,
                level.getGameTime() + partialTick, 0.85F);

        // The parent rides opposite the sun and shows a phase, exactly like the
        // overworld's Molten moon rides opposite the sun for Lush.
        float phaseAngle = (level.getMoonPhase() & 7) * (float) Math.PI / 4.0F;
        Vector3f sunLocal = new Vector3f((float) Math.cos(phaseAngle), 0.0F, (float) Math.sin(phaseAngle));
        pose.pushPose();
        pose.translate(0.0F, -GIANT_DISTANCE, 0.0F);
        // Roll the equator toward the viewer so the bands read across the disk.
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        PlanetRenderer.drawPlanetSphere(pose, pose.last().pose(), Planet.GAS_GIANT.texture(),
                0.0F, 0.0F, 0.0F, GIANT_SCALE, sunLocal, GIANT_BRIGHTNESS, 1.0F);
        pose.popPose();
        pose.popPose();
    }

    private static void vertexColor(BufferBuilder bb, Matrix4f matrix, float x, float y, float z, float[] rgba)
    {
        bb.addVertex(matrix, x, y, z).setColor(rgba[0], rgba[1], rgba[2], rgba[3]);
    }
}
