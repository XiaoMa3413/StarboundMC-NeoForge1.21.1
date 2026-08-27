package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.fuel.FuelControllerRoot;
import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.menu.FuelControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** LDLib2-backed fuel console that preserves the existing vanilla slot interaction path. */
public final class FuelControllerScreen
        extends StarboundModularScreen<FuelControllerMenu, FuelControllerRoot> {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 220;

    public FuelControllerScreen(FuelControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_W;
        imageHeight = PANEL_H;
    }

    @Override
    protected FuelControllerRoot createRoot() {
        return new FuelControllerRoot(leftPos, topPos, title, playerInventoryTitle);
    }

    @Override
    protected ResourceLocation stylesheet() {
        return ResourceLocation.fromNamespaceAndPath(
                StarboundMC.MODID, "lss/ship_machine_inventory.lss");
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (root != null) {
            root.refreshFuel();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
