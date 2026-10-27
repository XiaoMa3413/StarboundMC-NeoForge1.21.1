package com.starboundmc.client.enemy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.starboundmc.enemy.EnemyBolt;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Full-bright billboard; actual projectile movement and hits remain server-owned. */
public final class EnemyBoltRenderer extends EntityRenderer<EnemyBolt> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("starboundmc","textures/entity/enemies/plasma_bolt.png");
    public EnemyBoltRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(EnemyBolt entity){return TEXTURE;}
    @Override public void render(EnemyBolt entity,float yaw,float partialTick,PoseStack poses,MultiBufferSource buffers,int light) {
        poses.pushPose();poses.mulPose(entityRenderDispatcher.cameraOrientation());poses.mulPose(Axis.YP.rotationDegrees(180));poses.scale(.28f,.28f,.28f);
        var out=buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));var pose=poses.last();
        out.addVertex(pose,-.5f,-.5f,0).setColor(-1).setUv(0,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,0,1);
        out.addVertex(pose,.5f,-.5f,0).setColor(-1).setUv(1,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,0,1);
        out.addVertex(pose,.5f,.5f,0).setColor(-1).setUv(1,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,0,1);
        out.addVertex(pose,-.5f,.5f,0).setColor(-1).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,0,1);
        poses.popPose();super.render(entity,yaw,partialTick,poses,buffers,light);
    }
}
