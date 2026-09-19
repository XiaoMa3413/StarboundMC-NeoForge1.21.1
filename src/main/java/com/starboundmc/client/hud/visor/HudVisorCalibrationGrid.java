// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

/**
 * Development-only overlay enabled with {@code -Dstarboundmc.debug.hudVisorGrid=true}.
 * It draws direct low-opacity lines, so enabling it never allocates a full-screen framebuffer.
 */
public final class HudVisorCalibrationGrid {
    private static final boolean ENABLED = Boolean.getBoolean("starboundmc.debug.hudVisorGrid");
    private static final int RGB = 0x95E8E2;

    private HudVisorCalibrationGrid() { }

    public static boolean enabled() { return ENABLED; }

    public static void render(GuiGraphics graphics) {
        if (!ENABLED)
            return;
        for (int line = -8; line <= 8; line += 2) {
            float coordinate = line / 10F;
            segment(graphics, coordinate, -.82F, coordinate, .82F);
            segment(graphics, -.92F, coordinate, .92F, coordinate);
        }
        segment(graphics, -1F, 0F, 1F, 0F);
        segment(graphics, 0F, -1F, 0F, 1F);
    }

    private static void segment(GuiGraphics graphics, float fromX, float fromY,
                                float toX, float toY) {
        float width = graphics.guiWidth();
        float height = graphics.guiHeight();
        float rawFromX = (fromX + 1F) * width * .5F;
        float rawFromY = (fromY + 1F) * height * .5F;
        float rawToX = (toX + 1F) * width * .5F;
        float rawToY = (toY + 1F) * height * .5F;
        var from = HudVisorSurface.projectGui(rawFromX, rawFromY, width, height);
        var to = HudVisorSurface.projectGui(rawToX, rawToY, width, height);
        float dx = to.x() - from.x();
        float dy = to.y() - from.y();
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < .001F)
            return;
        float nx = -dy / length * .3F;
        float ny = dx / length * .3F;
        float fade = Math.min(HudVisorSurface.edgeFade(fromX, fromY),
                HudVisorSurface.edgeFade(toX, toY));
        int alpha = Math.round(38 * fade);
        int color = alpha << 24 | RGB;
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        var pose = graphics.pose().last().pose();
        buffer.addVertex(pose, from.x() + nx, from.y() + ny, 0).setColor(color);
        buffer.addVertex(pose, to.x() + nx, to.y() + ny, 0).setColor(color);
        buffer.addVertex(pose, to.x() - nx, to.y() - ny, 0).setColor(color);
        buffer.addVertex(pose, from.x() - nx, from.y() - ny, 0).setColor(color);
    }
}
