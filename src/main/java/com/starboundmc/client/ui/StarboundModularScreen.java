package com.starboundmc.client.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Shared lifecycle bridge for StarboundMC screens rendered by LDLib2.
 *
 * <p>The root element owns the whole visual surface. Vanilla rendering is kept
 * empty here so individual screens do not accidentally layer the old pixel GUI
 * underneath their LDLib2 component tree.</p>
 */
public abstract class StarboundModularScreen<M extends AbstractContainerMenu, R extends UIElement>
        extends AbstractContainerScreen<M> {
    private final Inventory playerInventory;
    protected ModularUI modularUI;
    protected R root;

    protected StarboundModularScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.playerInventory = inventory;
        imageWidth = 0;
        imageHeight = 0;
    }

    protected abstract R createRoot();

    protected abstract ResourceLocation stylesheet();

    @Override
    protected void init() {
        disposeModularUi();
        super.init();

        root = createRoot();
        modularUI = ModularUI.of(UI.of(root, stylesheet()), playerInventory.player);
        modularUI.setMenu(menu);
        modularUI.setScreenAndInit(this);
        addRenderableWidget(modularUI.getWidget());
        setFocused(modularUI.getWidget());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (modularUI != null) {
            modularUI.tick();
        }
    }

    @Override
    public void removed() {
        disposeModularUi();
        super.removed();
    }

    private void disposeModularUi() {
        if (modularUI != null) {
            modularUI.onRemoved();
            modularUI.setScreen(null);
            modularUI = null;
        }
        root = null;
    }
}
