// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Compact visor-native boot readout; no full-screen surface or persistent panel. */
final class HudBootStatusRenderer {
    static final int WIDTH = 192;
    static final int HEIGHT = 68;
    static final int PROMPT_WIDTH = 228;
    static final int PROMPT_HEIGHT = 36;
    private static final int CYAN = 0x95E8E2;
    private static final int AMBER = 0xFFD17C;
    private static final int RED = 0xFF9477;
    private static final String[] STEP_KEYS = {
            "hud.starboundmc.boot.visor_bus",
            "hud.starboundmc.boot.visual_link",
            "hud.starboundmc.boot.optical_array",
            "hud.starboundmc.boot.local_nav",
            "hud.starboundmc.boot.ship_network",
            "hud.starboundmc.boot.nova_core"
    };
    private static final int[] STEP_COLORS = {CYAN, CYAN, CYAN, AMBER, RED, RED};

    private HudBootStatusRenderer() { }

    static void draw(GuiGraphics graphics, HudBootController.Presentation presentation) {
        var font = Minecraft.getInstance().font;
        boolean starting = presentation.state() == HudBootController.State.STARTING;
        Component header = Component.translatable(starting
                ? "hud.starboundmc.boot.starting" : "hud.starboundmc.boot.safe_mode");
        graphics.fill(3, 3, WIDTH - 3, HEIGHT - 3, 0x24050D12);
        graphics.fill(5, 5, 7, 15, 0xB0000000 | (starting ? CYAN : AMBER));
        graphics.drawString(font, header, 11, 6, 0xD8000000 | (starting ? CYAN : AMBER), false);
        graphics.fill(5, 17, WIDTH - 6, 18, 0x30000000 | CYAN);

        int lines = presentation.revealedLines();
        for (int index = 0; index < Math.min(lines, STEP_KEYS.length); index++) {
            float y = 23F + (index - presentation.scrollRows()) * 11F;
            if (y >= 19F && y < HEIGHT - 2F)
                line(graphics, Component.translatable(STEP_KEYS[index]), y, STEP_COLORS[index]);
        }

        int scanY = 19 + Math.round(46F * presentation.scanProgress());
        graphics.fill(4, scanY, WIDTH - 5, scanY + 1, 0x18000000 | CYAN);
    }

    static void drawPrompt(GuiGraphics graphics, HudBootController.Presentation presentation) {
        var font = Minecraft.getInstance().font;
        graphics.fill(1, 1, PROMPT_WIDTH - 1, PROMPT_HEIGHT - 1, 0x42050D12);
        graphics.fill(1, 1, PROMPT_WIDTH - 1, 2, 0x78000000 | CYAN);
        graphics.fill(1, PROMPT_HEIGHT - 2, PROMPT_WIDTH - 1, PROMPT_HEIGHT - 1,
                0x78000000 | CYAN);
        graphics.fill(1, 1, 2, PROMPT_HEIGHT - 1, 0x78000000 | CYAN);
        graphics.fill(PROMPT_WIDTH - 2, 1, PROMPT_WIDTH - 1, PROMPT_HEIGHT - 1,
                0x78000000 | CYAN);
        graphics.fill(7, 7, 9, PROMPT_HEIGHT - 7, 0xC0000000 | CYAN);

        Component text = Component.translatable("hud.starboundmc.boot.restarting");
        float scale = Math.min(1F, 198F / Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(PROMPT_WIDTH / 2F,
                (PROMPT_HEIGHT - font.lineHeight * scale) / 2F, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawCenteredString(font, text, 0, 0, 0xEC000000 | CYAN);
        graphics.pose().popPose();

        int scanY = 4 + Math.round((PROMPT_HEIGHT - 8F) * presentation.scanProgress());
        graphics.fill(3, scanY, PROMPT_WIDTH - 3, scanY + 1, 0x20000000 | CYAN);
    }

    private static void line(GuiGraphics graphics, Component text, float y, int rgb) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(1F, 180F / Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(7, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, 0xC8000000 | rgb, false);
        graphics.pose().popPose();
    }
}
