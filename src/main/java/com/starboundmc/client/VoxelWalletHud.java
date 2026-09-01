package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

/**
 * Small voxel balance readout in the bottom-right corner of the player
 * inventory screen. The Foreground event fires with the pose already
 * translated to the GUI origin, so drawing uses container-local coordinates.
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class VoxelWalletHud {
    private static final float TEXT_SCALE = 0.75F;
    private static final int MARGIN = 5;
    private static final int COLOR = 0xFFE066;

    private VoxelWalletHud() {
    }

    @SubscribeEvent
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Font font = minecraft.font;
        Component text = Component.translatable("gui.starboundmc.voxel_wallet",
                String.format("%,d", ClientVoxelWalletState.balance()));
        GuiGraphics graphics = event.getGuiGraphics();
        int textWidth = font.width(text);
        int x = event.getContainerScreen().getXSize() - MARGIN;
        int y = event.getContainerScreen().getYSize() - MARGIN;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        graphics.drawString(font, text, -(int) (textWidth * TEXT_SCALE), -(int) (font.lineHeight * TEXT_SCALE), COLOR, true);
        graphics.pose().popPose();
    }
}
