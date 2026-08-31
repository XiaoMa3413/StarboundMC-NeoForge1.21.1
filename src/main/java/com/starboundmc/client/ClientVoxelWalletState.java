package com.starboundmc.client;

/** Client-side mirror of the server-authoritative voxel wallet balance. */
public final class ClientVoxelWalletState {
    private static volatile int balance;

    private ClientVoxelWalletState() {
    }

    public static int balance() {
        return balance;
    }

    public static void set(int value) {
        balance = Math.max(0, value);
    }

    public static void reset() {
        balance = 0;
    }
}
