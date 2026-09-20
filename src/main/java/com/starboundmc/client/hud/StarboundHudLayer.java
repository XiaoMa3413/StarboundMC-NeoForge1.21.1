// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.starboundmc.client.hud.ar.ArWorldRenderer;
import com.starboundmc.client.hud.visor.HudVisorCalibrationGrid;
import com.starboundmc.client.hud.visor.HudVisorProjection;
import com.starboundmc.client.hud.visor.VisorCompassRenderer;
import com.starboundmc.client.epp.EppClientState;
import com.starboundmc.epp.OxygenRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Owns the player-device HUD composition. World-locked AR is deliberately
 * rendered outside the visor transform, while N.O.V.A. broadcasts use their
 * own independent flat layer.
 */
public final class StarboundHudLayer implements ModularHudLayer {
    private static final int SURVIVAL_WIDTH = 128;
    private static final int COMPONENT_HEIGHT = 48;
    public static final StarboundHudLayer INSTANCE = new StarboundHudLayer();
    private ModularUI ui;
    private final HudVisorProjection survivalProjection = new HudVisorProjection(SURVIVAL_WIDTH, COMPONENT_HEIGHT);
    private final HudVisorProjection compassProjection = new HudVisorProjection(272, COMPONENT_HEIGHT);
    private final HudVisorProjection controlsProjection = new HudVisorProjection(360, COMPONENT_HEIGHT);
    private final HudVisorProjection bootStatusProjection = new HudVisorProjection(
            HudBootStatusRenderer.WIDTH, HudBootStatusRenderer.HEIGHT);
    private final HudVisorProjection bootPromptProjection = new HudVisorProjection(
            HudBootStatusRenderer.PROMPT_WIDTH, HudBootStatusRenderer.PROMPT_HEIGHT);
    @Override public ModularUI getModularUI() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive()) { releaseUi(); return null; }
        if (mc.options.hideGui) return null;
        if (ui == null) ui = ModularUI.of(UI.of(
                new HudRoot(survivalProjection, compassProjection, controlsProjection,
                        bootStatusProjection, bootPromptProjection),
                ResourceLocation.fromNamespaceAndPath("starboundmc", "lss/epp.lss")));
        return ui;
    }
    @Override public void render(GuiGraphics graphics, DeltaTracker dt) {
        var current = getModularUI();
        if (current != null && validModularUI(current)) current.getWidget().render(graphics, Integer.MAX_VALUE, Integer.MAX_VALUE, dt.getGameTimeDeltaPartialTick(false));
    }
    public void resetConnectionState() {
        releaseUi();
        HudBootController.INSTANCE.reset();
    }

    private void releaseUi() {
        if (ui != null) ui.onRemoved();
        ui = null;
        survivalProjection.close();
        compassProjection.close();
        controlsProjection.close();
        bootStatusProjection.close();
        bootPromptProjection.close();
        ArWorldRenderer.INSTANCE.reset();
    }
    private static boolean showEvaNavigation() {
        var player = Minecraft.getInstance().player;
        return player != null && player.level().dimension().equals(com.starboundmc.world.ShipDimensions.SHIP_LEVEL)
                && com.starboundmc.epp.EvaMovement.mode(player) != com.starboundmc.epp.EvaState.NORMAL;
    }
    private static final class HudRoot extends UIElement {
        private long lastFrame;
        private float lastYaw, lastPitch, driftX, driftY;
        private final OxygenHudFade oxygenFade = new OxygenHudFade();
        private final OxygenHudFade coldFade = new OxygenHudFade();
        private final OxygenHudFade heatFade = new OxygenHudFade();
        private final HudVisibilityFade navigationFade = new HudVisibilityFade();
        private final HudVisibilityFade evaControlsFade = new HudVisibilityFade();
        private boolean lastThrust;
        private final HudVisorProjection survivalProjection;
        private final HudVisorProjection compassProjection;
        private final HudVisorProjection controlsProjection;
        private final HudVisorProjection bootStatusProjection;
        private final HudVisorProjection bootPromptProjection;

        HudRoot(HudVisorProjection survivalProjection, HudVisorProjection compassProjection,
                HudVisorProjection controlsProjection, HudVisorProjection bootStatusProjection,
                HudVisorProjection bootPromptProjection) {
            this.survivalProjection = survivalProjection;
            this.compassProjection = compassProjection;
            this.controlsProjection = controlsProjection;
            this.bootStatusProjection = bootStatusProjection;
            this.bootPromptProjection = bootPromptProjection;
            layout(l -> l.widthPercent(100).heightPercent(100)); setAllowHitTest(false);
        }
        @Override public void drawBackgroundAdditional(GUIContext context) {
            var mc = Minecraft.getInstance();
            float seconds = lastFrame == 0 ? 0 : Math.min(.1f, (System.nanoTime() - lastFrame) / 1_000_000_000f);
            updateDrift(mc);
            context.graphics.pose().pushPose();
            context.graphics.pose().translate(driftX, driftY, 0);
            HudVisorCalibrationGrid.render(context.graphics);
            context.graphics.pose().popPose();
            var boot = HudBootController.INSTANCE.presentation(context.partialTick);
            if (mc.screen == null)
                drawBootPresentation(context.graphics, boot);
            boolean eva = showEvaNavigation();
            if (eva) lastThrust = com.starboundmc.epp.EvaMovement.mode(mc.player)
                    == com.starboundmc.epp.EvaState.THRUST;
            float delta = mc.isPaused() ? 0 : seconds;
            float navigationOpacity = navigationFade.update(
                    delta, eva || HudBootController.INSTANCE.localNavigationActive());
            float controlsOpacity = evaControlsFade.update(delta, eva);
            if (navigationOpacity > .001f
                    && mc.player.level().dimension().equals(com.starboundmc.world.ShipDimensions.SHIP_LEVEL))
                drawNavigation(context.graphics, navigationOpacity, controlsOpacity,
                        boot.statusOpacity());
            var s = EppClientState.snapshot; if (s == null) return;
            var g = context.graphics;
            int x = g.guiWidth() - 140;
            float opacity = oxygenFade.update(
                    mc.isPaused() ? 0 : seconds, s.airless() || s.exposure() > 0 || s.refilling());
            boolean oxygenVisible = opacity > .001f;
            boolean coldVisible = s.coldTier() > 0 || s.coldExposure() > 0;
            boolean heatVisible = s.heatTier() > 0 || s.heatExposure() > 0;
            float coldOpacity = coldFade.update(mc.isPaused() ? 0 : seconds, coldVisible && s.coldProtection() <= 0);
            float heatOpacity = heatFade.update(mc.isPaused() ? 0 : seconds, heatVisible && s.heatProtection() <= 0);
            coldVisible = coldOpacity > .001f;
            heatVisible = heatOpacity > .001f;
            int rows = (oxygenVisible ? 1 : 0) + (coldVisible ? 1 : 0) + (heatVisible ? 1 : 0);
            int y = g.guiHeight() - 82 - Math.max(0, rows - 2) * 40;
            float visorOpacity = HudBootController.INSTANCE.visorOpacity();
            g.pose().pushPose();
            g.pose().translate(driftX, driftY, 0);
            if (oxygenVisible) survivalProjection.draw(g, x, y,
                    SURVIVAL_WIDTH, COMPONENT_HEIGHT, opacity * visorOpacity,
                    canvas -> drawFlat(canvas, context.partialTick));
            int row = oxygenVisible ? 40 : 0;
            if (coldVisible) {
                survivalProjection.draw(g, x, y + row,
                        SURVIVAL_WIDTH, COMPONENT_HEIGHT, coldOpacity * visorOpacity,
                        canvas -> drawThermal(canvas, false));
                row += 40;
            }
            if (heatVisible) survivalProjection.draw(g, x, y + row,
                    SURVIVAL_WIDTH, COMPONENT_HEIGHT, heatOpacity * visorOpacity,
                    canvas -> drawThermal(canvas, true));
            g.pose().popPose();
        }

        private void drawBootPresentation(GuiGraphics g, HudBootController.Presentation boot) {
            if (boot.promptOpacity() > .001F) {
                float fit = Math.min(1F,
                        Math.max(1F, g.guiWidth() - 24F) / HudBootStatusRenderer.PROMPT_WIDTH);
                float width = HudBootStatusRenderer.PROMPT_WIDTH * fit;
                float height = HudBootStatusRenderer.PROMPT_HEIGHT * fit;
                g.pose().pushPose();
                g.pose().translate(driftX, driftY, 0);
                bootPromptProjection.draw(g, (g.guiWidth() - width) / 2F,
                        (g.guiHeight() - height) / 2F, width, height, boot.promptOpacity(),
                        canvas -> HudBootStatusRenderer.drawPrompt(canvas, boot));
                g.pose().popPose();
            }
            if (boot.statusOpacity() > .001F) {
                float fit = Math.min(1F,
                        Math.max(1F, g.guiWidth() - 20F) / HudBootStatusRenderer.WIDTH);
                float width = HudBootStatusRenderer.WIDTH * fit;
                float height = HudBootStatusRenderer.HEIGHT * fit;
                g.pose().pushPose();
                g.pose().translate(driftX, driftY, 0);
                bootStatusProjection.draw(g, g.guiWidth() - width - 10F, 10F,
                        width, height, boot.statusOpacity(),
                        canvas -> HudBootStatusRenderer.draw(canvas, boot));
                g.pose().popPose();
            }
        }

        private void drawNavigation(GuiGraphics g, float opacity, float controlsOpacity,
                                    float bootStatusOpacity) {
            var mc = Minecraft.getInstance();
            int x = g.guiWidth() / 2;
            ArWorldRenderer.INSTANCE.render(g, opacity, controlsOpacity > .001F);
            float navFit = Math.min(1f, (g.guiWidth() - 16f) / 272f);
            float visorOpacity = HudBootController.INSTANCE.visorOpacity();
            float bootOffset = 56F * bootStatusOpacity;
            g.pose().pushPose();
            g.pose().translate(driftX, driftY, 0);
            compassProjection.draw(g, x - 136 * navFit, 18 + bootOffset,
                    272 * navFit, COMPONENT_HEIGHT * navFit, opacity * visorOpacity,
                    VisorCompassRenderer::draw);
            if (controlsOpacity <= .001F) {
                g.pose().popPose();
                return;
            }
            boolean thrust = lastThrust;
            var hint = thrust ? Component.translatable("hud.starboundmc.eva.controls",
                    mc.options.keyUp.getTranslatedKeyMessage(), mc.options.keyLeft.getTranslatedKeyMessage(),
                    mc.options.keyDown.getTranslatedKeyMessage(), mc.options.keyRight.getTranslatedKeyMessage(),
                    mc.options.keyJump.getTranslatedKeyMessage(), mc.options.keyShift.getTranslatedKeyMessage())
                    : Component.translatable("hud.starboundmc.eva.no_thrusters",
                            com.starboundmc.client.ModKeyBindings.returnToShip.getTranslatedKeyMessage());
            // Render text at its native aspect ratio into a genuinely wider target.
            float width = Math.min(g.guiWidth() - 16f, 360f);
            float fit = width / 360f;
            controlsProjection.draw(g, x - width / 2, 63 + bootOffset,
                    width, COMPONENT_HEIGHT * fit, controlsOpacity * visorOpacity, canvas -> {
                float textScale = Math.min(1f, 340f / Math.max(1, mc.font.width(hint)));
                canvas.pose().pushPose();
                canvas.pose().translate(180, 12, 0); canvas.pose().scale(textScale, textScale, 1);
                canvas.drawCenteredString(mc.font, hint, 0, 0, thrust ? 0xB895E8E2 : 0xD8FFD17C);
                canvas.pose().popPose();
            });
            g.pose().popPose();
        }

        private void drawThermal(GuiGraphics g, boolean heat) {
            var s = EppClientState.snapshot;
            var font = Minecraft.getInstance().font;
            int exposure = heat ? s.heatExposure() : s.coldExposure();
            String hazard = heat ? "heat" : "cold";
            int rgb = exposure >= 75 ? 0xFF9477 : exposure >= 25 ? 0xFFD17C : heat ? 0xF3BE96 : 0xA7DFFF;
            g.fill(6, 24, 122, 25, 0x28000000 | rgb);
            int filled = Math.round(116f * exposure / 100);
            if (filled > 0) g.fill(6, 24, 6 + filled, 25, 0xBA000000 | rgb);
            drawFlatText(g, font, Component.translatable("hud.starboundmc.epp." + hazard, exposure), 9, 216, rgb);
            boolean protectedFromHazard = (heat ? s.heatProtection() : s.coldProtection()) > 0;
            boolean hazardous = (heat ? s.heatTier() : s.coldTier()) > 0;
            drawFlatText(g, font, Component.translatable("hud.starboundmc.epp." +
                    (!hazardous ? "thermal_recovery" : protectedFromHazard ? "thermal_protected" : hazard + "_unprotected")),
                    33, 184, rgb);
            // The localized readout uses the full row; its color and exposure bar carry the warning.
        }

        private void drawFlat(GuiGraphics g, float partialTick) {
            var s = EppClientState.snapshot;
            var mc = Minecraft.getInstance();
            int warning = OxygenRules.warning(s.oxygen(), s.capacity());
            boolean danger = s.airless() && (!s.equipped() || warning >= 2);
            int rgb = danger ? 0xFF9477 : 0x95E8E2;
            float fraction = s.equipped() ? Math.clamp((float) s.oxygen() / Math.max(1, s.capacity()), 0f, 1f) : 0f;
            // All content is flat here; only the final texture mesh bends it.
            int alpha = s.refilling()
                    ? 150 + (int) (35 * Math.sin((mc.player.tickCount + partialTick) * 0.16)) : 170;
            int filled = Math.round(116 * fraction);
            g.fill(6, 24, 122, 25, 0x28000000 | rgb);
            if (filled > 0) {
                g.fill(6, 23, 6 + filled, 26, 0x14000000 | rgb);
                g.fill(6, 24, 6 + filled, 25, (alpha << 24) | rgb);
            }

            String value = "O₂  " + (s.equipped() ? Math.round(100f * fraction) + "%" : "—");
            drawFlatText(g, mc.font, Component.literal(value), 9, 216, rgb);
            if (!s.equipped() || warning >= 1) {
                // Amber stays steady; urgent warnings pulse once per second.
                boolean urgent = !s.equipped() || warning >= 2;
                float phase = (mc.player.tickCount + partialTick) * (float) Math.PI / 10;
                int opacity = urgent ? Math.round(140 + 100 * (float) Math.cos(phase)) : 210;
                int warningRgb = urgent ? 0xFF9477 : 0xFFD17C;
                drawWarning(g, opacity, warningRgb);
            }

            // Routine operation stays silent; only actionable states add a caption.
            String key = !s.equipped() ? "no_epp" : s.airless() && s.oxygen() == 0 ? "depleted"
                    : s.airless() && warning >= 2 ? "critical" : s.refilling() ? "refill" : null;
            if (key != null) {
                var caption = Component.translatable("hud.starboundmc.epp." + key);
                drawFlatText(g, mc.font, caption, 33, 184, rgb);
            }
        }

        /** Draw into the same transparent texture as the readout. */
        private static void drawWarning(GuiGraphics g, int alpha, int rgb) {
            g.pose().pushPose();
            g.pose().translate(21, 9, 0);
            g.pose().scale(0.5f, 0.5f, 1);
            int color = (alpha << 24) | rgb;
            for (int row = 0; row < 18; row++) {
                int halfWidth = row / 2;
                g.fill(-halfWidth, row, -halfWidth + 2, row + 1, color);
                if (halfWidth > 0) g.fill(halfWidth, row, halfWidth + 2, row + 1, color);
            }
            g.fill(-8, 17, 10, 19, color);
            g.fill(0, 6, 2, 12, color);
            g.fill(0, 14, 2, 16, color);
            g.pose().popPose();
        }

        private static void drawFlatText(GuiGraphics g, Font font, Component text,
                                         float y, int alpha, int rgb) {
            var ordered = text.getVisualOrderText();
            float scale = Math.min(1f, 108f / Math.max(1, font.width(ordered)));
            g.pose().pushPose();
            g.pose().translate((SURVIVAL_WIDTH - font.width(ordered) * scale) / 2, y, 0);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, ordered, 0, 0, (alpha << 24) | rgb, false);
            g.pose().popPose();
        }

        /** Tiny, bounded optical lag; reset after hiding, menus, pauses or camera cuts. */
        private void updateDrift(Minecraft mc) {
            long now = System.nanoTime();
            float seconds = (now - lastFrame) / 1_000_000_000f;
            float yaw = mc.gameRenderer.getMainCamera().getYRot();
            float pitch = mc.gameRenderer.getMainCamera().getXRot();
            float turn = Mth.wrapDegrees(yaw - lastYaw);
            float tilt = pitch - lastPitch;
            if (lastFrame == 0 || seconds > 0.25f || mc.isPaused() || mc.screen != null
                    || !mc.options.getCameraType().isFirstPerson() || Math.abs(turn) > 45 || Math.abs(tilt) > 45) {
                driftX = driftY = 0;
            } else {
                float decay = (float) Math.exp(-9 * seconds);
                driftX = Math.clamp(driftX * decay - turn * 0.065f, -1.8f, 1.8f);
                driftY = Math.clamp(driftY * decay + tilt * 0.045f, -1.2f, 1.2f);
            }
            lastFrame = now;
            lastYaw = yaw;
            lastPitch = pitch;
        }
    }
}
