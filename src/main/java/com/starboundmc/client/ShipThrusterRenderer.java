package com.starboundmc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.ShipEngineBlock;
import com.starboundmc.block.entity.ShipEngineBlockEntity;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

import java.lang.ref.WeakReference;

/** The solid nozzle is baked from the Blockbench OBJ; only hot gas is rendered here. */
public final class ShipThrusterRenderer implements BlockEntityRenderer<ShipEngineBlockEntity> {
    private static final int SIDES = 16;
    private static final float MOUTH_Z = .045f;
    private static final float[] DISTANCE = {0, .16f, .31f, .48f, .63f, .80f, 1};
    private static final float[] RADIUS = {.37f, .30f, .32f, .22f, .24f, .14f, .002f};
    private static final float[] LINER_Z = {.81f, .69f, .51f, .40f, .29f, .175f, MOUTH_Z};
    private static final float[] LINER_R = {.115f, .148f, .279f, .340f, .388f, .419f, .436f};
    private WeakReference<Level> animationLevel = new WeakReference<>(null);
    private float frozenTime;
    private float frozenIntensity;

    public ShipThrusterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ShipEngineBlockEntity engine, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        var level = engine.getLevel();
        // The legacy BE type is also used by the separate, indoor ignition machine.
        if (level == null || !level.dimension().equals(ShipDimensions.SHIP_LEVEL)
                || !engine.getBlockState().is(ModBlocks.SHIP_ENGINE.get())) return;
        if (animationLevel.get() != level) {
            animationLevel = new WeakReference<>(level);
            frozenTime = 0;
            frozenIntensity = 0;
        }
        if (!Minecraft.getInstance().isPaused()) {
            frozenTime = (level.getGameTime() % 24000) + partialTick;
            frozenIntensity = ClientPlanetState.thrusterIntensity();
        }
        if (frozenIntensity < .005f) return;
        var facing = engine.getBlockState().getValue(ShipEngineBlock.FACING);
        var outlet = engine.getBlockPos().relative(facing);
        if (level.getBlockState(outlet).isSolidRender(level, outlet)) return;

        float phase = frozenTime * .7f + (engine.getBlockPos().asLong() & 255) * .17f;
        float flutter = 1 + .025f * (float) Math.sin(phase) + .015f * (float) Math.sin(phase * 1.73);
        float length = (1.5f + .95f * frozenIntensity) * flutter;
        pose.pushPose();
        pose.translate(.5, .5, .5);
        float rotation = -facing.toYRot() + 180;
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-.5, -.5, -.5);
        var consumer = buffers.getBuffer(ThrusterRenderTypes.FLAME);
        // Additive layers saturate to a white-hot core, retaining a blue perimeter.
        plume(consumer, pose, length, 1.06f, .48f * frozenIntensity, 26, 98, 255);
        plume(consumer, pose, length * .91f, .85f, .78f * frozenIntensity, 60, 170, 255);
        plume(consumer, pose, length * .76f, .62f, frozenIntensity, 223, 245, 255);
        plume(consumer, pose, length * .55f, .42f, frozenIntensity, 255, 252, 243);
        // Small luminous knots break up the smooth cone into an energetic jet.
        for (int j = 0; j < 3; j++) {
            float z = MOUTH_Z - length * (.22f + j * .21f);
            float radius = .17f - j * .035f;
            float alpha = (.70f + .12f * (float) Math.sin(phase - j)) * frozenIntensity;
            ringSegment(consumer, pose, z + .15f, .015f, z, radius,
                    0, .10f, 175, 225, 255, alpha);
            ringSegment(consumer, pose, z, radius, z - .23f, .006f,
                    .10f, .36f, 175, 225, 255, alpha);
        }
        for (int j = 0; j < LINER_Z.length - 1; j++) {
            ringSegment(consumer, pose, LINER_Z[j], LINER_R[j], LINER_Z[j + 1], LINER_R[j + 1],
                    .06f, .06f, 69, 164, 255, .65f * frozenIntensity);
        }
        // Camera-facing, analytic falloff creates soft glow without post-processing or bloom mods.
        var halos = buffers.getBuffer(ThrusterRenderTypes.HALO);
        Quaternionf camera = Axis.YP.rotationDegrees(-rotation)
                .mul(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        glow(halos, pose, camera, MOUTH_Z - .03f, .80f, .52f * frozenIntensity);
        glow(halos, pose, camera, MOUTH_Z - length * .28f, .67f, .38f * frozenIntensity);
        glow(halos, pose, camera, MOUTH_Z - length * .55f, .46f, .27f * frozenIntensity);
        glow(halos, pose, camera, MOUTH_Z - length * .78f, .30f, .16f * frozenIntensity);
        pose.popPose();
    }

    private static void glow(VertexConsumer out, PoseStack pose, Quaternionf camera,
                             float z, float radius, float alpha) {
        pose.pushPose();
        pose.translate(.5f, .5f, z);
        pose.mulPose(camera);
        glowVertex(out, pose, -radius, -radius, 0, 0, alpha);
        glowVertex(out, pose, radius, -radius, 1, 0, alpha);
        glowVertex(out, pose, radius, radius, 1, 1, alpha);
        glowVertex(out, pose, -radius, radius, 0, 1, alpha);
        pose.popPose();
    }

    private static void glowVertex(VertexConsumer out, PoseStack pose, float x, float y,
                                   float u, float v, float alpha) {
        out.addVertex(pose.last(), x, y, 0).setUv(u, v)
                .setColor(37, 118, 255, Math.round(alpha * 255));
    }

    private static void plume(VertexConsumer out, PoseStack pose, float length, float width,
                              float alpha, int r, int g, int b) {
        for (int j = 0; j < DISTANCE.length - 1; j++) {
            ringSegment(out, pose, MOUTH_Z - DISTANCE[j] * length, RADIUS[j] * width,
                    MOUTH_Z - DISTANCE[j + 1] * length, RADIUS[j + 1] * width,
                    DISTANCE[j], DISTANCE[j + 1], r, g, b, alpha);
        }
    }

    private static void ringSegment(VertexConsumer out, PoseStack pose,
                                    float z0, float radius0, float z1, float radius1,
                                    float u0, float u1, int r, int g, int b, float alpha) {
        for (int i = 0; i < SIDES; i++) {
            float a = (float) ((i + .5) * Math.PI * 2 / SIDES);
            float next = (float) ((i + 1.5) * Math.PI * 2 / SIDES);
            vertex(out, pose, a, radius0, z0, u0, (float) i / SIDES, r, g, b, alpha);
            vertex(out, pose, next, radius0, z0, u0, (float) (i + 1) / SIDES, r, g, b, alpha);
            vertex(out, pose, next, radius1, z1, u1, (float) (i + 1) / SIDES, r, g, b, alpha);
            vertex(out, pose, a, radius1, z1, u1, (float) i / SIDES, r, g, b, alpha);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack pose, float angle, float radius,
                               float z, float u, float v, int r, int g, int b, float alpha) {
        float nx = (float) Math.cos(angle), ny = (float) Math.sin(angle);
        out.addVertex(pose.last(), .5f + nx * radius, .5f + ny * radius, z)
                .setColor(r, g, b, Math.round(alpha * 255))
                .setUv(u, v);
    }

    @Override
    public AABB getRenderBoundingBox(ShipEngineBlockEntity engine) {
        // Keep the plume visible when its nozzle is just outside the camera frustum.
        return new AABB(engine.getBlockPos()).inflate(3);
    }
}
