package com.starboundmc.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaimRefinedVoxelsPacketTest {
    @Test
    void encodeDecodePreservesMachinePosition() {
        ClaimRefinedVoxelsPacket original = new ClaimRefinedVoxelsPacket(new BlockPos(-7, 68, 12));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClaimRefinedVoxelsPacket.STREAM_CODEC.encode(buffer, original);
            ClaimRefinedVoxelsPacket decoded = ClaimRefinedVoxelsPacket.STREAM_CODEC.decode(buffer);
            assertEquals(original, decoded);
            assertEquals(original.type(), decoded.type());
        } finally {
            buffer.release();
        }
    }
}
