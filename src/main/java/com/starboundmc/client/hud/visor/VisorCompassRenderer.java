// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import com.starboundmc.client.hud.animation.HudComponentPresentation;

/** Flat compass artwork with smooth motion and directional fading within the component. */
public final class VisorCompassRenderer {
    private VisorCompassRenderer() { }

    public static void draw(GuiGraphics graphics) {
        draw(graphics, 1F);
    }

    public static void draw(GuiGraphics graphics, float establishment) {
        var mc = Minecraft.getInstance();
        float heading = Mth.positiveModulo(mc.gameRenderer.getMainCamera().getYRot() + 180, 360);
        for (int degree = 0; degree < 360; degree += 10) {
            float offset = Mth.wrapDegrees(degree - heading);
            if (Math.abs(offset) > 80)
                continue;
            float x = 136 + offset * 1.5F;
            float edge = Math.clamp((80 - Math.abs(offset)) / 26F, 0F, 1F);
            edge = edge * edge * (3 - 2 * edge);
            float reveal = HudComponentPresentation.compassReveal(establishment, Math.abs(offset) / 80F);
            int alpha = Math.round(210 * (1 - Math.abs(offset) / 105) * edge * reveal);
            int color = alpha << 24 | 0x95E8E2;
            // Keep subpixel positions through the supersampled component texture.
            graphics.pose().pushPose();
            graphics.pose().translate(x, 0, 0);
            graphics.fill(0, 19, 1, degree % 30 == 0 ? 26 : 23, color);
            if (degree % 30 == 0) {
                String label = switch (degree) {
                    case 0 -> "N";
                    case 90 -> "E";
                    case 180 -> "S";
                    case 270 -> "W";
                    default -> Integer.toString(degree);
                };
                // Font treats alpha 0..3 as opaque; skip effectively invisible labels.
                if (alpha >= 4)
                    graphics.drawCenteredString(mc.font, label, 0, 7, color);
            }
            graphics.pose().popPose();
        }
        float reference = HudComponentPresentation.smooth(establishment / .18F);
        graphics.fill(135, 26, 138, 30, Math.round(238 * reference) << 24 | 0x95E8E2);
        if (Math.round(236 * reference) >= 4)
            graphics.drawCenteredString(mc.font, Math.round(heading) % 360 + "°", 136, 34,
                    Math.round(236 * reference) << 24 | 0xC5EFEB);
    }
}
