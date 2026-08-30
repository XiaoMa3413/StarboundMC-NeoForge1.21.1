package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.starboundmc.StarboundMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Portrait surface for N.O.V.A.
 *
 * <p>The checked-in T0 prototype intentionally falls back to a diagnostic
 * geometric silhouette until the approved original pixel-art asset is supplied.
 * Dropping the final 96x112 image at the resource location below activates it
 * without changing the surrounding UI tree.</p>
 */
public final class NovaPortraitElement extends UIElement {
    public static final ResourceLocation PORTRAIT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            StarboundMC.MODID, "textures/gui/ship_ai/nova_bust.png");

    private final boolean portraitAssetPresent;
    private int ticks;
    private boolean speaking;

    public NovaPortraitElement() {
        addClass("nova-portrait");
        setAllowHitTest(false);
        setOverflowVisible(false);

        portraitAssetPresent = Minecraft.getInstance().getResourceManager()
                .getResource(PORTRAIT_TEXTURE)
                .isPresent();
        if (portraitAssetPresent) {
            addClass("nova-portrait-asset");
            style(style -> style.backgroundTexture(SpriteTexture.of(PORTRAIT_TEXTURE)));
        } else {
            // This class is intentionally visible in the debugger/LSS tree so the
            // geometric T0 stand-in cannot be mistaken for approved character art.
            addClass("nova-portrait-prototype");
        }
    }

    public boolean hasPortraitAsset() {
        return portraitAssetPresent;
    }

    public void setSpeaking(boolean speaking) {
        this.speaking = speaking;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        ticks++;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        int x = Mth.floor(getContentX());
        int y = Mth.floor(getContentY());
        int width = Math.max(1, Mth.floor(getContentWidth()));
        int height = Math.max(1, Mth.floor(getContentHeight()));

        if (!portraitAssetPresent) {
            drawPrototypePlaceholder(context.graphics, x, y, width, height);
        }
        drawHologramOverlay(context, x, y, width, height);
    }

    /** Draws a deliberately geometric T0 placeholder, never final N.O.V.A. artwork. */
    private void drawPrototypePlaceholder(GuiGraphics graphics, int x, int y, int width, int height) {
        fillRelative(graphics, x, y, width, height, 0.31F, 0.12F, 0.69F, 0.19F, 0xBB315762);
        fillRelative(graphics, x, y, width, height, 0.25F, 0.19F, 0.75F, 0.48F, 0xCC173541);
        fillRelative(graphics, x, y, width, height, 0.30F, 0.23F, 0.70F, 0.44F, 0xE607151B);
        fillRelative(graphics, x, y, width, height, 0.36F, 0.30F, 0.64F, 0.35F,
                speaking && (ticks / 4) % 2 == 0 ? 0xFFE4FFFF : 0xFF70E6E3);

        fillRelative(graphics, x, y, width, height, 0.38F, 0.48F, 0.47F, 0.61F, 0xC82A5360);
        fillRelative(graphics, x, y, width, height, 0.53F, 0.48F, 0.62F, 0.61F, 0xC82A5360);
        fillRelative(graphics, x, y, width, height, 0.18F, 0.59F, 0.82F, 0.70F, 0xC41D424E);
        fillRelative(graphics, x, y, width, height, 0.10F, 0.69F, 0.43F, 0.78F, 0xA52C5965);
        fillRelative(graphics, x, y, width, height, 0.57F, 0.69F, 0.90F, 0.78F, 0xA52C5965);
        fillRelative(graphics, x, y, width, height, 0.28F, 0.78F, 0.72F, 0.86F, 0x85336A74);

        fillRelative(graphics, x, y, width, height, 0.22F, 0.89F, 0.31F, 0.94F, 0x8B58B8B5);
        fillRelative(graphics, x, y, width, height, 0.43F, 0.87F, 0.51F, 0.92F, 0xA665D6D1);
        fillRelative(graphics, x, y, width, height, 0.65F, 0.90F, 0.73F, 0.96F, 0x7950A7A6);
    }

    private void drawHologramOverlay(GUIContext context, int x, int y, int width, int height) {
        GuiGraphics graphics = context.graphics;
        for (int lineY = y + 2; lineY < y + height; lineY += 4) {
            graphics.fill(x, lineY, x + width, lineY + 1, 0x1929A5A6);
        }

        float phase = (ticks + context.partialTick) * 0.65F;
        int scanY = y + Mth.floor(phase % Math.max(1, height));
        graphics.fill(x, scanY, x + width, Math.min(y + height, scanY + 1), 0x8A8CF5F2);
    }

    private static void fillRelative(GuiGraphics graphics, int x, int y, int width, int height,
                                     float left, float top, float right, float bottom, int color) {
        int x0 = x + Mth.floor(width * left);
        int y0 = y + Mth.floor(height * top);
        int x1 = x + Mth.ceil(width * right);
        int y1 = y + Mth.ceil(height * bottom);
        graphics.fill(x0, y0, x1, y1, color);
    }
}
