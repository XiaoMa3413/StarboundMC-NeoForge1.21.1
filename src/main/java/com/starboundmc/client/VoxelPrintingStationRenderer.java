package com.starboundmc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.starboundmc.block.VoxelPrintingStationBlock;
import com.starboundmc.block.entity.VoxelPrintingStationBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Open-bay printing effect. The item keeps its final proportions while its
 * baked quads are clipped at the current build layer. Two overhead probe
 * mounts stay fixed while their heads rotate to follow mirrored target points
 * sweeping across the full active build layer. Each beam reaches both ends.
 */
public final class VoxelPrintingStationRenderer
        implements BlockEntityRenderer<VoxelPrintingStationBlockEntity> {
    private static final float ITEM_BOTTOM_Y = 0.34F;
    private static final float ITEM_HEIGHT = 0.32F;
    private static final float ITEM_CENTER_Y = ITEM_BOTTOM_Y + ITEM_HEIGHT * 0.5F;
    private static final float LEFT_PROBE_PIVOT_X = 0.36F;
    private static final float RIGHT_PROBE_PIVOT_X = 0.64F;
    private static final float PROBE_PIVOT_Y = 0.80F;
    private static final float PROBE_PIVOT_Z = 0.50F;
    private static final float PROBE_TARGET_Z = 0.35F;
    private static final float PROBE_NOZZLE_LENGTH = 0.12F;
    private static final float PROBE_FULL_SWEEP_RADIUS = 0.175F;
    private static final float PROBE_SWEEP_SPEED = 0.16F;

    private final ItemRenderer itemRenderer;

    public VoxelPrintingStationRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(VoxelPrintingStationBlockEntity station, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        var snapshot = ClientVoxelMachineState.snapshotAt(station.getBlockPos());
        ItemStack result = ItemStack.EMPTY;
        float formation = 0.0F;
        boolean activelyPrinting = false;

        if (snapshot != null && snapshot.resultCount() > 0) {
            var item = BuiltInRegistries.ITEM.get(snapshot.resultItemId());
            if (item != Items.AIR) {
                result = new ItemStack(item, snapshot.resultCount());
                activelyPrinting = snapshot.progress() > 0;
                if (activelyPrinting) {
                    float remainingTicks = ClientVoxelMachineState.interpolatedRemainingTicksAt(
                            station.getBlockPos(), station.getLevel().getGameTime(), partialTick);
                    float elapsedTicks = snapshot.totalTicks() - remainingTicks;
                    formation = Mth.clamp(elapsedTicks / Math.max(1.0F, snapshot.totalTicks()), 0.02F, 1.0F);
                } else {
                    formation = 1.0F;
                }
            }
        }

        float scanY = result.isEmpty()
                ? ITEM_BOTTOM_Y
                : ITEM_BOTTOM_Y + ITEM_HEIGHT * formation;

        Direction facing = station.getBlockState().getValue(VoxelPrintingStationBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facing.toYRot() + 180.0F));
        pose.translate(-0.5, -0.5, -0.5);

        renderOverheadProbes(station, pose, buffers, scanY, activelyPrinting, partialTick);
        if (!result.isEmpty()) {
            renderHologramAndScan(station, pose, buffers, formation, scanY);
            renderFormingItem(station, result, pose, buffers, formation, packedOverlay);
        }
        pose.popPose();
    }

    private void renderOverheadProbes(VoxelPrintingStationBlockEntity station, PoseStack pose,
                                      MultiBufferSource buffers, float scanY,
                                      boolean activelyPrinting, float partialTick) {
        float sweepOffset = 0.0F;
        if (activelyPrinting) {
            float animationTime = station.getLevel().getGameTime() + partialTick;
            sweepOffset = Mth.sin(animationTime * PROBE_SWEEP_SPEED) * PROBE_FULL_SWEEP_RADIUS;
        }

        float targetY = activelyPrinting ? scanY : ITEM_BOTTOM_Y;
        float targetZ = activelyPrinting ? PROBE_TARGET_Z : PROBE_PIVOT_Z;
        float leftTargetX = activelyPrinting
                ? 0.5F + sweepOffset
                : LEFT_PROBE_PIVOT_X;
        float rightTargetX = activelyPrinting
                ? 0.5F - sweepOffset
                : RIGHT_PROBE_PIVOT_X;
        ProbeAim leftAim = createProbeAim(true, leftTargetX, targetY, targetZ);
        ProbeAim rightAim = createProbeAim(false, rightTargetX, targetY, targetZ);

        VertexConsumer solids = buffers.getBuffer(RenderType.debugFilledBox());
        renderProbeSolids(pose, solids, leftAim);
        renderProbeSolids(pose, solids, rightAim);

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        renderProbeOutlines(pose, lines, leftAim);
        renderProbeOutlines(pose, lines, rightAim);
        if (activelyPrinting) {
            drawBeam(lines, pose,
                    leftAim.nozzleX(), leftAim.nozzleY(), leftAim.nozzleZ(),
                    leftAim.targetX(), leftAim.targetY(), leftAim.targetZ());
            drawBeam(lines, pose,
                    rightAim.nozzleX(), rightAim.nozzleY(), rightAim.nozzleZ(),
                    rightAim.targetX(), rightAim.targetY(), rightAim.targetZ());
        }
    }

    private static ProbeAim createProbeAim(boolean left, float targetX, float targetY, float targetZ) {
        float pivotX = left ? LEFT_PROBE_PIVOT_X : RIGHT_PROBE_PIVOT_X;
        float directionX = targetX - pivotX;
        float directionY = targetY - PROBE_PIVOT_Y;
        float directionZ = targetZ - PROBE_PIVOT_Z;
        float inverseLength = 1.0F / Mth.sqrt(
                directionX * directionX + directionY * directionY + directionZ * directionZ);
        return new ProbeAim(
                pivotX, PROBE_PIVOT_Y, PROBE_PIVOT_Z,
                targetX, targetY, targetZ,
                directionX * inverseLength,
                directionY * inverseLength,
                directionZ * inverseLength);
    }

    private static void renderProbeSolids(PoseStack pose, VertexConsumer solids, ProbeAim aim) {
        // The mounting block stays attached to the underside of the top beam.
        addFilledBox(pose, solids,
                aim.pivotX() - 0.048, aim.pivotY() + 0.012, aim.pivotZ() - 0.0433,
                aim.pivotX() + 0.048, 0.84, aim.pivotZ() + 0.0433,
                0.67F, 0.70F, 0.68F, 1.0F);

        pose.pushPose();
        pose.translate(aim.pivotX(), aim.pivotY(), aim.pivotZ());
        pose.mulPose(probeRotation(aim));
        addFilledBox(pose, solids,
                -0.0387, -0.070, -0.0367,
                0.0387, 0.012, 0.0367,
                0.67F, 0.70F, 0.68F, 1.0F);
        addFilledBox(pose, solids,
                -0.0267, -0.100, -0.0267,
                0.0267, -0.070, 0.0267,
                0.10F, 0.16F, 0.19F, 1.0F);
        addFilledBox(pose, solids,
                -0.0227, -PROBE_NOZZLE_LENGTH, -0.0227,
                0.0227, -0.100, 0.0227,
                0.08F, 0.84F, 0.95F, 1.0F);
        pose.popPose();
    }

    private static void renderProbeOutlines(PoseStack pose, VertexConsumer lines, ProbeAim aim) {
        LevelRenderer.renderLineBox(pose, lines,
                new AABB(
                        aim.pivotX() - 0.048, aim.pivotY() + 0.012, aim.pivotZ() - 0.0433,
                        aim.pivotX() + 0.048, 0.84, aim.pivotZ() + 0.0433),
                0.11F, 0.17F, 0.19F, 0.9F);

        pose.pushPose();
        pose.translate(aim.pivotX(), aim.pivotY(), aim.pivotZ());
        pose.mulPose(probeRotation(aim));
        LevelRenderer.renderLineBox(pose, lines,
                new AABB(-0.0387, -0.070, -0.0367,
                        0.0387, 0.012, 0.0367),
                0.11F, 0.17F, 0.19F, 0.9F);
        LevelRenderer.renderLineBox(pose, lines,
                new AABB(-0.0227, -PROBE_NOZZLE_LENGTH, -0.0227,
                        0.0227, -0.100, 0.0227),
                0.55F, 0.98F, 1.0F, 0.98F);
        pose.popPose();
    }

    private static Quaternionf probeRotation(ProbeAim aim) {
        // Rotate the local downward axis (0, -1, 0) onto the normalized target direction.
        return new Quaternionf(
                -aim.directionZ(), 0.0F, aim.directionX(), 1.0F - aim.directionY())
                .normalize();
    }

    private static void addFilledBox(PoseStack pose, VertexConsumer consumer,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     float red, float green, float blue, float alpha) {
        LevelRenderer.addChainedFilledBoxVertices(
                pose, consumer, minX, minY, minZ, maxX, maxY, maxZ,
                red, green, blue, alpha);
    }

    private void renderHologramAndScan(VoxelPrintingStationBlockEntity station, PoseStack pose,
                                        MultiBufferSource buffers, float formation, float scanY) {
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        float pulse = 0.62F + 0.18F * (float) Math.sin(
                (station.getLevel().getGameTime() + formation * 10.0F) * 0.35F);
        AABB hologram = new AABB(0.32, ITEM_BOTTOM_Y, 0.27, 0.68, 0.68, 0.43);
        LevelRenderer.renderLineBox(pose, lines, hologram,
                0.30F, 0.90F, 1.0F, pulse);

        LevelRenderer.renderLineBox(pose, lines,
                0.29, scanY - 0.006, 0.25,
                0.71, scanY + 0.006, 0.45,
                0.72F, 0.98F, 1.0F, 0.95F);

    }

    private void renderFormingItem(VoxelPrintingStationBlockEntity station, ItemStack result,
                                   PoseStack pose, MultiBufferSource buffers,
                                   float formation, int packedOverlay) {
        pose.pushPose();
        pose.translate(0.5, ITEM_CENTER_Y, 0.35);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(0.32F, 0.32F, 0.32F);

        MultiBufferSource clippedBuffers = formation >= 0.999F
                ? buffers
                : renderType -> new LayerClippedVertexConsumer(buffers.getBuffer(renderType), formation);
        itemRenderer.renderStatic(result, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, packedOverlay, pose, clippedBuffers,
                station.getLevel(), (int) station.getBlockPos().asLong());
        pose.popPose();
    }

    private static void drawBeam(VertexConsumer consumer, PoseStack pose,
                                 float startX, float startY, float startZ,
                                 float endX, float endY, float endZ) {
        for (float offset : new float[]{-0.004F, 0.0F, 0.004F}) {
            consumer.addVertex(pose.last(), startX + offset, startY, startZ)
                    .setColor(120, 238, 255, 235)
                    .setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
            consumer.addVertex(pose.last(), endX + offset, endY, endZ)
                    .setColor(235, 255, 255, 255)
                    .setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
        }
    }

    private record ProbeAim(
            float pivotX, float pivotY, float pivotZ,
            float targetX, float targetY, float targetZ,
            float directionX, float directionY, float directionZ) {
        private float nozzleX() {
            return pivotX + directionX * PROBE_NOZZLE_LENGTH;
        }

        private float nozzleY() {
            return pivotY + directionY * PROBE_NOZZLE_LENGTH;
        }

        private float nozzleZ() {
            return pivotZ + directionZ * PROBE_NOZZLE_LENGTH;
        }
    }

    /**
     * Clips baked item quads in their native 0..1 model space. Intersections
     * interpolate position and UV data, so the visible portion stays at its
     * final size instead of being vertically compressed.
     */
    private static final class LayerClippedVertexConsumer implements VertexConsumer {
        private static final int VERTEX_STRIDE = 8;
        private final VertexConsumer delegate;
        private final float clipY;

        private LayerClippedVertexConsumer(VertexConsumer delegate, float clipY) {
            this.delegate = delegate;
            this.clipY = clipY;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            delegate.setNormal(normalX, normalY, normalZ);
            return this;
        }

        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad,
                                float red, float green, float blue, float alpha,
                                int packedLight, int packedOverlay, boolean readExistingColor) {
            for (BakedQuad clipped : clipQuad(quad, clipY)) {
                delegate.putBulkData(pose, clipped, red, green, blue, alpha,
                        packedLight, packedOverlay, readExistingColor);
            }
        }

        private static List<BakedQuad> clipQuad(BakedQuad quad, float clipY) {
            int[] packed = quad.getVertices();
            if (packed.length != VERTEX_STRIDE * 4) {
                return List.of(quad);
            }

            List<int[]> polygon = new ArrayList<>(4);
            for (int vertex = 0; vertex < 4; vertex++) {
                int[] data = new int[VERTEX_STRIDE];
                System.arraycopy(packed, vertex * VERTEX_STRIDE, data, 0, VERTEX_STRIDE);
                polygon.add(data);
            }

            List<int[]> clipped = new ArrayList<>(5);
            int[] previous = polygon.get(polygon.size() - 1);
            boolean previousInside = vertexY(previous) <= clipY;
            for (int[] current : polygon) {
                boolean currentInside = vertexY(current) <= clipY;
                if (previousInside != currentInside) {
                    float denominator = vertexY(current) - vertexY(previous);
                    float factor = denominator == 0.0F
                            ? 0.0F
                            : (clipY - vertexY(previous)) / denominator;
                    clipped.add(interpolate(previous, current, Mth.clamp(factor, 0.0F, 1.0F)));
                }
                if (currentInside) {
                    clipped.add(current);
                }
                previous = current;
                previousInside = currentInside;
            }

            if (clipped.size() < 3) {
                return List.of();
            }
            if (clipped.size() == 4) {
                return List.of(copyQuad(quad, clipped.get(0), clipped.get(1), clipped.get(2), clipped.get(3)));
            }

            List<BakedQuad> triangles = new ArrayList<>(clipped.size() - 2);
            for (int index = 1; index < clipped.size() - 1; index++) {
                int[] first = clipped.get(0);
                int[] second = clipped.get(index);
                int[] third = clipped.get(index + 1);
                triangles.add(copyQuad(quad, first, second, third, third));
            }
            return triangles;
        }

        private static float vertexY(int[] vertex) {
            return Float.intBitsToFloat(vertex[1]);
        }

        private static int[] interpolate(int[] start, int[] end, float factor) {
            int[] result = new int[VERTEX_STRIDE];
            for (int index : new int[]{0, 1, 2, 4, 5}) {
                float startValue = Float.intBitsToFloat(start[index]);
                float endValue = Float.intBitsToFloat(end[index]);
                result[index] = Float.floatToRawIntBits(Mth.lerp(factor, startValue, endValue));
            }
            result[3] = lerpPackedBytes(start[3], end[3], factor);
            result[6] = lerpPackedShorts(start[6], end[6], factor);
            result[7] = start[7];
            return result;
        }

        private static int lerpPackedBytes(int start, int end, float factor) {
            int result = 0;
            for (int shift = 0; shift < 32; shift += 8) {
                int startChannel = start >>> shift & 0xFF;
                int endChannel = end >>> shift & 0xFF;
                int channel = Math.round(Mth.lerp(factor, startChannel, endChannel));
                result |= channel << shift;
            }
            return result;
        }

        private static int lerpPackedShorts(int start, int end, float factor) {
            int low = Math.round(Mth.lerp(factor, start & 0xFFFF, end & 0xFFFF));
            int high = Math.round(Mth.lerp(factor, start >>> 16 & 0xFFFF, end >>> 16 & 0xFFFF));
            return low & 0xFFFF | high << 16;
        }

        private static BakedQuad copyQuad(BakedQuad source,
                                          int[] first, int[] second, int[] third, int[] fourth) {
            int[] vertices = new int[VERTEX_STRIDE * 4];
            int[][] parts = {first, second, third, fourth};
            for (int index = 0; index < parts.length; index++) {
                System.arraycopy(parts[index], 0, vertices, index * VERTEX_STRIDE, VERTEX_STRIDE);
            }
            return new BakedQuad(vertices, source.getTintIndex(), source.getDirection(),
                    source.getSprite(), source.isShade(), source.hasAmbientOcclusion());
        }
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
