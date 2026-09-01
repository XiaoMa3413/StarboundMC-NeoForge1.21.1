package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.client.voxel.VoxelPrintingStationRoot;
import com.starboundmc.menu.VoxelPrintingStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** LDLib2 printing screen with a recipe list and selected-item details. */
public final class VoxelPrintingStationScreen
        extends StarboundModularScreen<VoxelPrintingStationMenu, VoxelPrintingStationRoot> {
    private static final int PANEL_W = 364;
    private static final int PANEL_H = 234;

    public VoxelPrintingStationScreen(
            VoxelPrintingStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_W;
        imageHeight = PANEL_H;
    }

    @Override
    protected VoxelPrintingStationRoot createRoot() {
        return new VoxelPrintingStationRoot(menu, leftPos, topPos, title, playerInventoryTitle);
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
            root.refresh();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
