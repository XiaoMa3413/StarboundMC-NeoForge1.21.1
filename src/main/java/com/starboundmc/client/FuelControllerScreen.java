package com.starboundmc.client;

import com.starboundmc.menu.FuelControllerMenu;
import com.starboundmc.network.AddFuelPacket;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.warp.ShipWarpManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Fuel console — workbench-style industrial panel (176x220): a steel
 * "console" plate holding the fuel gauge with its numeric readout, five fuel
 * sockets and the refuel button, plus a warp-cost reminder; an amber
 * separator leads into the player inventory. Fuel data comes from
 * ClientPlanetState (synced by SyncFuelPacket whenever the tank changes).
 */
public class FuelControllerScreen extends AbstractContainerScreen<FuelControllerMenu>
{
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 220;

    // Menu slots: fuel row at y=56, player rows 136..168, hotbar 190.
    private static final int GAUGE_X = 28;
    private static final int GAUGE_Y = 38;
    private static final int GAUGE_W = 120;
    private static final int GAUGE_H = 12;

    private PixelButton addButton;

    public FuelControllerScreen(FuelControllerMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void init()
    {
        super.init();
        this.addButton = new PixelButton(this.leftPos + 43, this.topPos + 78, 90, 16,
                Component.translatable("gui.starboundmc.fuel_controller.add"),
                b -> ModNetwork.sendToServer(new AddFuelPacket()));
        this.addRenderableWidget(this.addButton);
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

        // Console plate: gauge + readout area, fuel sockets, refuel button,
        // warp-cost reminder (slots sit at menu row y=56).
        UiStyle.drawPlate(graphics, x + 3, y + 24, 170, 92);

        int maxFuel = Math.max(1, ClientPlanetState.getMaxFuel());
        int fuel = Math.max(0, Math.min(maxFuel, ClientPlanetState.getFuel()));
        UiStyle.drawHBar(graphics, x + GAUGE_X, y + GAUGE_Y, GAUGE_W, GAUGE_H, fuel, maxFuel);

        for (int col = 0; col < FuelControllerMenu.FUEL_SLOTS; col++)
        {
            UiStyle.drawSlot(graphics, x + 42 + col * 18, y + 55);
        }

        UiStyle.drawSeparator(graphics, x + 4, y + 118, 168);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        // NOTE: AbstractContainerScreen translates the pose by (leftPos, topPos)
        // before calling renderLabels, so ALL coordinates here are panel-relative.
        UiStyle.drawHeader(graphics, this.font, 0, 0, this.title, PANEL_W);

        // Numeric readout above the gauge.
        Component fuelText = Component.translatable("gui.starboundmc.fuel",
                ClientPlanetState.getFuel(), ClientPlanetState.getMaxFuel());
        graphics.drawString(this.font, fuelText, (PANEL_W - this.font.width(fuelText)) / 2, 27, UiStyle.C_ACCENT_LIGHT, true);

        // Warp cost reminder (in-system vs cross-system).
        Component local = Component.translatable("gui.starboundmc.starmap.fuel_cost_local",
                ShipWarpManager.WARP_FUEL_COST);
        Component cross = Component.translatable("gui.starboundmc.starmap.fuel_cost_cross",
                ShipWarpManager.CROSS_SYSTEM_FUEL_COST);
        graphics.drawString(this.font, local, (PANEL_W - this.font.width(local)) / 2, 98, UiStyle.C_DIM, false);
        graphics.drawString(this.font, cross, (PANEL_W - this.font.width(cross)) / 2, 106, UiStyle.C_DIM, false);

        graphics.drawString(this.font, this.playerInventoryTitle, 8, 125, UiStyle.C_DIM, true);
    }
}
