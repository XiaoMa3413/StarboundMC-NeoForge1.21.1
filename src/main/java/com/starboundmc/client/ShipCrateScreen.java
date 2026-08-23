package com.starboundmc.client;

import com.starboundmc.menu.ShipCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ship storage crate — industrial-sci-fi panel in the workbench style:
 *  dark steel gradient, 2px pixel frame, amber corner brackets, "PWR"
 *  indicator, a steel cargo plate with recessed 6x9 sockets, and an amber
 *  separator above the player inventory. */
public class ShipCrateScreen extends AbstractContainerScreen<ShipCrateMenu>
{
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 247;

    // Menu slot grid: 6x9 cargo at rows 34..124, 3x9 player at 164..200,
    // hotbar at 218 (see ShipCrateMenu).
    private static final int CARGO_Y0 = 34;
    private static final int CARGO_ROWS = 6;

    public ShipCrateScreen(ShipCrateMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;

        UiStyle.drawPanel(graphics, x, y, PANEL_W, PANEL_H);

        // Cargo bay plate + recessed sockets for the 6x9 storage grid.
        UiStyle.drawPlate(graphics, x + 3, y + 31, 170, 112);
        for (int row = 0; row < CARGO_ROWS; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                UiStyle.drawSlot(graphics, x + 7 + col * 18, y + CARGO_Y0 - 1 + row * 18);
            }
        }

        UiStyle.drawSeparator(graphics, x + 4, y + 147, 168);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        // NOTE: AbstractContainerScreen translates the pose by (leftPos, topPos)
        // before calling renderLabels, so ALL coordinates here are panel-relative.
        UiStyle.drawHeader(graphics, this.font, 0, 0, this.title, PANEL_W);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, 153, UiStyle.C_DIM, true);
    }
}
