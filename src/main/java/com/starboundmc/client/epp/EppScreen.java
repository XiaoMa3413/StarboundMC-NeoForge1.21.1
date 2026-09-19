// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import com.starboundmc.client.ui.StarboundModularScreen;
import com.starboundmc.epp.EppMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class EppScreen extends StarboundModularScreen<EppMenu, EquipmentConfigRoot> {
    public EppScreen(EppMenu menu, Inventory inv, Component title) { super(menu, inv, title); imageWidth = EppMenu.WIDTH; imageHeight = EppMenu.HEIGHT; }
    @Override protected EquipmentConfigRoot createRoot() { return new EquipmentConfigRoot(leftPos, topPos, menu); }
    @Override protected ResourceLocation stylesheet() { return ResourceLocation.fromNamespaceAndPath("starboundmc", "lss/equipment.lss"); }
    @Override public void containerTick() { super.containerTick(); if (root != null) root.refresh(); }
    @Override public void render(GuiGraphics g, int x, int y, float dt) { super.render(g, x, y, dt); renderTooltip(g, x, y); }
}
