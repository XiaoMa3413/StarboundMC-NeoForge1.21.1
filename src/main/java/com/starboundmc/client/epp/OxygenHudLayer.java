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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class OxygenHudLayer implements ModularHudLayer {
    public static final OxygenHudLayer INSTANCE = new OxygenHudLayer();
    private ModularUI ui;
    @Override public ModularUI getModularUI() {
        var mc = Minecraft.getInstance(); var s = EppClientState.snapshot;
        if (mc.player == null || !mc.player.isAlive() || mc.options.hideGui || s == null
                || !(s.equipped() || s.airless() || s.exposure() > 0)) return null;
        if (ui == null) ui = ModularUI.of(UI.of(new Gauge(), ResourceLocation.fromNamespaceAndPath("starboundmc", "lss/epp.lss")));
        return ui;
    }
    @Override public void render(GuiGraphics graphics, DeltaTracker dt) {
        var current = getModularUI();
        if (current != null && validModularUI(current)) current.getWidget().render(graphics, Integer.MAX_VALUE, Integer.MAX_VALUE, dt.getGameTimeDeltaPartialTick(false));
    }
    public void reset() { if (ui != null) ui.onRemoved(); ui = null; }
    private static final class Gauge extends UIElement {
        private long lastFrame;
        private float lastYaw, lastPitch, driftX, driftY;
        private static final float ARC_WIDTH = 116f;

        Gauge() { layout(l -> l.widthPercent(100).heightPercent(100)); setAllowHitTest(false); }
        @Override public void drawBackgroundAdditional(GUIContext context) {
            var s = EppClientState.snapshot; if (s == null) return;
            var mc = Minecraft.getInstance(); var g = context.graphics;
            int x = g.guiWidth() - 140, y = g.guiHeight() - 82;
            updateDrift(mc);
            g.pose().pushPose();
            g.pose().translate(x + driftX, y + driftY, 0);
            int warning = OxygenRules.warning(s.oxygen(), s.capacity());
            boolean danger = s.airless() && (!s.equipped() || warning >= 2);
            int rgb = danger ? 0xFF9477 : 0x95E8E2;
            float fraction = s.equipped() ? Math.clamp((float) s.oxygen() / Math.max(1, s.capacity()), 0f, 1f) : 0f;
            // Open visor arc, rising toward the screen edge. Half-pixel samples keep
            // its shallow curvature readable without a panel or a heavy outline.
            int alpha = s.refilling()
                    ? 150 + (int) (35 * Math.sin((mc.player.tickCount + context.partialTick) * 0.16)) : 170;
            g.pose().pushPose();
            g.pose().scale(0.5f, 0.5f, 1f);
            for (int i = 0; i < 232; i++) {
                float t = i / 231f;
                int bend = Math.round(2 * curve(t * ARC_WIDTH));
                float fade = edgeFade(t);
                int opacity = Math.round((t < fraction ? alpha : 40) * fade);
                g.fill(i, bend - 2, i + 1, bend + 3, (Math.round(20 * fade) << 24) | rgb);
                g.fill(i, bend, i + 1, bend + 1, (opacity << 24) | rgb);
            }
            g.pose().popPose();

            String value = "O₂  " + (s.equipped() ? Math.round(100f * fraction) + "%" : "—");
            drawProjectedText(g, mc.font, Component.literal(value), -15, 216, rgb);
            if (!s.equipped() || warning >= 1) {
                // Amber stays steady; urgent warnings pulse once per second.
                boolean urgent = !s.equipped() || warning >= 2;
                float phase = (mc.player.tickCount + context.partialTick) * (float) Math.PI / 10;
                int opacity = urgent ? Math.round(140 + 100 * (float) Math.cos(phase)) : 210;
                int warningRgb = urgent ? 0xFF9477 : 0xFFD17C;
                drawWarning(g, opacity, warningRgb);
            }

            // Routine operation stays silent; only actionable states add a caption.
            String key = !s.equipped() ? "no_epp" : s.airless() && s.oxygen() == 0 ? "depleted"
                    : s.airless() && warning >= 2 ? "critical" : s.refilling() ? "refill" : null;
            if (key != null) {
                var caption = Component.translatable("hud.starboundmc.epp." + key);
                drawProjectedText(g, mc.font, caption, 9, 184, rgb);
            }
            g.pose().popPose();
        }

        private static float curve(float x) {
            float t = x / ARC_WIDTH;
            return -18 * t * t;
        }

        private static float edgeFade(float t) {
            return 0.55f + 0.45f * (float) Math.sin(Math.PI * Math.clamp(t, 0f, 1f));
        }

        private static void project(GuiGraphics g, float center, float offset) {
            float slope = -36 * center / (ARC_WIDTH * ARC_WIDTH);
            float normal = (float) Math.sqrt(1 + slope * slope);
            g.pose().translate(center - offset * slope / normal, curve(center) + offset / normal, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan(slope))));
        }

        /** Small outlined triangle beside the readout; uses the same surface as text. */
        private static void drawWarning(GuiGraphics g, int alpha, int rgb) {
            g.pose().pushPose();
            project(g, 15, -15);
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

        /** Both rows use the same visor surface, including localized warning glyphs. */
        private static void drawProjectedText(GuiGraphics g, Font font, Component text,
                                              float offset, int alpha, int rgb) {
            var ordered = text.getVisualOrderText();
            float scale = Math.min(1f, 108f / Math.max(1, font.width(ordered)));
            float[] cursor = {(ARC_WIDTH - font.width(ordered) * scale) / 2};
            ordered.accept((index, style, codePoint) -> {
                var glyph = FormattedCharSequence.codepoint(codePoint, style);
                float width = font.width(glyph) * scale;
                float center = cursor[0] + width / 2;
                int opacity = Math.round(alpha * edgeFade(center / ARC_WIDTH));
                g.pose().pushPose();
                project(g, center, offset);
                g.pose().scale(scale, scale, 1);
                g.drawString(font, glyph, -font.width(glyph) / 2f, 0, (opacity << 24) | rgb, false);
                g.pose().popPose();
                cursor[0] += width;
                return true;
            });
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
                float decay = (float) Math.exp(-10 * seconds);
                driftX = Math.clamp(driftX * decay - turn * 0.06f, -1.5f, 1.5f);
                driftY = Math.clamp(driftY * decay + tilt * 0.04f, -1f, 1f);
            }
            lastFrame = now;
            lastYaw = yaw;
            lastPitch = pitch;
        }
    }
}
