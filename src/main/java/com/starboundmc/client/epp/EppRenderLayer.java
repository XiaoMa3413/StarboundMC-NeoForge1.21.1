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
    private final ItemStack model = new ItemStack(ModItems.EPP_MK1.get());
    public EppRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }
    @Override public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                                 float swing, float amount, float dt, float age, float yaw, float pitch) {
        if (player.isInvisible() || !player.getData(ModAttachments.EPP_VISUAL)) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pose.translate(0, .36, .30);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.scale(.8f, .8f, .8f);
        Minecraft.getInstance().getItemRenderer().renderStatic(model, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, buffer, player.level(), player.getId());
        pose.popPose();
    }
}
