package com.starboundmc.economy;

import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncVoxelWalletPacket;
import com.starboundmc.story.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative voxel wallet mutations with client synchronization. */
public final class VoxelWalletService {
    private VoxelWalletService() {
    }

    public static int balanceOf(ServerPlayer player) {
        return player.getData(ModAttachments.VOXEL_WALLET).balance();
    }

    public static void add(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        VoxelWalletState updated = player.getData(ModAttachments.VOXEL_WALLET).add(amount);
        player.setData(ModAttachments.VOXEL_WALLET, updated);
        sync(player);
    }

    /** Returns true and deducts when affordable; returns false without changes. */
    public static boolean trySpend(ServerPlayer player, int amount) {
        VoxelWalletState current = player.getData(ModAttachments.VOXEL_WALLET);
        VoxelWalletState updated = current.spend(amount);
        if (updated == current) {
            return false;
        }
        player.setData(ModAttachments.VOXEL_WALLET, updated);
        sync(player);
        return true;
    }

    public static void sync(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new SyncVoxelWalletPacket(balanceOf(player)));
    }
}
