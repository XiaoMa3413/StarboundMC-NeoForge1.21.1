package com.starboundmc.client.starmap;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** Shared sub-pixel vector primitives used by scene and selection layers. */
final class StarmapVectorDrawing {
    private StarmapVectorDrawing() {}

    static void drawOrbit(GuiGraphics graphics, float centerX, float centerY,
                          float radius, int color) {
        drawOrbit(graphics, centerX, centerY, radius, color, false);
    }

    static void drawOrbit(GuiGraphics graphics, float centerX, float centerY,
                          float radius, int color, boolean dashed) {
        int steps = Math.max(128, (int) Math.ceil(radius * Math.PI * 2.4D));
        float circumference = Math.max(1.0F, (float) (Math.PI * 2.0D * radius));
        int dashSpan = Math.max(2, Math.round(steps * 7.0F / circumference));
        int gapSpan = Math.max(2, Math.round(steps * 11.0F / circumference));
        int dashPeriod = dashSpan + gapSpan;
        int glow = multiplyAlpha(color, dashed ? 0.22F : 0.30F);
        int core = multiplyAlpha(color, dashed ? 0.82F : 0.94F);
        for (int pass = 0; pass < 2; pass++) {
            int passColor = pass == 0 ? glow : core;
            float width = pass == 0 ? (dashed ? 2.8F : 3.4F) : (dashed ? 0.85F : 1.05F);
            for (int i = 0; i < steps; i++) {
                if (dashed && (i % dashPeriod) >= dashSpan)
                    continue;
                double a = Math.PI * 2.0D * i / steps;
                double b = Math.PI * 2.0D * (i + 1) / steps;
                drawSmoothSegment(graphics,
                        centerX + (float) Math.cos(a) * radius,
                        centerY + (float) Math.sin(a) * radius,
                        centerX + (float) Math.cos(b) * radius,
                        centerY + (float) Math.sin(b) * radius,
                        width, passColor);
            }
        }
    }

    static void drawDashedLine(GuiGraphics graphics, float x1, float y1,
                               float x2, float y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.01F)
            return;
        float dash = 6.0F;
        float gap = 8.0F;
        int glow = multiplyAlpha(color, 0.28F);
        int core = multiplyAlpha(color, 0.92F);
        for (int pass = 0; pass < 2; pass++) {
            int passColor = pass == 0 ? glow : core;
            float width = pass == 0 ? 3.2F : 1.05F;
            for (float offset = 0.0F; offset < length; offset += dash + gap) {
                float start = offset / length;
                float end = Math.min(length, offset + dash) / length;
                drawSmoothSegment(graphics,
                        x1 + dx * start, y1 + dy * start,
                        x1 + dx * end, y1 + dy * end,
                        width, passColor);
            }
        }
    }

    static void drawSelectionBrackets(GuiGraphics graphics, float centerX, float centerY,
                                      float bodySize, float alpha) {
        float pad = Math.max(4.0F, bodySize * 0.22F);
        float half = bodySize * 0.5F + pad;
        float left = centerX - half;
        float top = centerY - half;
        float right = centerX + half;
        float bottom = centerY + half;
        float corner = Math.max(5.0F, bodySize * 0.30F);
        float thickness = Math.max(1.0F, Math.min(1.65F, bodySize * 0.08F));
        int core = multiplyAlpha(0xFF63E2DF, alpha);
        int glow = multiplyAlpha(0x4A63E2DF, alpha);
        drawCorner(graphics, left, top, corner, thickness, true, true, glow, core);
        drawCorner(graphics, right, top, corner, thickness, false, true, glow, core);
        drawCorner(graphics, left, bottom, corner, thickness, true, false, glow, core);
        drawCorner(graphics, right, bottom, corner, thickness, false, false, glow, core);
    }

    private static void drawCorner(GuiGraphics graphics, float x, float y, float length,
                                   float thickness, boolean left, boolean top,
                                   int glow, int core) {
        float horizontal = left ? x + length : x - length;
        float vertical = top ? y + length : y - length;
        drawSmoothSegment(graphics, x, y, horizontal, y, thickness * 2.8F, glow);
        drawSmoothSegment(graphics, x, y, x, vertical, thickness * 2.8F, glow);
        drawSmoothSegment(graphics, x, y, horizontal, y, thickness, core);
        drawSmoothSegment(graphics, x, y, x, vertical, thickness, core);
    }

    private static void drawSmoothSegment(GuiGraphics graphics, float x1, float y1,
                                          float x2, float y2, float width, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001F)
            return;
        float half = width * 0.5F;
        float nx = -dy / length * half;
        float ny = dx / length * half;
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(matrix, x1 + nx, y1 + ny, 0).setColor(color);
        buffer.addVertex(matrix, x2 + nx, y2 + ny, 0).setColor(color);
        buffer.addVertex(matrix, x2 - nx, y2 - ny, 0).setColor(color);
        buffer.addVertex(matrix, x1 - nx, y1 - ny, 0).setColor(color);
    }

    private static int multiplyAlpha(int color, float factor) {
        int alpha = (color >>> 24) & 0xFF;
        int scaled = Math.max(0, Math.min(255, Math.round(alpha * factor)));
        return (scaled << 24) | (color & 0x00FFFFFF);
    }
}
