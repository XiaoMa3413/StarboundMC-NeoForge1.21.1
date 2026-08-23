package com.starboundmc.client;

import com.starboundmc.menu.AlloyFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Alloy furnace — workbench-style industrial panel. The machine area sits on
 *  a steel plate holding the input / fuel / output sockets, with an amber fuel
 *  gauge and a cyan smelting progress channel; an amber separator leads into
 *  the player inventory. */
public class AlloyFurnaceScreen extends AbstractContainerScreen<AlloyFurnaceMenu>
{
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 176;

    // Menu slots: input (56,17), fuel (56,53), output (116,35); player rows
    // 96..132, hotbar 150 (see AlloyFurnaceMenu).
    private static final int FUEL_X = 56;
    private static final int FUEL_Y = 36;
    private static final int PROG_X = 79;
    private static final int PROG_Y = 35;

    public AlloyFurnaceScreen(AlloyFurnaceMenu menu, Inventory playerInventory, Component title)
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

        // Machine plate; the input socket's top edge meets the plate's top rule.
        UiStyle.drawPlate(graphics, x + 3, y + 16, 170, 61);
        UiStyle.drawSlot(graphics, x + 55, y + 16);
        UiStyle.drawSlot(graphics, x + 55, y + 52);
        UiStyle.drawSlot(graphics, x + 115, y + 34);
        UiStyle.drawFuelBar(graphics, x + FUEL_X, y + FUEL_Y,
                this.menu.getLitTime(), this.menu.getLitDuration());
        UiStyle.drawProgressBar(graphics, x + PROG_X, y + PROG_Y,
                this.menu.getCookingProgress(), this.menu.getCookingTotalTime());

        UiStyle.drawSeparator(graphics, x + 4, y + 80, 168);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        // NOTE: AbstractContainerScreen translates the pose by (leftPos, topPos)
        // before calling renderLabels, so ALL coordinates here are panel-relative.
        UiStyle.drawHeader(graphics, this.font, 0, 0, this.title, PANEL_W);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, 84, UiStyle.C_DIM, true);
    }
}
