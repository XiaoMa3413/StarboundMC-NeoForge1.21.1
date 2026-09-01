package com.starboundmc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request to start refining the input in the refinery at the given position. */
public record StartRefinementPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<StartRefinementPacket> TYPE = PayloadSupport.type("start_refinement");
    public static final StreamCodec<FriendlyByteBuf, StartRefinementPacket> STREAM_CODEC =
            CustomPacketPayload.codec(StartRefinementPacket::write, StartRefinementPacket::new);

    private StartRefinementPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public Type<StartRefinementPacket> type() {
        return TYPE;
    }
}
