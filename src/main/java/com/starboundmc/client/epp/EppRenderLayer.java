// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.starboundmc.item.ModItems;
import com.starboundmc.story.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class EppRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ItemStack mk1 = new ItemStack(ModItems.EPP_MK1.get());
    private final ItemStack mk2 = new ItemStack(ModItems.EPP_MK2.get());
    private final ItemStack mk3 = new ItemStack(ModItems.EPP_MK3.get());
    public EppRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }
    @Override public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                                 float swing, float amount, float dt, float age, float yaw, float pitch) {
        int generation = player.getData(ModAttachments.EPP_VISUAL);
        if (player.isInvisible() || generation == 0) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pose.translate(0, .36, .30);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.scale(.8f, .8f, .8f);
        Minecraft.getInstance().getItemRenderer().renderStatic(generation >= 3 ? mk3 : generation >= 2 ? mk2 : mk1, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, buffer, player.level(), player.getId());
        pose.popPose();
    }
}
