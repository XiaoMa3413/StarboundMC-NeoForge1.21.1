// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.space;
import com.starboundmc.StarboundMC;
import com.starboundmc.encounter.*;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Non-interactive distant hull/solar-array proxy converges on the reserved block-space anchor. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class RelayProxyRenderer {
    private static BlockPos trackedOrigin;
    private static double lastFrame = Double.NaN;
    private static double localReadyTicks;
    private static float proxyAlpha = 1;

    private RelayProxyRenderer() { }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance(); var state = RelayClientState.snapshot;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || mc.level == null
                || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL) || state == null
                || !(state.phase() == RelayData.Phase.APPROACHING.ordinal()
                || state.phase() == RelayData.Phase.ACTIVE.ordinal())) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) reset();
            return;
        }
        double partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = mc.level.getGameTime() + partial;
        if (!state.origin().equals(trackedOrigin)) {
            trackedOrigin = state.origin(); lastFrame = now; localReadyTicks = 0; proxyAlpha = 1;
        }
        double elapsed = Double.isFinite(lastFrame) ? Math.clamp(now - lastFrame, 0, 2) : 0;
        lastFrame = now;
        boolean active = state.phase() == RelayData.Phase.ACTIVE.ordinal();
        boolean blocksReady = active && footprintLoaded(mc.level, state.origin());
        localReadyTicks = blocksReady ? localReadyTicks + elapsed : 0;
        proxyAlpha = RelayProxyView.advanceAlpha(proxyAlpha,
                blocksReady && localReadyTicks >= RelayProxyView.LOCAL_READY_HOLD_TICKS, elapsed);
        if (proxyAlpha <= .002f) return;

        double extra = state.outsideCrew() > 0 ? 0 : Math.clamp(mc.level.getGameTime() - RelayClientState.receivedTick
                + partial, 0, 10);
        double t = active ? 1
                : Math.clamp((state.ticks() + extra) / RelayEncounter.APPROACH_TICKS, 0, 1);
        var target = RelayGeometry.center(state.origin());
        var camera = event.getCamera().getPosition();
        var placement = RelayProxyView.placement(camera, target, RelayProxyView.approachDistanceFactor(t),
                mc.options.renderDistance().get());
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(placement.center().x - camera.x, placement.center().y - camera.y, placement.center().z - camera.z);
        float scale = (float) placement.scale();
        pose.scale(scale, scale, scale);
        pose.translate(-RelayGeometry.WIDTH / 2.0, -RelayGeometry.HEIGHT / 2.0, -RelayGeometry.DEPTH / 2.0);
        var buffers = mc.renderBuffers().bufferSource(); var type = RenderType.debugQuads(); var vertices = buffers.getBuffer(type);
        float a = proxyAlpha;
        // Match the authored template's deck, roof caps, docking porch and array banks.
        box(pose, vertices, 8, 4, 6, 23, 9, 25, .32f, .36f, .38f, a);
        for (int x : new int[]{8, 22}) {
            box(pose, vertices, x - .02, 6, 7, x + 1.02, 7, 24, .10f, .19f, .22f, a);
            box(pose, vertices, x - .03, 7, 7, x + 1.03, 8, 24, .62f, .65f, .65f, a);
        }
        for (int z : new int[]{7, 17}) {
            box(pose, vertices, 10, 9, z, 21, 10, z + 7, .62f, .65f, .65f, a);
            box(pose, vertices, 12, 10, z + 1, 19, 11, z + 6, .72f, .74f, .73f, a);
        }
        for (int z : new int[]{6, 14, 16, 24}) {
            box(pose, vertices, 7, 3, z, 24, 4, z + 1, .19f, .22f, .24f, a);
            box(pose, vertices, 7, 9, z, 24, 10, z + 1, .19f, .22f, .24f, a);
            for (int x : new int[]{7, 23})
                box(pose, vertices, x, 4, z, x + 1, 9, z + 1, .62f, .65f, .65f, a);
        }
        box(pose, vertices, 9, 4, 2, 14, 5, 6, .19f, .22f, .24f, a);
        box(pose, vertices, 9, 8, 2, 14, 9, 6, .62f, .65f, .65f, a);
        for (int x : new int[]{9, 13}) {
            box(pose, vertices, x, 5, 2, x + 1, 8, 6, .32f, .36f, .38f, a);
            box(pose, vertices, x, 5, 1.98, x + 1, 6, 2, .96f, .76f, .36f, a);
            box(pose, vertices, x, 7, 1.98, x + 1, 8, 2, .82f, .64f, .12f, a);
        }
        box(pose, vertices, 2, 6, 15, 29, 7, 16, .19f, .22f, .24f, a);
        for (int x : new int[]{3, 27})
            box(pose, vertices, x, 6, 7, x + 1, 7, 24, .57f, .60f, .61f, a);
        for (int x : new int[]{1, 25}) for (int z : new int[]{7, 17}) {
            box(pose, vertices, x, 7, z, x + 5, 8, z + 7, .19f, .22f, .24f, a);
            box(pose, vertices, x + 1, 8, z + 1, x + 4, 9, z + 6, .08f, .23f, .48f, a);
            for (int row = 1; row < 6; row++)
                box(pose, vertices, x + 1, 9.01, z + row, x + 4, 9.04, z + row + .06, .28f, .48f, .66f, a);
        }
        box(pose, vertices, 15, 10, 20, 16, 12, 21, .19f, .22f, .24f, a);
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            int radius = dx * dx + dz * dz;
            if (radius > 20) continue;
            int y = radius <= 5 ? 12 : radius <= 13 ? 13 : 14;
            float color = radius <= 13 ? .82f : .60f;
            box(pose, vertices, 15 + dx, y, 20 + dz, 16 + dx, y + 1, 21 + dz, color, color, color * .97f, a);
        }
        box(pose, vertices, 15.4, 13, 20.4, 15.6, 16, 20.6, .90f, .93f, .86f, a);
        box(pose, vertices, 19.4, 10, 9.4, 19.6, 14, 9.6, .55f, .60f, .62f, a);
        box(pose, vertices, 19, 14, 9, 20, 15, 10, .70f, .15f, .12f, a);
        buffers.endBatch(type); pose.popPose();
    }

    private static void box(com.mojang.blaze3d.vertex.PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vertices,
                            double x0, double y0, double z0, double x1, double y1, double z1,
                            float red, float green, float blue, float alpha) {
        var matrix = pose.last().pose();
        float left = (float) x0, right = (float) x1, bottom = (float) y0, top = (float) y1;
        float front = (float) z0, back = (float) z1;
        face(matrix, vertices, red, green, blue, alpha, 1f,
                left, top, front, right, top, front, right, top, back, left, top, back);
        face(matrix, vertices, red, green, blue, alpha, .48f,
                left, bottom, back, right, bottom, back, right, bottom, front, left, bottom, front);
        face(matrix, vertices, red, green, blue, alpha, .82f,
                left, bottom, front, right, bottom, front, right, top, front, left, top, front);
        face(matrix, vertices, red, green, blue, alpha, .68f,
                right, bottom, back, left, bottom, back, left, top, back, right, top, back);
        face(matrix, vertices, red, green, blue, alpha, .60f,
                left, bottom, back, left, bottom, front, left, top, front, left, top, back);
        face(matrix, vertices, red, green, blue, alpha, .74f,
                right, bottom, front, right, bottom, back, right, top, back, right, top, front);
    }

    private static void face(org.joml.Matrix4f matrix, com.mojang.blaze3d.vertex.VertexConsumer vertices,
                             float red, float green, float blue, float alpha, float shade,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        vertices.addVertex(matrix, x0, y0, z0).setColor(red * shade, green * shade, blue * shade, alpha);
        vertices.addVertex(matrix, x1, y1, z1).setColor(red * shade, green * shade, blue * shade, alpha);
        vertices.addVertex(matrix, x2, y2, z2).setColor(red * shade, green * shade, blue * shade, alpha);
        vertices.addVertex(matrix, x3, y3, z3).setColor(red * shade, green * shade, blue * shade, alpha);
    }

    static boolean footprintLoaded(ClientLevel level, BlockPos origin) {
        int y = origin.getY() + RelayGeometry.HEIGHT / 2;
        int minChunkX = origin.getX() >> 4, maxChunkX = (origin.getX() + RelayGeometry.WIDTH - 1) >> 4;
        int minChunkZ = origin.getZ() >> 4, maxChunkZ = (origin.getZ() + RelayGeometry.DEPTH - 1) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
                if (!level.hasChunkAt(new BlockPos(chunkX << 4, y, chunkZ << 4))) return false;
        return true;
    }

    static void reset() {
        trackedOrigin = null; lastFrame = Double.NaN; localReadyTicks = 0; proxyAlpha = 1;
    }
}
