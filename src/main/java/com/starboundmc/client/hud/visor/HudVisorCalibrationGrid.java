// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import com.starboundmc.client.hud.visor.HudVisorGeometry.Profile;

/**
 * Development-only overlay enabled with {@code -Dstarboundmc.debug.hudVisorGrid=true}.
 * Component grids follow the actual projection; two full-width guides reveal the shared reference curves.
 */
public final class HudVisorCalibrationGrid {
    private static final boolean ENABLED = Boolean.getBoolean("starboundmc.debug.hudVisorGrid");
    private static final int RGB = 0x95E8E2;

    private HudVisorCalibrationGrid() { }

    public static boolean enabled() { return ENABLED; }

    public static void renderGuides(GuiGraphics graphics, float upperY, float lowerY,
                                    Profile upper, Profile lower) {
        if (!ENABLED) return;
        guide(graphics, upperY, upper);
        guide(graphics, lowerY, lower);
    }

    private static void guide(GuiGraphics graphics, float baseline, Profile profile) {
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        var pose = graphics.pose().last().pose();
        for (int x = 0; x < graphics.guiWidth(); x += 4) {
            float nextX = Math.min(x + 4, graphics.guiWidth());
            float y = baseline + HudVisorGeometry.curveOffset(profile, x, graphics.guiWidth(), graphics.guiHeight());
            float nextY = baseline + HudVisorGeometry.curveOffset(profile, nextX, graphics.guiWidth(), graphics.guiHeight());
            buffer.addVertex(pose, x, y - .25F, 0).setColor(0x4095E8E2);
            buffer.addVertex(pose, x, y + .25F, 0).setColor(0x4095E8E2);
            buffer.addVertex(pose, nextX, nextY + .25F, 0).setColor(0x4095E8E2);
            buffer.addVertex(pose, nextX, nextY - .25F, 0).setColor(0x4095E8E2);
        }
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        if (!ENABLED)
            return;
        int color = 38 << 24 | RGB;
        for (int x = 0; x < width; x += 16)
            graphics.fill(x, 0, x + 1, height, color);
        for (int y = 0; y < height; y += 8)
            graphics.fill(0, y, width, y + 1, color);
        graphics.fill(width - 1, 0, width, height, color);
        graphics.fill(0, height - 1, width, height, color);
        graphics.fill(width / 2 - 3, height / 2, width / 2 + 4, height / 2 + 1, 0x8095E8E2);
        graphics.fill(width / 2, height / 2 - 3, width / 2 + 1, height / 2 + 4, 0x8095E8E2);
    }
}
