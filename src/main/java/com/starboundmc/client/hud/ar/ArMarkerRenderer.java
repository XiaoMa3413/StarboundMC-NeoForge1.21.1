// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

/** Flat artwork around an immutable projected center. Animation changes strokes, never the target anchor. */
public final class ArMarkerRenderer {
    private static final boolean DEBUG = Boolean.getBoolean("starboundmc.debug.hudArStates");
    private final ArLabelLayout labels = new ArLabelLayout();

    public void beginFrame() { labels.clear(); }

    public void draw(GuiGraphics g, ArTarget target, ArTargetProjection.Point point,
                     ArVisualStateCache.State state, float opacity, boolean attentionOwner) {
        float x = point.x(), y = point.y();
        float attention = state.attention(attentionOwner);
        float weight = ArVisualStateCache.important(target) ? 1 : .85F;
        int color = color(target.rgb(), opacity * state.markerOpacity() * (210 * weight + 25 * attention));
        if (point.edge()) {
            double angle = Math.atan2(y - g.guiHeight() / 2F, x - g.guiWidth() / 2F);
            float wing = 7.5F + state.edgeSettle();
            line(g, x, y, x - (float) Math.cos(angle - .55) * wing,
                    y - (float) Math.sin(angle - .55) * wing, color);
            line(g, x, y, x - (float) Math.cos(angle + .55) * wing,
                    y - (float) Math.sin(angle + .55) * wing, color);
        } else {
            float lock = state.lockProgress();
            float radius = 6 + 3 * (1 - lock);
            if (lock < 1) g.fill((int) x, (int) y, (int) x + 1, (int) y + 1, color);
            float segment = target.guidance() == ArGuidanceMode.SIGNAL ? .7F * lock : lock;
            line(g, x, y - radius, x + radius * segment, y - radius * (1 - segment), color);
            line(g, x + radius, y, x + radius * (1 - segment), y + radius * segment, color);
            line(g, x, y + radius, x - radius * segment, y + radius * (1 - segment), color);
            line(g, x - radius, y, x - radius * (1 - segment), y - radius * segment, color);
            if (attention > .01) {
                int accent = color(target.rgb(), 100 * opacity * attention);
                line(g, x - 3, y - 10, x + 3, y - 10, accent);
                line(g, x - 3, y + 10, x + 3, y + 10, accent);
            }
        }

        Component current = withDirection(target.label(), point.behind());
        Component previous = state.previousLabel() == null ? null : withDirection(state.previousLabel(), point.behind());
        var font = Minecraft.getInstance().font;
        int width = Math.max(font.width(current), previous == null ? 0 : font.width(previous));
        float scale = Math.min(.68F, (g.guiWidth() - 20F) / Math.max(1, width));
        float half = width * scale / 2;
        // Only labels yield to each other. The world-locked geometry never moves for layout.
        if (!labels.place(x, y, half, g.guiWidth(), g.guiHeight())) return;
        float labelAlpha = 208 * opacity * state.labelOpacity();
        float blend = state.identityBlend();
        g.pose().pushPose();
        g.pose().translate(labels.x(), labels.y(), 0);
        g.pose().scale(scale, scale, 1);
        if (previous != null) text(g, previous, target.rgb(), labelAlpha * state.previousLabelOpacity());
        text(g, current, target.rgb(), labelAlpha * blend);
        g.pose().popPose();
        if (DEBUG) g.drawString(font, state.phase(), (int) x + 9, (int) y - 9, 0xFF95E8E2);
    }

    private static Component withDirection(Component label, boolean behind) {
        return behind ? label.copy().append(" · ").append(Component.translatable("hud.starboundmc.eva.behind")) : label;
    }

    private static void text(GuiGraphics g, Component text, int rgb, float alpha) {
        if (Math.round(alpha) >= 4) g.drawCenteredString(Minecraft.getInstance().font, text, 0, 0, color(rgb, alpha));
    }

    private static int color(int rgb, float alpha) { return Math.clamp(Math.round(alpha), 0, 255) << 24 | rgb; }

    private static void line(GuiGraphics g, float x, float y, float endX, float endY, int color) {
        float dx = endX - x, dy = endY - y;
        float length = (float) Math.hypot(dx, dy);
        if (length < .001F) return;
        float nx = -dy / length * .55F, ny = dx / length * .55F;
        var buffer = g.bufferSource().getBuffer(RenderType.gui());
        var pose = g.pose().last().pose();
        buffer.addVertex(pose, x + nx, y + ny, 0).setColor(color);
        buffer.addVertex(pose, endX + nx, endY + ny, 0).setColor(color);
        buffer.addVertex(pose, endX - nx, endY - ny, 0).setColor(color);
        buffer.addVertex(pose, x - nx, y - ny, 0).setColor(color);
    }
}
