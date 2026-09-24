package com.starboundmc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.starboundmc.block.entity.ShipCrateBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Baked sliding doors and a flat, full-bright holographic screen. */
public final class ShipLockerRenderer implements BlockEntityRenderer<ShipCrateBlockEntity> {
    public static final ModelResourceLocation LEFT = model("ship_locker_left_door");
    public static final ModelResourceLocation RIGHT = model("ship_locker_right_door");
    private static final ResourceLocation HOLOGRAM = ResourceLocation.fromNamespaceAndPath(
            "starboundmc", "textures/effect/locker_hologram.png");
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(
            "starboundmc", "textures/effect/locker_hologram_frame.png");

    public ShipLockerRenderer(BlockEntityRendererProvider.Context context) {}

    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("starboundmc", "block/" + name));
    }

    @Override
    public void render(ShipCrateBlockEntity crate, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (crate.getLevel() == null) return;
        var door = LockerDoorPose.at(crate.getOpening(partialTick));
        var facing = crate.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.translate(-0.5, -0.5, -0.5);
        VertexConsumer solid = buffers.getBuffer(RenderType.cutout());
        drawDoor(LEFT, door.slide(), door.retreat(), pose, solid, light, overlay);
        drawDoor(RIGHT, -door.slide(), door.retreat(), pose, solid, light, overlay);
        if (door.hologramAlpha() > 0.001F) {
            float time = crate.getLevel().getGameTime() + partialTick;
            // A turn represented on the display plane: never extrude or rotate a physical padlock.
            float turn = 0.18F + 0.82F * Math.abs((float) Math.cos(time * Math.PI / 80));
            float halfWidth = 1.65F / 16 * turn;
            float halfHeight = 1.65F / 16;
            float centerX = 9.7F / 16;
            float centerY = 8.15F / 16 + (float) Math.sin(time * 0.045F) * 0.003F;
            float alpha = door.hologramAlpha() * (0.92F + 0.08F * (float) Math.sin(time * 0.08F));
            drawScreen(buffers.getBuffer(RenderType.entityTranslucentEmissive(HOLOGRAM)), pose,
                    centerX, centerY, halfWidth, halfHeight, alpha, 9.25F / 16);
            drawScreen(buffers.getBuffer(RenderType.entityTranslucentEmissive(FRAME)), pose,
                    centerX, 8.15F / 16, halfHeight, halfHeight, door.hologramAlpha(), 9.24F / 16);
        }
        pose.popPose();
    }

    private static void drawDoor(ModelResourceLocation id, float x, float z, PoseStack pose,
                                 VertexConsumer consumer, int light, int overlay) {
        pose.pushPose();
        pose.translate(x, 0, z);
        var mc = Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(id),
                ItemStack.EMPTY, light, overlay, pose, consumer);
        pose.popPose();
    }

    private static void drawScreen(VertexConsumer screen, PoseStack pose, float x, float y,
                                   float width, float height, float alpha, float z) {
        vertex(screen, pose, x + width, y + height, z, 0, 0, alpha);
        vertex(screen, pose, x - width, y + height, z, 1, 0, alpha);
        vertex(screen, pose, x - width, y - height, z, 1, 1, alpha);
        vertex(screen, pose, x + width, y - height, z, 0, 1, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack pose, float x, float y, float z,
                               float u, float v, float alpha) {
        consumer.addVertex(pose.last(), x, y, z)
                .setColor(1, 1, 1, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0, 0, -1);
    }
}
