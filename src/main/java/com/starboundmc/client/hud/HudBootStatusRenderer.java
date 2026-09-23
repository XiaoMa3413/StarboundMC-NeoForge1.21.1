// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** One quiet capability cue below the reticle; no terminal log, enclosing panel or scanning surface. */
public final class HudBootStatusRenderer {
    public static final int WIDTH = 228;
    public static final int HEIGHT = 64;
    private static final int CYAN = 0x95E8E2;
    private static final int AMBER = 0xFFD17C;

    private HudBootStatusRenderer() { }

    public static void draw(GuiGraphics graphics, HudBootController.Presentation presentation) {
        var cue = presentation.cue();
        if (cue == HudBootController.Cue.NONE) return;
        boolean limited = cue == HudBootController.Cue.CORE_UNAVAILABLE || cue == HudBootController.Cue.SAFE_MODE;
        int accent = limited ? AMBER : CYAN;
        String key = switch (cue) {
            case VISUAL_RESTORE -> "restore";
            case LOCAL_NAVIGATION -> "navigation";
            case CORE_UNAVAILABLE -> "unavailable";
            case SAFE_MODE -> "safe";
            case PERSONAL_INITIALIZATION -> "personal";
            case CORE_CONNECTING -> "connecting";
            case CORE_SYNCHRONIZING -> "synchronizing";
            case ONLINE -> "online";
            default -> throw new IllegalStateException("No artwork for " + cue);
        };
        line(graphics, Component.translatable("hud.starboundmc.capability." + key), 28, 236, accent);
        line(graphics, Component.translatable("hud.starboundmc.capability." + key + ".detail"), 42, 180, accent);
        // A small reference mark distinguishes system cues from the independent communication layer.
        graphics.fill(WIDTH / 2 - 6, 58, WIDTH / 2 + 6, 59, 0x60000000 | accent);

        float calibration = presentation.calibrationProgress();
        if (calibration > 0 && calibration < 1) {
            float amount = (float) Math.sin(Math.PI * calibration);
            int color = Math.round(110 * amount) << 24 | CYAN;
            int radius = Math.round(3 + 3 * (1 - calibration));
            int center = WIDTH / 2;
            // Four short references settle around the reticle once; never a screen-wide sweep.
            graphics.fill(center - radius - 4, 8, center - radius, 9, color);
            graphics.fill(center + radius, 8, center + radius + 4, 9, color);
            graphics.fill(center, 8 - radius, center + 1, 11 - radius, color);
            graphics.fill(center, 6 + radius, center + 1, 9 + radius, color);
        }
    }

    private static void line(GuiGraphics graphics, Component text, int y, int alpha, int rgb) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(1F, (WIDTH - 16F) / Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(WIDTH / 2F, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawCenteredString(font, text, 0, 0, alpha << 24 | rgb);
        graphics.pose().popPose();
    }
}
