package com.starboundmc.client;

import com.starboundmc.menu.ShipConsoleMenu;
import com.starboundmc.menu.ShipCrateMenu;
import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.menu.UpgradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Minimal client shell used until each gameplay screen reaches its migration stage. */
public class Stage2MenuScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    public Stage2MenuScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF18222D);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF5D7185);
    }
}

final class Stage2UpgradeScreen extends Stage2MenuScreen<UpgradeMenu> {
    Stage2UpgradeScreen(UpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}

final class Stage2ShipConsoleScreen extends Stage2MenuScreen<ShipConsoleMenu> {
    Stage2ShipConsoleScreen(ShipConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}

final class Stage2ShipCrateScreen extends Stage2MenuScreen<ShipCrateMenu> {
    Stage2ShipCrateScreen(ShipCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}

final class Stage2TeleporterScreen extends Stage2MenuScreen<TeleporterMenu> {
    Stage2TeleporterScreen(TeleporterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
