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
    private int blinkSequence;
    private int blinkStartTick = Integer.MIN_VALUE;
    private int nextBlinkTick = 78;
    private int gazeSequence;
    private int gazeTransitionStartTick;
    private int nextGazeTick = 52;
    private float gazeFromX;
    private float gazeFromY;
    private float gazeToX;
    private float gazeToY;
    private NovaPortraitActivity activity = NovaPortraitActivity.IDLE;
    private int activityStartTick = Integer.MIN_VALUE;
    private int confirmationStartTick = Integer.MIN_VALUE;
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

    void setActivity(NovaPortraitActivity activity) {
        NovaPortraitActivity next = activity == null ? NovaPortraitActivity.IDLE : activity;
        if (this.activity == next)
            return;
        this.activity = next;
        activityStartTick = ticks;
    }

    void triggerConfirmation() {
        confirmationStartTick = ticks;
    }

    /** Advances the continuous brightness wave when one code point becomes visible. */
    public void onTextAdvanced() {
        textPulseStep = NovaTextPulse.nextStep(textPulseStep);
        textPulseFrom = textPulseTo;
        textPulseTo = NovaTextPulse.targetForStep(textPulseStep);
        textPulseUpdateTick = ticks;
    }

    public void setCoreState(CoreState coreState) {
        if (this.coreState == CoreState.REBOOTING && coreState == CoreState.ONLINE)
            triggerConfirmation();
        this.coreState = coreState;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        ticks++;
        updateBlinkSchedule();
        updateGazeSchedule();
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
        float warningPulse = activity == NovaPortraitActivity.WARNING
                ? NovaEyeMotion.activityPulse(time, activityStartTick) : 0F;
        int activeConfirmationStart = activity == NovaPortraitActivity.CONFIRMATION
                ? Math.max(activityStartTick, confirmationStartTick) : confirmationStartTick;
        float confirmationPulse = activity == NovaPortraitActivity.WARNING
                || activity == NovaPortraitActivity.SCANNING
                ? 0F : NovaEyeMotion.activityPulse(time, activeConfirmationStart);
        boolean scanningActive = activity == NovaPortraitActivity.SCANNING
                && NovaEyeMotion.scanningPanelActive(time, activityStartTick);
        int gazeX = scanningActive
                ? Math.round(NovaEyeMotion.scanningGazeX(time, activityStartTick))
                : Math.round(NovaEyeMotion.gazeOffset(
                gazeFromX, gazeToX, time, gazeTransitionStartTick));
        int gazeY = Math.round(NovaEyeMotion.gazeOffset(
                gazeFromY, gazeToY, time, gazeTransitionStartTick));
        if (confirmationPulse >= 0.45F)
            gazeY--;

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
            if (scanningActive)
                eyesAlpha += 0.03F + 0.03F * Mth.sin(time * TWO_PI / 28F);
            eyesAlpha = Mth.clamp(eyesAlpha
                    + warningPulse * 0.14F + confirmationPulse * 0.12F, 0F, 1F);
            float activityScale = 1F + warningPulse * 0.05F
                    + confirmationPulse * 0.025F;
            drawEyes(context, x + jitterX + gazeX, y + hoverY + gazeY, width, height,
                    breathe, activityScale,
                    NovaEyeMotion.blinkScale(time, blinkStartTick), eyesAlpha);
            if (activity == NovaPortraitActivity.SCANNING)
                drawScanningDataPanel(context, x + jitterX, y + hoverY,
                        width, height, time);
        }
    }

    private void updateBlinkSchedule() {
        if (ticks < nextBlinkTick)
            return;
        blinkStartTick = ticks;
        blinkSequence++;
        nextBlinkTick = ticks + NovaEyeMotion.BLINK_DURATION_TICKS
                + NovaEyeMotion.blinkDelayTicks(blinkSequence);
    }

    private void updateGazeSchedule() {
        if (ticks < nextGazeTick)
            return;
        gazeFromX = gazeToX;
        gazeFromY = gazeToY;
        gazeToX = NovaEyeMotion.gazeX(gazeSequence);
        gazeToY = NovaEyeMotion.gazeY(gazeSequence);
        gazeSequence++;
        gazeTransitionStartTick = ticks;
        nextGazeTick = ticks + NovaEyeMotion.GAZE_TRANSITION_TICKS
                + NovaEyeMotion.gazeDelayTicks(gazeSequence);
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
                          float overallScale, float activityScale,
                          float verticalScale, float alpha) {
        float scaledWidth = width * overallScale * activityScale;
        float scaledHeight = height * overallScale * activityScale;
        float drawX = x + (width - scaledWidth) * 0.5F;
        float baseDrawY = y + (height - scaledHeight) * 0.5F;
        float eyeAnchorY = baseDrawY + scaledHeight * 0.46F;
        float eyeHeight = scaledHeight * verticalScale;
        float eyeDrawY = eyeAnchorY - eyeHeight * 0.46F;

        eyesTexture.setColor(whiteWithAlpha(alpha));
        eyesTexture.draw(context, drawX, eyeDrawY, scaledWidth, eyeHeight);
    }

    private void drawScanningDataPanel(GUIContext context, float x, float y,
                                       int width, int height, float time) {
        float widthReveal = NovaEyeMotion.scanningPanelWidthReveal(time, activityStartTick);
        float heightReveal = NovaEyeMotion.scanningPanelHeightReveal(time, activityStartTick);
        float dataReveal = NovaEyeMotion.scanningPanelDataReveal(time, activityStartTick);
        if (widthReveal <= 0F)
            return;

        int fullLeft = Mth.floor(x + width * 0.16F);
        int fullRight = Mth.ceil(x + width * 0.84F);
        int centerX = (fullLeft + fullRight) / 2;
        int panelWidth = Math.max(1, Math.round((fullRight - fullLeft) * widthReveal));
        int left = centerX - panelWidth / 2;
        int right = left + panelWidth;
        int fullTop = Mth.floor(y + height * 0.33F);
        int fullBottom = Mth.ceil(y + height * 0.62F);
        int centerY = Mth.floor(y + height * 0.475F);
        int panelHeight = Math.max(1, Math.round((fullBottom - fullTop) * heightReveal));
        int top = centerY - panelHeight / 2;
        int bottom = top + panelHeight;
        if (right <= left || bottom <= top)
            return;

        GuiGraphics graphics = context.graphics;
        if (heightReveal <= 0F) {
            graphics.fill(left, centerY, right, centerY + 1,
                    colorWithAlpha(0xD8FFFF, 0.92F * widthReveal));
            return;
        }
        graphics.fill(left, top, right, bottom,
                colorWithAlpha(0x071A22, 0.52F * heightReveal));

        int borderColor = colorWithAlpha(0x78F5F1, 0.72F * heightReveal);
        int cornerLength = Math.max(2, Math.min(5, panelWidth / 4));
        drawPanelCorners(graphics, left, top, right, bottom, cornerLength, borderColor);
        if (panelWidth < 8 || bottom - top < 6 || dataReveal <= 0F)
            return;

        int innerWidth = Math.max(1, right - left - 4);
        int innerHeight = Math.max(1, bottom - top - 4);
        int columns = width >= 64 ? 5 : 4;
        for (int column = 0; column < columns; column++) {
            int streamX = left + 2 + Math.round(
                    innerWidth * (column + 0.5F) / columns);
            float speed = 0.34F + column * 0.055F;
            for (int segment = 0; segment < 3; segment++) {
                int phase = Mth.floor(time * speed + column * 5F + segment * 7F);
                int streamY = top + 2 + Math.floorMod(phase, innerHeight);
                int segmentWidth = 1 + Math.floorMod(column + segment, 3);
                int segmentColor = colorWithAlpha(
                        segment == 0 ? 0xB8FFFF : 0x49D9D7,
                        (segment == 0 ? 0.82F : 0.48F) * dataReveal);
                graphics.fill(streamX, streamY,
                        Math.min(right - 1, streamX + segmentWidth),
                        Math.min(bottom - 1, streamY + 1), segmentColor);
            }
        }

        int cursorRange = Math.max(1, innerWidth - 2);
        int cursorX = left + 2 + Math.floorMod(Mth.floor(time * 0.72F), cursorRange);
        graphics.fill(cursorX, top + 1, Math.min(right - 1, cursorX + 2), top + 2,
                colorWithAlpha(0xE8FFFF, 0.88F * dataReveal));
    }

    private static void drawPanelCorners(GuiGraphics graphics,
                                         int left, int top, int right, int bottom,
                                         int length, int color) {
        graphics.fill(left, top, Math.min(right, left + length), top + 1, color);
        graphics.fill(left, top, left + 1, Math.min(bottom, top + length), color);
        graphics.fill(Math.max(left, right - length), top, right, top + 1, color);
        graphics.fill(right - 1, top, right, Math.min(bottom, top + length), color);
        graphics.fill(left, bottom - 1, Math.min(right, left + length), bottom, color);
        graphics.fill(left, Math.max(top, bottom - length), left + 1, bottom, color);
        graphics.fill(Math.max(left, right - length), bottom - 1, right, bottom, color);
        graphics.fill(right - 1, Math.max(top, bottom - length), right, bottom, color);
    }

    private static int whiteWithAlpha(float alpha) {
        int alphaByte = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        return alphaByte << 24 | 0x00FFFFFF;
    }

    private static int colorWithAlpha(int rgb, float alpha) {
        int alphaByte = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        return alphaByte << 24 | rgb & 0x00FFFFFF;
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
