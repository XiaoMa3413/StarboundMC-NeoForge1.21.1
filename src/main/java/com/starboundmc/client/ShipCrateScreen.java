package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.storage.ShipCrateRoot;
import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.menu.ShipCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** LDLib2-backed cargo locker that preserves the existing vanilla slot interaction path. */
public final class ShipCrateScreen
        extends StarboundModularScreen<ShipCrateMenu, ShipCrateRoot> {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 247;

    public ShipCrateScreen(ShipCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_W;
        imageHeight = PANEL_H;
    }

    @Override
    protected ShipCrateRoot createRoot() {
        return new ShipCrateRoot(leftPos, topPos, title, playerInventoryTitle);
    }

    @Override
    protected ResourceLocation stylesheet() {
        return ResourceLocation.fromNamespaceAndPath(
                StarboundMC.MODID, "lss/ship_machine_inventory.lss");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
