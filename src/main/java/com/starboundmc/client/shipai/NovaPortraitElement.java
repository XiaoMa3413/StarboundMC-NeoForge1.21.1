package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.starboundmc.StarboundMC;
import com.starboundmc.story.CoreState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Portrait surface for N.O.V.A.
 *
 * <p>The approved 96x112 pixel-art projection keeps the sphere and orbital ring
 * in one base image so their occlusion cannot drift apart. Only the eyes are a
 * separate transparent layer for blinking and speaking feedback. The original
 * composite remains the static fallback.</p>
 */
public final class NovaPortraitElement extends UIElement {
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    public static final ResourceLocation PORTRAIT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            StarboundMC.MODID, "textures/gui/ship_ai/nova_bust.png");
    public static final ResourceLocation BODY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            StarboundMC.MODID, "textures/gui/ship_ai/nova_body.png");
    public static final ResourceLocation EYES_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            StarboundMC.MODID, "textures/gui/ship_ai/nova_eyes.png");

    private final SpriteTexture portraitTexture = SpriteTexture.of(PORTRAIT_TEXTURE);
    private final SpriteTexture bodyTexture = SpriteTexture.of(BODY_TEXTURE);
    private final SpriteTexture eyesTexture = SpriteTexture.of(EYES_TEXTURE);
    private final boolean eyeLayerPresent;
    private final boolean portraitAssetPresent;
    private int ticks;
    private boolean speaking;
    private CoreState coreState;
    private int textPulseStep = -1;
    private int textPulseUpdateTick = Integer.MIN_VALUE;
    private float textPulseFrom;
    private float textPulseTo;

    public NovaPortraitElement() {
        addClass("nova-portrait");
        setAllowHitTest(false);
        setOverflowVisible(false);

        var resources = Minecraft.getInstance().getResourceManager();
        eyeLayerPresent = resources.getResource(BODY_TEXTURE).isPresent()
                && resources.getResource(EYES_TEXTURE).isPresent();
        portraitAssetPresent = eyeLayerPresent
                || resources.getResource(PORTRAIT_TEXTURE).isPresent();
        if (eyeLayerPresent) {
            addClass("nova-portrait-eyes-layered");
        } else if (portraitAssetPresent) {
            addClass("nova-portrait-asset");
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

    /** Advances the continuous brightness wave when one code point becomes visible. */
    public void onTextAdvanced() {
        textPulseStep = NovaTextPulse.nextStep(textPulseStep);
        textPulseFrom = textPulseTo;
        textPulseTo = NovaTextPulse.targetForStep(textPulseStep);
        textPulseUpdateTick = ticks;
    }

    public void setCoreState(CoreState coreState) {
        this.coreState = coreState;
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

        if (portraitAssetPresent) {
            drawAnimatedPortrait(context, x, y, width, height);
        } else {
            drawPrototypePlaceholder(context.graphics, x, y, width, height);
        }
        drawHologramOverlay(context, x, y, width, height);
    }

    private void drawAnimatedPortrait(GUIContext context, int x, int y, int width, int height) {
        float time = ticks + context.partialTick;
        float onlineActivity = coreState == CoreState.ONLINE ? 1F
                : coreState == CoreState.REBOOTING ? 0.45F : 0F;
        float hoverY = Mth.sin(time * TWO_PI / 72F) * 0.85F * onlineActivity;
        float breathe = 1F + Mth.sin(time * TWO_PI / 62F) * 0.006F * onlineActivity;
        float jitterX = projectionJitter(time);
        float textPulse = textPulse(context.partialTick);

        float portraitAlpha;
        if (coreState == null) {
            portraitAlpha = 0.5F + Mth.sin(time * TWO_PI / 44F) * 0.08F;
        } else {
            portraitAlpha = switch (coreState) {
                case OFFLINE -> 0.32F;
                case REBOOTING -> speaking
                        ? 0.64F
                        : 0.62F + Mth.sin(time * 0.72F) * 0.12F;
                case ONLINE -> speaking
                        ? 0.97F
                        : 0.97F + Mth.sin(time * TWO_PI / 78F) * 0.025F;
            };
        }
        SpriteTexture baseTexture = eyeLayerPresent ? bodyTexture : portraitTexture;
        drawScaled(context, baseTexture, x + jitterX, y + hoverY,
                width, height, breathe, portraitAlpha);

        if (eyeLayerPresent) {
            float eyesAlpha;
            if (coreState == null) {
                eyesAlpha = 0.28F;
            } else {
                eyesAlpha = switch (coreState) {
                    case OFFLINE -> 0.05F;
                    case REBOOTING -> speaking
                            ? 0.42F + textPulse * 0.58F
                            : ticks % 12 < 7 ? 0.68F : 0.18F;
                    case ONLINE -> speaking
                            ? 0.50F + textPulse * 0.50F
                            : 0.92F + Mth.sin(time * TWO_PI / 72F) * 0.05F;
                };
            }
            drawEyes(context, x + jitterX, y + hoverY, width, height,
                    breathe, blinkScale(time), eyesAlpha);
        }
    }

    private float textPulse(float partialTick) {
        if (textPulseStep < 0)
            return 0F;
        if (textPulseUpdateTick != ticks)
            return textPulseTo;
        return Mth.lerp(Mth.clamp(partialTick, 0F, 1F), textPulseFrom, textPulseTo);
    }

    private float projectionJitter(float time) {
        if (coreState == CoreState.REBOOTING)
            return Mth.sin(time * 1.75F) * 0.38F;
        if (coreState == CoreState.ONLINE && ticks % 241 < 2)
            return ticks % 2 == 0 ? 0.35F : -0.35F;
        return 0F;
    }

    private static float blinkScale(float time) {
        float phase = time % 127F;
        if (phase >= 4F)
            return 1F;
        float distanceFromClosed = Math.abs(phase - 2F) / 2F;
        return 0.12F + Mth.clamp(distanceFromClosed, 0F, 1F) * 0.88F;
    }

    private static void drawScaled(GUIContext context, SpriteTexture texture,
                                   float x, float y, float width, float height,
                                   float scale, float alpha) {
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        float drawX = x + (width - scaledWidth) * 0.5F;
        float drawY = y + (height - scaledHeight) * 0.5F;
        texture.setColor(whiteWithAlpha(alpha));
        texture.draw(context, drawX, drawY, scaledWidth, scaledHeight);
    }

    private void drawEyes(GUIContext context, float x, float y, float width, float height,
                          float overallScale, float verticalScale, float alpha) {
        float scaledWidth = width * overallScale;
        float scaledHeight = height * overallScale;
        float drawX = x + (width - scaledWidth) * 0.5F;
        float baseDrawY = y + (height - scaledHeight) * 0.5F;
        float eyeAnchorY = baseDrawY + scaledHeight * 0.46F;
        float eyeHeight = scaledHeight * verticalScale;
        float eyeDrawY = eyeAnchorY - eyeHeight * 0.46F;

        eyesTexture.setColor(whiteWithAlpha(alpha));
        eyesTexture.draw(context, drawX, eyeDrawY, scaledWidth, eyeHeight);
    }

    private static int whiteWithAlpha(float alpha) {
        int alphaByte = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        return alphaByte << 24 | 0x00FFFFFF;
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
        int lineColor = coreState == CoreState.OFFLINE ? 0x0A29A5A6 : 0x1229A5A6;
        for (int lineY = y + 2; lineY < y + height; lineY += 4) {
            graphics.fill(x, lineY, x + width, lineY + 1, lineColor);
        }

        float speed = coreState == CoreState.REBOOTING ? 1.35F
                : coreState == CoreState.OFFLINE ? 0.2F : 0.65F;
        float phase = (ticks + context.partialTick) * speed;
        int scanY = y + Mth.floor(phase % Math.max(1, height));
        int scanColor = coreState == CoreState.OFFLINE ? 0x2870B8B5 : 0x748CF5F2;
        graphics.fill(x, scanY, x + width, Math.min(y + height, scanY + 1), scanColor);
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
