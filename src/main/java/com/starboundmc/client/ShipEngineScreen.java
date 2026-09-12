package com.starboundmc.client;

import com.starboundmc.client.engine.ShipEngineRoot;
import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.menu.ShipEngineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ShipEngineScreen extends StarboundModularScreen<ShipEngineMenu, ShipEngineRoot> {
    public ShipEngineScreen(ShipEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ShipEngineMenu.WIDTH;
        imageHeight = ShipEngineMenu.HEIGHT;
    }
    @Override protected ShipEngineRoot createRoot() {
        return new ShipEngineRoot(leftPos, topPos, menu, title, playerInventoryTitle);
    }
    @Override protected ResourceLocation stylesheet() {
        return ResourceLocation.fromNamespaceAndPath("starboundmc", "lss/ship_engine.lss");
    }
    @Override public void containerTick() {
        super.containerTick();
        if (root != null) root.refresh();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
