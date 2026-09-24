package com.starboundmc.client;

import com.mojang.blaze3d.systems.RenderSystem;

/** Restores the common opaque state expected between the space render passes. */
final class SpaceRenderPassState
{
    private SpaceRenderPassState()
    {
    }

    static void restoreDefaults()
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
