package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client voxel wallet balance snapshot. */
public record SyncVoxelWalletPacket(int balance) implements CustomPacketPayload {
    public static final Type<SyncVoxelWalletPacket> TYPE = PayloadSupport.type("sync_voxel_wallet");
    public static final StreamCodec<FriendlyByteBuf, SyncVoxelWalletPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncVoxelWalletPacket::write, SyncVoxelWalletPacket::new);

    public SyncVoxelWalletPacket {
        if (balance < 0) {
            throw new IllegalArgumentException("Negative voxel wallet balance " + balance);
        }
    }

    private SyncVoxelWalletPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(balance);
    }

    @Override
    public Type<SyncVoxelWalletPacket> type() {
        return TYPE;
    }
}
