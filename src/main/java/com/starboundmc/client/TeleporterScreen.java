package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.teleporter.TeleporterRoot;
import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.menu.TeleporterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** LDLib2-backed destination console for the unified teleporter block. */
public final class TeleporterScreen
        extends StarboundModularScreen<TeleporterMenu, TeleporterRoot> {
    public TeleporterScreen(TeleporterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected TeleporterRoot createRoot() {
        return new TeleporterRoot();
    }

    @Override
    protected ResourceLocation stylesheet() {
        return ResourceLocation.fromNamespaceAndPath(
                StarboundMC.MODID, "lss/machine_ui.lss");
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (root != null) {
            root.refreshIfDirty();
        }
    }
}
