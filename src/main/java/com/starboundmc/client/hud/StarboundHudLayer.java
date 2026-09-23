// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.starboundmc.client.hud.ar.ArWorldRenderer;
import com.starboundmc.client.hud.animation.HudAnimationClock;
import com.starboundmc.client.hud.animation.HudComponentPresentation;
import com.starboundmc.client.hud.animation.SurvivalFeedback;
import com.starboundmc.client.hud.visor.HudVisorGeometry;
import com.starboundmc.client.hud.visor.HudVisorCalibrationGrid;
import com.starboundmc.client.hud.visor.HudVisorGeometry.Profile;
import com.starboundmc.client.hud.visor.HudVisorMotion;
import com.starboundmc.client.hud.visor.HudVisorProjection;
import com.starboundmc.client.hud.visor.VisorCompassRenderer;
import com.starboundmc.client.epp.EppClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
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
    private HudRoot root;
    private final HudVisorMotion visorMotion = new HudVisorMotion();
    private final HudVisorProjection survivalProjection = new HudVisorProjection(SURVIVAL_WIDTH, COMPONENT_HEIGHT, Profile.SURVIVAL);
    private final HudVisorProjection compassProjection = new HudVisorProjection(272, COMPONENT_HEIGHT, Profile.COMPASS);
    private final HudVisorProjection controlsProjection = new HudVisorProjection(360, COMPONENT_HEIGHT, Profile.EVA_CONTROLS);
    private final HudVisorProjection bootStatusProjection = new HudVisorProjection(
            HudBootStatusRenderer.WIDTH, HudBootStatusRenderer.HEIGHT);
    @Override public ModularUI getModularUI() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive()) { releaseUi(); return null; }
        if (mc.options.hideGui) { resetVisorMotion(); return null; }
        if (ui == null) {
            root = new HudRoot(survivalProjection, compassProjection, controlsProjection,
                    bootStatusProjection, visorMotion);
            ui = ModularUI.of(UI.of(root,
                    ResourceLocation.fromNamespaceAndPath("starboundmc", "lss/epp.lss")));
        }
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

    public void resetVisorMotion() {
        visorMotion.reset();
        if (root != null) root.suspendPresentation();
    }

    private void releaseUi() {
        resetVisorMotion();
        if (ui != null) ui.onRemoved();
        ui = null;
        root = null;
        survivalProjection.close();
        compassProjection.close();
        controlsProjection.close();
        bootStatusProjection.close();
        ArWorldRenderer.INSTANCE.reset();
    }
    private static boolean showEvaNavigation() {
        var player = Minecraft.getInstance().player;
        return player != null && player.level().dimension().equals(com.starboundmc.world.ShipDimensions.SHIP_LEVEL)
                && com.starboundmc.epp.EvaMovement.mode(player) != com.starboundmc.epp.EvaState.NORMAL;
    }
    private static final class HudRoot extends UIElement {
        private long lastFrame;
        private final HudVisorMotion motion;
        private net.minecraft.world.entity.Entity lastCameraEntity;
        private net.minecraft.client.multiplayer.ClientLevel lastCameraLevel;
        private final OxygenHudFade oxygenFade = new OxygenHudFade();
        private final OxygenHudFade coldFade = new OxygenHudFade();
        private final OxygenHudFade heatFade = new OxygenHudFade();
        private final HudVisibilityFade navigationFade = new HudVisibilityFade();
        private final HudVisibilityFade evaControlsFade = new HudVisibilityFade();
        private final HudAnimationClock animationClock = new HudAnimationClock();
        private final HudComponentPresentation navigation = new HudComponentPresentation(HudComponentPresentation.Kind.NAVIGATION);
        private final HudComponentPresentation controls = new HudComponentPresentation(HudComponentPresentation.Kind.CONTROLS);
        private final HudComponentPresentation oxygen = new HudComponentPresentation(HudComponentPresentation.Kind.TELEMETRY);
        private final HudComponentPresentation cold = new HudComponentPresentation(HudComponentPresentation.Kind.TELEMETRY);
        private final HudComponentPresentation heat = new HudComponentPresentation(HudComponentPresentation.Kind.TELEMETRY);
        private final SurvivalFeedback oxygenFeedback = new SurvivalFeedback(SurvivalFeedback.Kind.OXYGEN);
        private final SurvivalFeedback coldFeedback = new SurvivalFeedback(SurvivalFeedback.Kind.COLD);
        private final SurvivalFeedback heatFeedback = new SurvivalFeedback(SurvivalFeedback.Kind.HEAT);
        private boolean lastThrust;
        private final HudVisorProjection survivalProjection;
        private final HudVisorProjection compassProjection;
        private final HudVisorProjection controlsProjection;
        private final HudVisorProjection bootStatusProjection;

        HudRoot(HudVisorProjection survivalProjection, HudVisorProjection compassProjection,
                HudVisorProjection controlsProjection, HudVisorProjection bootStatusProjection,
                HudVisorMotion motion) {
            this.survivalProjection = survivalProjection;
            this.compassProjection = compassProjection;
            this.controlsProjection = controlsProjection;
            this.bootStatusProjection = bootStatusProjection;
            this.motion = motion;
            layout(l -> l.widthPercent(100).heightPercent(100)); setAllowHitTest(false);
        }
        @Override public void drawBackgroundAdditional(GUIContext context) {
            var mc = Minecraft.getInstance();
            long now = System.nanoTime();
            double elapsed = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000D;
            lastFrame = now;
            updateDrift(mc, elapsed);
            float seconds = (float) animationClock.advance(now,
                    !mc.isPaused() && mc.screen == null && !mc.options.hideGui
                            && mc.options.getCameraType().isFirstPerson());
            var boot = HudBootController.INSTANCE.presentation(context.partialTick);
            if (HudVisorCalibrationGrid.enabled()) {
                var g = context.graphics;
                float bottom = HudVisorGeometry.survivalAnchor(g.guiWidth(), g.guiHeight(), 2, 1,
                        survivalProjection.profile()).y();
                g.pose().pushPose();
                g.pose().translate(motion.x(), motion.y(), 0);
                HudVisorCalibrationGrid.renderGuides(g, 42, bottom,
                        compassProjection.profile(), survivalProjection.profile());
                g.pose().popPose();
            }
            if (mc.screen == null)
                drawBootPresentation(context.graphics, boot);
            boolean eva = showEvaNavigation();
            if (eva) lastThrust = com.starboundmc.epp.EvaMovement.mode(mc.player)
                    == com.starboundmc.epp.EvaState.THRUST;
            float delta = mc.isPaused() ? 0 : seconds;
            float navigationOpacity = navigationFade.update(
                    delta, eva || HudBootController.INSTANCE.localNavigationActive()
                            || ArWorldRenderer.INSTANCE.hasTargets());
            float controlsOpacity = evaControlsFade.update(delta, eva);
            navigation.update(delta, navigationOpacity > .001F);
            controls.update(delta, controlsOpacity > .001F);
            if (navigationOpacity > .001f)
                drawNavigation(context.graphics, navigationOpacity, controlsOpacity);
            var s = EppClientState.snapshot; if (s == null) return;
            oxygenFeedback.update(delta, HudSurvivalRenderer.fraction(s, oxygenFeedback.kind()), HudSurvivalRenderer.severity(s, oxygenFeedback.kind()));
            coldFeedback.update(delta, HudSurvivalRenderer.fraction(s, coldFeedback.kind()), HudSurvivalRenderer.severity(s, coldFeedback.kind()));
            heatFeedback.update(delta, HudSurvivalRenderer.fraction(s, heatFeedback.kind()), HudSurvivalRenderer.severity(s, heatFeedback.kind()));
            var g = context.graphics;
            float opacity = oxygenFade.update(
                    mc.isPaused() ? 0 : seconds, s.airless() || s.exposure() > 0 || s.refilling());
            boolean oxygenVisible = opacity > .001f;
            boolean coldVisible = s.coldTier() > 0 || s.coldExposure() > 0;
            boolean heatVisible = s.heatTier() > 0 || s.heatExposure() > 0;
            float coldOpacity = coldFade.update(mc.isPaused() ? 0 : seconds, coldVisible && s.coldProtection() <= 0);
            float heatOpacity = heatFade.update(mc.isPaused() ? 0 : seconds, heatVisible && s.heatProtection() <= 0);
            coldVisible = coldOpacity > .001f;
            heatVisible = heatOpacity > .001f;
            oxygen.update(delta, oxygenVisible);
            cold.update(delta, coldVisible);
            heat.update(delta, heatVisible);
            int rows = (oxygenVisible ? 1 : 0) + (coldVisible ? 1 : 0) + (heatVisible ? 1 : 0);
            if (rows == 0) return;
            var anchor = HudVisorGeometry.survivalAnchor(g.guiWidth(), g.guiHeight(), rows, 0,
                    survivalProjection.profile());
            float x = anchor.x();
            float y = anchor.y();
            float visorOpacity = HudBootController.INSTANCE.visorOpacity();
            g.pose().pushPose();
            g.pose().translate(motion.x(), motion.y(), 0);
            if (oxygenVisible) survivalProjection.draw(g, x, y + oxygen.offsetY(),
                    SURVIVAL_WIDTH, COMPONENT_HEIGHT, opacity * visorOpacity, Math.max(oxygen.glow(), oxygenFeedback.glow()),
                    canvas -> HudSurvivalRenderer.draw(canvas, s, oxygenFeedback));
            int row = oxygenVisible ? 40 : 0;
            if (coldVisible) {
                survivalProjection.draw(g, x, y + row + cold.offsetY(),
                        SURVIVAL_WIDTH, COMPONENT_HEIGHT, coldOpacity * visorOpacity, Math.max(cold.glow(), coldFeedback.glow()),
                        canvas -> HudSurvivalRenderer.draw(canvas, s, coldFeedback));
                row += 40;
            }
            if (heatVisible) survivalProjection.draw(g, x, y + row + heat.offsetY(),
                    SURVIVAL_WIDTH, COMPONENT_HEIGHT, heatOpacity * visorOpacity, Math.max(heat.glow(), heatFeedback.glow()),
                    canvas -> HudSurvivalRenderer.draw(canvas, s, heatFeedback));
            g.pose().popPose();
        }

        private void drawBootPresentation(GuiGraphics g, HudBootController.Presentation boot) {
            if (boot.statusOpacity() > .001F) {
                float fit = Math.min(1F,
                        Math.max(1F, g.guiWidth() - 20F) / HudBootStatusRenderer.WIDTH);
                float width = HudBootStatusRenderer.WIDTH * fit;
                float height = HudBootStatusRenderer.HEIGHT * fit;
                bootStatusProjection.draw(g, (g.guiWidth() - width) / 2F, g.guiHeight() / 2F - 8 * fit,
                        width, height, boot.statusOpacity(), .04F,
                        canvas -> HudBootStatusRenderer.draw(canvas, boot));
            }
        }

        private void drawNavigation(GuiGraphics g, float opacity, float controlsOpacity) {
            var mc = Minecraft.getInstance();
            int x = g.guiWidth() / 2;
            float navFit = Math.min(1f, (g.guiWidth() - 16f) / 272f);
            float visorOpacity = HudBootController.INSTANCE.visorOpacity();
            g.pose().pushPose();
            g.pose().translate(motion.x(), motion.y(), 0);
            compassProjection.draw(g, x - 136 * navFit, 18 + navigation.offsetY(),
                    272 * navFit, COMPONENT_HEIGHT * navFit, opacity * visorOpacity, navigation.glow(),
                    canvas -> VisorCompassRenderer.draw(canvas, navigation.progress()));
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
            controlsProjection.draw(g, x - width / 2, 63 + controls.offsetY(),
                    width, COMPONENT_HEIGHT * fit, controlsOpacity * visorOpacity, controls.glow(), canvas -> {
                float textScale = Math.min(1f, 340f / Math.max(1, mc.font.width(hint)));
                canvas.pose().pushPose();
                canvas.pose().translate(180, 12, 0); canvas.pose().scale(textScale, textScale, 1);
                canvas.drawCenteredString(mc.font, hint, 0, 0, thrust ? 0xB895E8E2 : 0xD8FFD17C);
                canvas.pose().popPose();
            });
            g.pose().popPose();
        }

        /** Tiny, bounded optical lag; reset after hiding, menus, pauses or camera cuts. */
        private void updateDrift(Minecraft mc, double seconds) {
            var camera = mc.gameRenderer.getMainCamera();
            if (camera.getEntity() != lastCameraEntity || mc.level != lastCameraLevel) {
                motion.reset();
                suspendPresentation();
            }
            lastCameraEntity = camera.getEntity();
            lastCameraLevel = mc.level;
            motion.update(seconds, camera.getYRot(), camera.getXRot(),
                    !mc.isPaused() && mc.screen == null && !mc.options.hideGui
                            && mc.options.getCameraType().isFirstPerson());
        }

        private void suspendPresentation() {
            animationClock.suspend();
            oxygenFeedback.settle(); coldFeedback.settle(); heatFeedback.settle();
            navigation.settle(); controls.settle(); oxygen.settle(); cold.settle(); heat.settle();
        }
    }
}
