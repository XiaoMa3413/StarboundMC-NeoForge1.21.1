package com.starboundmc.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable per-player voxel wallet balance stored as a data attachment. */
public record VoxelWalletState(int balance) {
    public static final VoxelWalletState DEFAULT = new VoxelWalletState(0);

    public static final Codec<VoxelWalletState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("balance", 0).forGetter(VoxelWalletState::balance)
    ).apply(instance, VoxelWalletState::new));

    public VoxelWalletState {
        balance = Math.max(0, balance);
    }

    public VoxelWalletState add(int amount) {
        if (amount <= 0) {
            return this;
        }
        long merged = (long) balance + amount;
        return new VoxelWalletState((int) Math.min(merged, Integer.MAX_VALUE));
    }

    public boolean canAfford(int amount) {
        return amount >= 0 && balance >= amount;
    }

    /** Returns the new state, or this one when the balance is insufficient. */
    public VoxelWalletState spend(int amount) {
        if (amount <= 0 || balance < amount) {
            return this;
        }
        return new VoxelWalletState(balance - amount);
    }
}
