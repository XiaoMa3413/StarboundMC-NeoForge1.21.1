package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.ui.AlloyFurnaceRoot;
import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.menu.AlloyFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Graphite furnace controls; the existing menu still owns all 39 slots. */
public final class AlloyFurnaceScreen extends StarboundModularScreen<AlloyFurnaceMenu, AlloyFurnaceRoot> {
    public AlloyFurnaceScreen(AlloyFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 176;
    }

    @Override
    protected AlloyFurnaceRoot createRoot() {
        return new AlloyFurnaceRoot(menu, leftPos, topPos, title, playerInventoryTitle);
    }

    @Override
    protected ResourceLocation stylesheet() {
        return ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "lss/ship_machine_inventory.lss");
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (root != null) root.refresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
