package com.starboundmc.network;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request to cancel one queued (never active) printing entry. */
public record CancelPrintQueuePacket(BlockPos pos, UUID queueId) implements CustomPacketPayload {
    public static final Type<CancelPrintQueuePacket> TYPE = PayloadSupport.type("cancel_print_queue");
    public static final StreamCodec<FriendlyByteBuf, CancelPrintQueuePacket> STREAM_CODEC =
            CustomPacketPayload.codec(CancelPrintQueuePacket::write, CancelPrintQueuePacket::new);

    private CancelPrintQueuePacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readUUID());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeUUID(queueId);
    }

    @Override
    public Type<CancelPrintQueuePacket> type() {
        return TYPE;
    }
}
