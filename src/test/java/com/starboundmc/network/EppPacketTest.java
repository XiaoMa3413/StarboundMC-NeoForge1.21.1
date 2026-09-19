// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EppPacketTest {
    @Test void mobilityAndModuleActionsRoundTrip() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var click = new EppModuleClickPacket(3, 42, 37, 1);
            EppModuleClickPacket.STREAM_CODEC.encode(buffer, click);
            assertEquals(click, EppModuleClickPacket.STREAM_CODEC.decode(buffer));
            var boost = new JumpBoostPacket(-1, 1);
            JumpBoostPacket.STREAM_CODEC.encode(buffer, boost);
            assertEquals(boost, JumpBoostPacket.STREAM_CODEC.decode(buffer));
            var state = new MobilityStatePacket(true, false);
            MobilityStatePacket.STREAM_CODEC.encode(buffer, state);
            assertEquals(state, MobilityStatePacket.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void oxygenColdAndHeatSnapshotRoundTripIndependently() {
        var input = new EppSnapshotPacket(1080, 1080, 0, true, false, false, 2, 76, 3, 1, 34, 2, 0);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            EppSnapshotPacket.STREAM_CODEC.encode(buffer, input);
            assertEquals(input, EppSnapshotPacket.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void remotePlayersRetainTheirChassisGeneration() {
        var input = new EppVisualPacket(42, 2);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            EppVisualPacket.STREAM_CODEC.encode(buffer, input);
            assertEquals(input, EppVisualPacket.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
}
