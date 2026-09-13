package com.starboundmc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Acknowledges this player's submission, independently of other players' queue updates. */
public record PrintSubmissionResultPacket(int containerId, BlockPos pos, boolean accepted)
        implements CustomPacketPayload {
    public static final Type<PrintSubmissionResultPacket> TYPE = PayloadSupport.type("print_submission_result");
    public static final StreamCodec<FriendlyByteBuf, PrintSubmissionResultPacket> STREAM_CODEC =
            CustomPacketPayload.codec(PrintSubmissionResultPacket::write, PrintSubmissionResultPacket::new);

    private PrintSubmissionResultPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readBlockPos(), buffer.readBoolean());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(accepted);
    }

    @Override
    public Type<PrintSubmissionResultPacket> type() { return TYPE; }
}
