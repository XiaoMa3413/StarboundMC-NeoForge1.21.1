// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Flat compass artwork; {@link HudVisorProjection} supplies the shared visor geometry. */
public final class VisorCompassRenderer {
    private VisorCompassRenderer() { }

    public static void draw(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        float heading = Mth.positiveModulo(mc.gameRenderer.getMainCamera().getYRot() + 180, 360);
        for (int degree = 0; degree < 360; degree += 10) {
            float offset = Mth.wrapDegrees(degree - heading);
            if (Math.abs(offset) > 80)
                continue;
            int x = Math.round(136 + offset * 1.5F);
            int alpha = Math.round(210 * (1 - Math.abs(offset) / 105));
            int color = alpha << 24 | 0x95E8E2;
            graphics.fill(x, 19, x + 1, degree % 30 == 0 ? 26 : 23, color);
            if (degree % 30 == 0) {
                String label = switch (degree) {
                    case 0 -> "N";
                    case 90 -> "E";
                    case 180 -> "S";
                    case 270 -> "W";
                    default -> Integer.toString(degree);
                };
                graphics.drawCenteredString(mc.font, label, x, 7, color);
            }
        }
        graphics.fill(135, 26, 138, 30, 0xEE95E8E2);
        graphics.drawCenteredString(mc.font, Math.round(heading) % 360 + "°", 136, 34,
                0xCC95E8E2);
    }
}
