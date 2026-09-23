// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud;

import com.starboundmc.client.hud.animation.SurvivalFeedback;
import com.starboundmc.epp.OxygenRules;
import com.starboundmc.network.EppSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Native survival artwork, shared by gameplay and the opt-in rendering checks. */
public final class HudSurvivalRenderer {
    private HudSurvivalRenderer() { }

    public static float fraction(EppSnapshotPacket s, SurvivalFeedback.Kind kind) {
        return switch (kind) {
            case OXYGEN -> s.equipped() ? Math.clamp((float) s.oxygen() / Math.max(1, s.capacity()), 0F, 1F) : 0;
            case COLD -> Math.clamp(s.coldExposure() / 100F, 0F, 1F);
            case HEAT -> Math.clamp(s.heatExposure() / 100F, 0F, 1F);
        };
    }

    public static int severity(EppSnapshotPacket s, SurvivalFeedback.Kind kind) {
        if (kind == SurvivalFeedback.Kind.OXYGEN)
            return !s.airless() ? 0 : !s.equipped() ? 3 : Math.min(3, OxygenRules.warning(s.oxygen(), s.capacity()));
        int exposure = kind == SurvivalFeedback.Kind.HEAT ? s.heatExposure() : s.coldExposure();
        return exposure >= 75 ? 3 : exposure >= 50 ? 2 : exposure >= 25 ? 1 : 0;
    }

    public static void draw(GuiGraphics g, EppSnapshotPacket snapshot, SurvivalFeedback feedback) {
        if (feedback.kind() == SurvivalFeedback.Kind.OXYGEN) oxygen(g, snapshot, feedback);
        else thermal(g, snapshot, feedback);
    }

    private static void thermal(GuiGraphics g, EppSnapshotPacket s, SurvivalFeedback feedback) {
        boolean heat = feedback.kind() == SurvivalFeedback.Kind.HEAT;
        int exposure = heat ? s.heatExposure() : s.coldExposure();
        String hazard = heat ? "heat" : "cold";
        int rgb = exposure >= 75 ? 0xFF9477 : exposure >= 25 ? 0xFFD17C : heat ? 0xF3BE96 : 0xA7DFFF;
        int filled = Math.round(116 * feedback.displayed());
        g.fill(6, 24, 122, 25, 0x28000000 | rgb);
        if (filled > 0) g.fill(6, 24, 6 + filled, 25, 0xBA000000 | rgb);
        text(g, Component.translatable("hud.starboundmc.epp." + hazard, exposure), 9, 236, rgb);
        boolean protectedFromHazard = (heat ? s.heatProtection() : s.coldProtection()) > 0;
        boolean hazardous = (heat ? s.heatTier() : s.coldTier()) > 0;
        text(g, Component.translatable("hud.starboundmc.epp." +
                (!hazardous ? "thermal_recovery" : protectedFromHazard ? "thermal_protected" : hazard + "_unprotected")),
                33, 184, rgb);
        int cue = Math.round(170 * feedback.pulse());
        if (cue > 0) {
            // Heat rises above the bar; cold settles below it. The readout itself stays still.
            int top = heat ? 21 : 25;
            g.fill(6, top, 7, top + 3, cue << 24 | rgb);
            g.fill(121, top, 122, top + 3, cue << 24 | rgb);
        }
    }

    private static void oxygen(GuiGraphics g, EppSnapshotPacket s, SurvivalFeedback feedback) {
        int warning = OxygenRules.warning(s.oxygen(), s.capacity());
        boolean danger = s.airless() && (!s.equipped() || warning >= 2);
        int rgb = danger ? 0xFF9477 : 0x95E8E2;
        int filled = Math.round(116 * feedback.displayed());
        g.fill(6, 24, 122, 25, 0x28000000 | rgb);
        if (filled > 0) {
            g.fill(6, 23, 6 + filled, 26, 0x14000000 | rgb);
            g.fill(6, 24, 6 + filled, 25, 0xAA000000 | rgb);
        }
        // Numerical truth is never delayed by the visual bar's recovery.
        String value = "O₂  " + (s.equipped() ? Math.round(100 * fraction(s, SurvivalFeedback.Kind.OXYGEN)) + "%" : "—");
        text(g, Component.literal(value), 9, 236, rgb);
        if (!s.equipped() || warning >= 1) {
            boolean urgent = !s.equipped() || warning >= 2;
            float pulse = feedback.pulse();
            warning(g, Math.round((urgent ? 180 : 210) + (urgent ? 60 : 35) * pulse),
                    urgent ? 0xFF9477 : 0xFFD17C, -.35F * pulse);
        }
        String key = !s.equipped() ? "no_epp" : s.airless() && s.oxygen() == 0 ? "depleted"
                : s.airless() && warning >= 2 ? "critical" : s.refilling() ? "refill" : null;
        if (key != null) text(g, Component.translatable("hud.starboundmc.epp." + key), 33, 184, rgb);
    }

    private static void warning(GuiGraphics g, int alpha, int rgb, float offsetY) {
        g.pose().pushPose();
        g.pose().translate(21, 9 + offsetY, 0);
        g.pose().scale(.5F, .5F, 1);
        int color = alpha << 24 | rgb;
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

    private static void text(GuiGraphics g, Component text, float y, int alpha, int rgb) {
        var font = Minecraft.getInstance().font;
        var ordered = text.getVisualOrderText();
        float scale = Math.min(1F, 108F / Math.max(1, font.width(ordered)));
        g.pose().pushPose();
        g.pose().translate((128 - font.width(ordered) * scale) / 2, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, ordered, 0, 0, alpha << 24 | rgb, true);
        g.pose().popPose();
    }
}
