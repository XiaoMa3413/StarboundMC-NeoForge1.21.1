package com.starboundmc.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransporterEffectPacketTest {
    @Test void preservesDimensionFractionalDeckAndEffectKind() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (boolean device : new boolean[]{false, true}) for (boolean arrival : new boolean[]{false, true}) {
                var packet = new TransporterEffectPacket(ResourceLocation.parse("starboundmc:ship"),
                        new Vec3(-12.5, 101.5625, -6.54375), device, arrival);
                TransporterEffectPacket.STREAM_CODEC.encode(buffer, packet);
                assertEquals(packet, TransporterEffectPacket.STREAM_CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            }
        } finally { buffer.release(); }
    }
}
