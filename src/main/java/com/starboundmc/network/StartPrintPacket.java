package com.starboundmc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server request to print the recipe with the given id from the
 * printing station at the given position. The server revalidates materials
 * and wallet balance before anything is consumed.
 */
public record StartPrintPacket(BlockPos pos, net.minecraft.resources.ResourceLocation recipeId, int quantity)
        implements CustomPacketPayload {
    public static final Type<StartPrintPacket> TYPE = PayloadSupport.type("start_print");
    public static final StreamCodec<FriendlyByteBuf, StartPrintPacket> STREAM_CODEC =
            CustomPacketPayload.codec(StartPrintPacket::write, StartPrintPacket::new);

    private StartPrintPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readResourceLocation(), buffer.readVarInt());
    }

    public StartPrintPacket(BlockPos pos, net.minecraft.resources.ResourceLocation recipeId) {
        this(pos, recipeId, 1);
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeResourceLocation(recipeId);
        buffer.writeVarInt(quantity);
    }

    @Override
    public Type<StartPrintPacket> type() {
        return TYPE;
    }
}
