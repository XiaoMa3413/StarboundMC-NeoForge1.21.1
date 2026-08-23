package com.starboundmc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.starboundmc.entity.SeatEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** Renders nothing; the seat is invisible. */
public class SeatRenderer extends EntityRenderer<SeatEntity>
{
    public SeatRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public void render(SeatEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light)
    {
    }

    @Override
    public ResourceLocation getTextureLocation(SeatEntity entity)
    {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
