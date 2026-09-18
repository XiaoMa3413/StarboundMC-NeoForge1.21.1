// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.epp.EppServiceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class EppServiceScreen extends StarboundModularScreen<EppServiceMenu, EppServiceRoot> {
    public EppServiceScreen(EppServiceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = EppServiceMenu.WIDTH; imageHeight = EppServiceMenu.HEIGHT;
    }
    @Override protected EppServiceRoot createRoot() { return new EppServiceRoot(leftPos, topPos, menu); }
    @Override protected ResourceLocation stylesheet() { return ResourceLocation.fromNamespaceAndPath("starboundmc", "lss/epp.lss"); }
    @Override public void containerTick() { super.containerTick(); if (root != null) root.refresh(); }
    @Override public void render(GuiGraphics graphics, int x, int y, float partialTick) {
        super.render(graphics, x, y, partialTick); renderTooltip(graphics, x, y);
    }
}
