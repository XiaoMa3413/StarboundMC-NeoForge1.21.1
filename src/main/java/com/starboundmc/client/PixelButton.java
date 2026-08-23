package com.starboundmc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Industrial-sci-fi pixel button used by the upgrade workbench: chunky 2px
 * frame, steel plate fill, cyan glow on hover, dark when disabled.
 */
public class PixelButton extends Button
{
    public PixelButton(int x, int y, int width, int height, Component message, OnPress onPress)
    {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int fill;
        int border;
        if (!this.active)
        {
            fill = 0xFF171B21;
            border = 0xFF2A313B;
        }
        else if (this.isHoveredOrFocused())
        {
            fill = 0xFF23606E;
            border = 0xFF3FD0E8;
        }
        else
        {
            fill = 0xFF242B34;
            border = 0xFF4A5560;
        }

        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, fill);
        // Chunky 2px pixel border.
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 2, border);
        graphics.fill(this.getX(), this.getY() + this.height - 2, this.getX() + this.width, this.getY() + this.height, border);
        graphics.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.height, border);
        graphics.fill(this.getX() + this.width - 2, this.getY(), this.getX() + this.width, this.getY() + this.height, border);

        graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2,
                this.active ? 0xFFFFFFFF : 0xFF6E7680);
    }
}
