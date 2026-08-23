package com.starboundmc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class SciFiButton extends Button
{
    private static final int LEGACY_FILL_DISABLED = 0xFF3A4048;
    private static final int LEGACY_FILL_HOVERED = 0xFF2E87A5;
    private static final int LEGACY_FILL_NORMAL = 0xFF24566E;
    private static final int LEGACY_BORDER = 0xFF6E8CA6;
    private static final int LEGACY_TEXT = 0xFFFFFFFF;

    private final StarmapHiDpiGraphics hiDpi;
    private final Style style;
    private boolean pressed;

    public SciFiButton(int x, int y, int width, int height, Component message, OnPress onPress)
    {
        this(x, y, width, height, message, onPress, null, Style.SECONDARY);
    }

    /** Enables virtual-pixel chrome while retaining the vanilla Button contract. */
    public SciFiButton(int x, int y, int width, int height, Component message,
                       OnPress onPress, StarmapHiDpiGraphics hiDpi)
    {
        this(x, y, width, height, message, onPress, hiDpi, Style.SECONDARY);
    }

    public SciFiButton(int x, int y, int width, int height, Component message,
                       OnPress onPress, StarmapHiDpiGraphics hiDpi, Style style)
    {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.hiDpi = hiDpi;
        this.style = style == null ? Style.SECONDARY : style;
    }

    @Override
    public void onClick(double mouseX, double mouseY)
    {
        this.pressed = true;
        super.onClick(mouseX, mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        this.pressed = false;
        super.onRelease(mouseX, mouseY);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        boolean starmapTheme = this.hiDpi != null;
        if (this.hiDpi == null)
        {
            int fillColor = !this.active ? LEGACY_FILL_DISABLED
                    : this.isHoveredOrFocused() ? LEGACY_FILL_HOVERED : LEGACY_FILL_NORMAL;
            this.drawLegacyChrome(graphics, fillColor, LEGACY_BORDER);
        }
        else
            this.drawHighDensityChrome(graphics);

        int textOffset = starmapTheme && this.active && this.pressed && this.isHovered
                ? StarmapVisualTheme.BUTTON_PRESS_OFFSET : 0;
        int textColor = !starmapTheme ? LEGACY_TEXT
                : this.active ? StarmapVisualTheme.TEXT_PRIMARY
                : StarmapVisualTheme.TEXT_DISABLED;
        graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2 + textOffset, textColor);
    }

    /** Preserves the shared button's existing appearance outside the star map. */
    private void drawLegacyChrome(GuiGraphics graphics, int fillColor, int borderColor)
    {
        int x = this.getX();
        int y = this.getY();
        graphics.fill(x, y, x + this.width, y + this.height, fillColor);
        graphics.fill(x, y, x + this.width, y + 1, borderColor);
        graphics.fill(x, y + this.height - 1, x + this.width, y + this.height, borderColor);
        graphics.fill(x, y, x + 1, y + this.height, borderColor);
        graphics.fill(x + this.width - 1, y, x + this.width, y + this.height, borderColor);
    }

    /** Pixel-mechanical key cap; legacy users of this class remain unchanged. */
    private void drawHighDensityChrome(GuiGraphics graphics)
    {
        if (!this.active)
            this.pressed = false;
        boolean hovered = this.active && this.isHovered;
        boolean focused = this.active && this.isFocused();
        boolean pressedNow = hovered && this.pressed;

        int fillColor;
        if (!this.active)
            fillColor = StarmapVisualTheme.BUTTON_FILL_DISABLED;
        else if (this.style == Style.PRIMARY)
            fillColor = hovered ? StarmapVisualTheme.BUTTON_PRIMARY_HOVERED
                    : focused ? StarmapVisualTheme.BUTTON_FILL_FOCUSED
                    : StarmapVisualTheme.BUTTON_PRIMARY_FILL;
        else
            fillColor = hovered ? StarmapVisualTheme.BUTTON_FILL_HOVERED
                    : focused ? StarmapVisualTheme.BUTTON_FILL_FOCUSED
                    : StarmapVisualTheme.BUTTON_FILL_NORMAL;

        int lightEdge = !this.active ? StarmapVisualTheme.SHELL_EDGE
                : focused ? StarmapVisualTheme.SELECTION
                : StarmapVisualTheme.SHELL_HIGHLIGHT;
        int statusColor = !this.active ? 0
                : focused || this.style == Style.PRIMARY
                        ? StarmapVisualTheme.ACCENT
                        : StarmapVisualTheme.BUTTON_STATUS_DIM;
        try (StarmapHiDpiGraphics.DrawScope draw = this.hiDpi.begin(graphics))
        {
            StarmapTerminalPrimitives.drawMechanicalButton(draw,
                    this.getX(), this.getY(), this.width, this.height,
                    fillColor, lightEdge, StarmapVisualTheme.FRAME_INNER,
                    statusColor, pressedNow);
        }
    }

    public enum Style
    {
        SECONDARY,
        PRIMARY
    }
}
