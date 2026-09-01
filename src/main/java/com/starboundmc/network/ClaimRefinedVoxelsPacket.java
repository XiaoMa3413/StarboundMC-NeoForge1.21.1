package com.starboundmc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request to claim the refinery's public voxel output. */
public record ClaimRefinedVoxelsPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ClaimRefinedVoxelsPacket> TYPE = PayloadSupport.type("claim_refined_voxels");
    public static final StreamCodec<FriendlyByteBuf, ClaimRefinedVoxelsPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ClaimRefinedVoxelsPacket::write, ClaimRefinedVoxelsPacket::new);

    private ClaimRefinedVoxelsPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public Type<ClaimRefinedVoxelsPacket> type() {
        return TYPE;
    }
}
