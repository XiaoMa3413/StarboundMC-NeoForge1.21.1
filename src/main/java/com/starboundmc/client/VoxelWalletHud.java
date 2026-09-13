package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

/**
 * Transparent voxel balance readout beside the player inventory. The
 * Foreground event fires with the pose already translated to the GUI origin,
 * so drawing uses container-local coordinates.
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class VoxelWalletHud {
    private static final float LABEL_SCALE = 0.65F;
    private static final float BALANCE_SCALE = 0.9F;
    private static final int SIDE_GAP = 8;
    private static final int ICON_SIZE = 16;
    private static final int TEXT_GAP = 4;
    private static final int BLOCK_HEIGHT = 20;
    private static final int LABEL_COLOR = 0xFF9EA9AC;
    private static final int BALANCE_COLOR = 0xFFFFE071;

    private VoxelWalletHud() {
    }

    @SubscribeEvent
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Font font = minecraft.font;
        Component label = Component.translatable("gui.starboundmc.voxel_wallet.label");
        Component balance = Component.literal(String.format("%,d", ClientVoxelWalletState.balance()));
        int blockWidth = walletWidth(font, label, balance);
        WalletPosition position = walletPosition(screen, blockWidth);
        if (position == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();

        graphics.pose().pushPose();
        graphics.pose().translate(position.x(), position.y(), 0.0F);
        graphics.renderItem(new ItemStack(ModItems.VOXEL.get()), 0, 2);

        int textX = ICON_SIZE + TEXT_GAP;
        graphics.pose().pushPose();
        graphics.pose().translate(textX, 1, 0.0F);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1.0F);
        graphics.drawString(font, label, 0, 0, LABEL_COLOR, true);
        graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(textX, 10, 0.0F);
        graphics.pose().scale(BALANCE_SCALE, BALANCE_SCALE, 1.0F);
        graphics.drawString(font, balance, 0, 0, BALANCE_COLOR, true);
        graphics.pose().popPose();
        graphics.pose().popPose();
    }

    private static int walletWidth(Font font, Component label, Component balance) {
        int labelWidth = Math.round(font.width(label) * LABEL_SCALE);
        int balanceWidth = Math.round(font.width(balance) * BALANCE_SCALE);
        return ICON_SIZE + TEXT_GAP + Math.max(labelWidth, balanceWidth);
    }

    private static WalletPosition walletPosition(InventoryScreen screen, int blockWidth) {
        int preferredY = screen.getYSize() - BLOCK_HEIGHT - 8;
        int right = screen.getGuiLeft() + screen.getXSize() + SIDE_GAP;
        if (right + blockWidth <= screen.width) {
            return new WalletPosition(screen.getXSize() + SIDE_GAP, preferredY);
        }

        int left = screen.getGuiLeft() - SIDE_GAP - blockWidth;
        if (left >= 0) {
            return new WalletPosition(-SIDE_GAP - blockWidth, preferredY);
        }

        int centeredX = (screen.getXSize() - blockWidth) / 2;
        int below = screen.getGuiTop() + screen.getYSize() + SIDE_GAP;
        if (below + BLOCK_HEIGHT <= screen.height) {
            return new WalletPosition(centeredX, screen.getYSize() + SIDE_GAP);
        }

        int above = screen.getGuiTop() - SIDE_GAP - BLOCK_HEIGHT;
        if (above >= 0) {
            return new WalletPosition(centeredX, -SIDE_GAP - BLOCK_HEIGHT);
        }

        // There is no safe external area at pathological resolutions. Hiding
        // is preferable to covering inventory slots or the carried stack.
        return null;
    }

    private record WalletPosition(int x, int y) {
    }
}
