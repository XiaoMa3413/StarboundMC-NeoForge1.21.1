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
    private static final int PANEL_W = 440;
    private static final int COMPACT_W = 320;
    private static final int PANEL_H = 240;
    private final PrintSubmissionState submission = new PrintSubmissionState();

    public VoxelPrintingStationScreen(
            VoxelPrintingStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_W;
        imageHeight = PANEL_H;
    }

    @Override
    protected void init() {
        imageWidth = width < PANEL_W ? COMPACT_W : PANEL_W;
        super.init();
    }

    @Override
    protected VoxelPrintingStationRoot createRoot() {
        return new VoxelPrintingStationRoot(menu, leftPos, topPos, title, playerInventoryTitle,
                submission, imageWidth < PANEL_W);
    }

    public void acceptSubmission(boolean accepted) {
        submission.complete(accepted, net.minecraft.Util.getMillis());
        if (root != null) root.refresh();
    }

    @Override
    protected ResourceLocation stylesheet() {
        return ResourceLocation.fromNamespaceAndPath(
                StarboundMC.MODID, "lss/voxel_printing_station.lss");
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
