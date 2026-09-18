// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.starboundmc.epp.OxygenRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class OxygenHudLayer implements ModularHudLayer {
    public static final OxygenHudLayer INSTANCE = new OxygenHudLayer();
    private ModularUI ui;
    private final VisorHudProjection projection = new VisorHudProjection();
    @Override public ModularUI getModularUI() {
        var mc = Minecraft.getInstance(); var s = EppClientState.snapshot;
        if (mc.player == null || !mc.player.isAlive() || mc.options.hideGui
                || !(showBeacon() || s != null && (s.equipped() || s.airless() || s.exposure() > 0 || s.coldTier() > 0 || s.coldExposure() > 0))) return null;
        if (ui == null) ui = ModularUI.of(UI.of(new Gauge(projection), ResourceLocation.fromNamespaceAndPath("starboundmc", "lss/epp.lss")));
        return ui;
    }
    @Override public void render(GuiGraphics graphics, DeltaTracker dt) {
        var current = getModularUI();
        if (current != null && validModularUI(current)) current.getWidget().render(graphics, Integer.MAX_VALUE, Integer.MAX_VALUE, dt.getGameTimeDeltaPartialTick(false));
    }
    public void reset() { if (ui != null) ui.onRemoved(); ui = null; projection.close(); }
    private static boolean showBeacon() {
        var player = Minecraft.getInstance().player;
        return player != null && player.level().dimension().equals(com.starboundmc.world.ShipDimensions.SHIP_LEVEL)
                && com.starboundmc.epp.EvaMovement.mode(player) != com.starboundmc.epp.EvaState.NORMAL;
    }
    private static final class Gauge extends UIElement {
        private long lastFrame;
        private float lastYaw, lastPitch, driftX, driftY;
        private final VisorHudProjection projection;

        Gauge(VisorHudProjection projection) { this.projection = projection; layout(l -> l.widthPercent(100).heightPercent(100)); setAllowHitTest(false); }
        @Override public void drawBackgroundAdditional(GUIContext context) {
            if (showBeacon()) drawBeacon(context.graphics);
            var s = EppClientState.snapshot; if (s == null) return;
            var mc = Minecraft.getInstance(); var g = context.graphics;
            int x = g.guiWidth() - 140, y = g.guiHeight() - 82;
            updateDrift(mc);
            boolean oxygenVisible = s.equipped() || s.airless() || s.exposure() > 0;
            if (oxygenVisible) projection.draw(g, x + driftX, y + driftY, canvas -> drawFlat(canvas, context.partialTick));
            if (s.coldTier() > 0 || s.coldExposure() > 0)
                projection.draw(g, x + driftX, y + driftY + (oxygenVisible ? 40 : 0), this::drawCold);
        }

        private void drawBeacon(GuiGraphics g) {
            var mc = Minecraft.getInstance();
            var bearing = ShipBeaconBearing.from(mc.player.position(),
                    net.minecraft.world.phys.Vec3.atBottomCenterOf(com.starboundmc.world.ShipStructure.SHIP_TELEPORTER_POS),
                    mc.player.getYRot());
            int x = g.guiWidth() / 2, y = 22;
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) bearing.turnDegrees()));
            for (int row = 0; row < 5; row++) g.fill(-row, row - 5, row + 1, row - 4, 0xD095E8E2);
            g.fill(-1, 0, 2, 5, 0xD095E8E2);
            g.pose().popPose();
            var height = (bearing.height() >= 0 ? "+" : "") + Math.round(bearing.height());
            g.drawCenteredString(mc.font, Component.translatable("hud.starboundmc.eva.beacon",
                    Math.round(bearing.distance()), height), x, y + 12, 0xD095E8E2);
            boolean thrust = com.starboundmc.epp.EvaMovement.mode(mc.player) == com.starboundmc.epp.EvaState.THRUST;
            var hint = thrust ? Component.translatable("hud.starboundmc.eva.controls",
                    mc.options.keyUp.getTranslatedKeyMessage(), mc.options.keyLeft.getTranslatedKeyMessage(),
                    mc.options.keyDown.getTranslatedKeyMessage(), mc.options.keyRight.getTranslatedKeyMessage(),
                    mc.options.keyJump.getTranslatedKeyMessage(), mc.options.keyShift.getTranslatedKeyMessage())
                    : Component.translatable("hud.starboundmc.eva.no_thrusters",
                            com.starboundmc.client.ModKeyBindings.returnToShip.getTranslatedKeyMessage());
            float scale = Math.min(1f, (g.guiWidth() - 20f) / Math.max(1, mc.font.width(hint)));
            g.pose().pushPose();
            g.pose().translate(x, y + 24, 0);
            g.pose().scale(scale, scale, 1);
            g.drawCenteredString(mc.font, hint, 0, 0, thrust ? 0xB095E8E2 : 0xFFFFD17C);
            g.pose().popPose();
        }

        private void drawCold(GuiGraphics g) {
            var s = EppClientState.snapshot;
            var font = Minecraft.getInstance().font;
            int rgb = s.coldExposure() >= 75 ? 0xFF9477 : s.coldExposure() >= 25 ? 0xFFD17C : 0xA7DFFF;
            g.fill(6, 24, 122, 25, 0x28000000 | rgb);
            int filled = Math.round(116f * s.coldExposure() / 100);
            if (filled > 0) g.fill(6, 24, 6 + filled, 25, 0xBA000000 | rgb);
            drawFlatText(g, font, Component.translatable("hud.starboundmc.epp.cold", s.coldExposure()), 9, 216, rgb);
            drawFlatText(g, font, Component.translatable("hud.starboundmc.epp.cold_protection", s.coldProtection(), s.coldTier()), 33, 184, rgb);
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
            g.pose().translate((VisorSurface.WIDTH - font.width(ordered) * scale) / 2, y, 0);
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
