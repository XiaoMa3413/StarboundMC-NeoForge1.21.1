package com.starboundmc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request to stop the unfinished refinery cycle at the given position. */
public record StopRefinementPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<StopRefinementPacket> TYPE = PayloadSupport.type("stop_refinement");
    public static final StreamCodec<FriendlyByteBuf, StopRefinementPacket> STREAM_CODEC =
            CustomPacketPayload.codec(StopRefinementPacket::write, StopRefinementPacket::new);

    private StopRefinementPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public Type<StopRefinementPacket> type() {
        return TYPE;
    }
}
