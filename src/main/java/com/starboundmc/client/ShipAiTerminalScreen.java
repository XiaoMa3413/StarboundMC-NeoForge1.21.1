package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.shipai.ShipAiTerminalRoot;
import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.menu.ShipAiTerminalMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** LDLib2-backed prototype screen for the shipboard N.O.V.A. terminal. */
public final class ShipAiTerminalScreen
        extends StarboundModularScreen<ShipAiTerminalMenu, ShipAiTerminalRoot> {
    public ShipAiTerminalScreen(ShipAiTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected ShipAiTerminalRoot createRoot() {
        return new ShipAiTerminalRoot();
    }

    @Override
    protected ResourceLocation stylesheet() {
        return ResourceLocation.fromNamespaceAndPath(
                StarboundMC.MODID, "lss/ship_ai_terminal.lss");
    }
}
