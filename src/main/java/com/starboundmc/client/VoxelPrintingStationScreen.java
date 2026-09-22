package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.client.voxel.VoxelPrintingStationRoot;
import com.starboundmc.menu.VoxelPrintingStationMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/** LDLib2 printing screen with a recipe list and selected-item details. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class VoxelPrintingStationScreen
        extends StarboundModularScreen<VoxelPrintingStationMenu, VoxelPrintingStationRoot> {
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 240;
    private final PrintSubmissionState submission = new PrintSubmissionState();

    /** Apply the console palette only while this screen owns the tooltip. */
    @SubscribeEvent
    public static void tooltipColors(RenderTooltipEvent.Color event) {
        if (Minecraft.getInstance().screen instanceof VoxelPrintingStationScreen) {
            event.setBackground(0xF8101A22);
            event.setBorderStart(0xFF526975);
            event.setBorderEnd(0xFF344B55);
        }
    }

    public VoxelPrintingStationScreen(
            VoxelPrintingStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_W;
        imageHeight = PANEL_H;
    }

    @Override
    protected void init() {
        // One panel size: the inventory the menu fixes needs 320px, and a wider panel would only add
        // room the detail view cannot use.
        imageWidth = PANEL_W;
        super.init();
    }

    @Override
    protected VoxelPrintingStationRoot createRoot() {
        return new VoxelPrintingStationRoot(menu, leftPos, topPos, title, playerInventoryTitle,
                submission);
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
