package com.starboundmc.network;

import com.starboundmc.story.SituationTopic;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShipAiActionPacketTest
{
    @Test
    void onlyReadActionAcceptsOneExactTopicBit()
    {
        ShipAiActionPacket packet =
                ShipAiActionPacket.markSituationRead(4, 1L, SituationTopic.INCIDENT);

        assertEquals(ShipAiActionPacket.Action.MARK_SITUATION_READ, packet.action());
        assertEquals(SituationTopic.INCIDENT, packet.situationTopic());
        assertThrows(IllegalArgumentException.class, () ->
                new ShipAiActionPacket(4, 1L, ShipAiActionPacket.Action.MARK_SITUATION_READ,
                        SituationTopic.NOVA_IDENTITY.mask() | SituationTopic.INCIDENT.mask()));
        assertThrows(IllegalArgumentException.class, () ->
                new ShipAiActionPacket(4, 1L, ShipAiActionPacket.Action.BEGIN_CORE_REBOOT, 1));
    }

    @Test
    void decoderRejectsUnknownActionId()
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            buffer.writeVarInt(4);
            buffer.writeVarLong(1L);
            buffer.writeByte(255);
            buffer.writeVarInt(0);
            assertThrows(IllegalArgumentException.class,
                    () -> ShipAiActionPacket.STREAM_CODEC.decode(buffer));
        }
        finally
        {
            buffer.release();
        }
    }

    @Test
    void requestIdMustBePositive()
    {
        assertThrows(IllegalArgumentException.class, () ->
                new ShipAiActionPacket(4, 0L,
                        ShipAiActionPacket.Action.BEGIN_CORE_REBOOT, 0));
    }
}
