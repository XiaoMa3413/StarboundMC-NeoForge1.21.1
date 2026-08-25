package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.starboundmc.menu.StarmapTerminalMenu;
import com.starboundmc.StarboundMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/** LDLib2 screen for the standalone starmap terminal. */
public final class StarmapTerminalScreen extends AbstractContainerScreen<StarmapTerminalMenu> {
    private ModularUI modularUI;
    private StarmapTerminalRoot root;

    public StarmapTerminalScreen(StarmapTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 0;
        imageHeight = 0;
    }

    @Override
    protected void init() {
        disposeModularUi();
        super.init();
        root = new StarmapTerminalRoot();
        modularUI = ModularUI.of(UI.of(root, ResourceLocation.fromNamespaceAndPath(
                        StarboundMC.MODID, "lss/starmap_redraw.lss")),
                Minecraft.getInstance().player);
        modularUI.setMenu(menu);
        modularUI.setScreenAndInit(this);
        addRenderableWidget(modularUI.getWidget());
        setFocused(modularUI.getWidget());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (root != null)
            root.prepareFrame(partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        boolean mapHandled = root != null && root.dragView((float) mouseX, (float) mouseY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY) || mapHandled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        if (root != null)
            root.finishViewDrag();
        return handled;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xFF050912);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (modularUI != null)
            modularUI.tick();
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
