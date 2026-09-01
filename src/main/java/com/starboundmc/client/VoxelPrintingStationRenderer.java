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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Dual-probe printing effect: a full-size cyan hologram volume remains visible
 * while the selected result forms from bottom to top and the probes alternate.
 */
public final class VoxelPrintingStationRenderer
        implements BlockEntityRenderer<VoxelPrintingStationBlockEntity> {
    private final ItemRenderer itemRenderer;

    public VoxelPrintingStationRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(VoxelPrintingStationBlockEntity station, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        var snapshot = ClientVoxelMachineState.snapshotAt(station.getBlockPos());
        if (snapshot == null || snapshot.resultCount() <= 0) {
            return;
        }
        var item = BuiltInRegistries.ITEM.get(snapshot.resultItemId());
        if (item == Items.AIR) {
            return;
        }
        ItemStack result = new ItemStack(item, snapshot.resultCount());
        float formation = snapshot.progress() > 0
                ? (snapshot.totalTicks() - snapshot.progress()) / (float) Math.max(1, snapshot.totalTicks())
                : 1.0F;
        formation = Math.max(0.02F, Math.min(1.0F, formation));

        Direction facing = station.getBlockState().getValue(VoxelPrintingStationBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facing.toYRot() + 180.0F));
        pose.translate(-0.5, -0.5, -0.5);

        renderHologramAndScan(station, pose, buffers, formation);
        renderFormingItem(station, result, pose, buffers, formation, packedOverlay);
        pose.popPose();
    }

    private void renderHologramAndScan(VoxelPrintingStationBlockEntity station, PoseStack pose,
                                        MultiBufferSource buffers, float formation) {
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        float pulse = 0.62F + 0.18F * (float) Math.sin(
                (station.getLevel().getGameTime() + formation * 10.0F) * 0.35F);
        AABB hologram = new AABB(0.32, 0.34, 0.27, 0.68, 0.76, 0.43);
        LevelRenderer.renderLineBox(pose, lines, hologram,
                0.30F, 0.90F, 1.0F, pulse);

        float scanY = 0.34F + formation * 0.42F;
        LevelRenderer.renderLineBox(pose, lines,
                0.29, scanY - 0.006, 0.25,
                0.71, scanY + 0.006, 0.45,
                0.72F, 0.98F, 1.0F, 0.95F);

        if (formation < 1.0F) {
            boolean leftProbe = ((station.getLevel().getGameTime() / 4L) & 1L) == 0L;
            float startX = leftProbe ? 0.07F : 0.93F;
            drawBeam(lines, pose, startX, 0.47F, 0.64F, 0.50F, scanY, 0.35F);
        }
    }

    private void renderFormingItem(VoxelPrintingStationBlockEntity station, ItemStack result,
                                   PoseStack pose, MultiBufferSource buffers,
                                   float formation, int packedOverlay) {
        pose.pushPose();
        pose.translate(0.5, 0.34 + 0.16 * formation, 0.35);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(0.32F, 0.32F * formation, 0.32F);
        itemRenderer.renderStatic(result, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, packedOverlay, pose, buffers,
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

    @Override
    public int getViewDistance() {
        return 48;
    }
}
