package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.client.upgrade.UpgradeRoot;
import com.starboundmc.menu.UpgradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** LDLib2-backed Matter Manipulator workbench with vanilla-owned inventory slots. */
public final class UpgradeScreen extends StarboundModularScreen<UpgradeMenu, UpgradeRoot> {
    private final Inventory inventory;

    public UpgradeScreen(UpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.inventory = inventory;
        imageWidth = UpgradeRoot.PANEL_W;
        imageHeight = UpgradeRoot.PANEL_H;
    }

    @Override
    protected UpgradeRoot createRoot() {
        return new UpgradeRoot(leftPos, topPos, title, playerInventoryTitle, menu, inventory);
    }

    @Override
    protected ResourceLocation stylesheet() {
        return ResourceLocation.fromNamespaceAndPath(
                StarboundMC.MODID, "lss/upgrade_workbench.lss");
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (root != null) {
            root.refreshState();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
