package com.starboundmc.network;

import com.starboundmc.story.SituationTopic;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShipAiActionPacketTest
{
    @Test void taskActionsRoundTripAndRejectUnknownTasks() {
        for (var action : new ShipAiActionPacket.Action[]{ShipAiActionPacket.Action.CLAIM_TASK_REWARD,
                ShipAiActionPacket.Action.TRACK_TASK}) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                var packet = new ShipAiActionPacket(4, 12, action, 2);
                ShipAiActionPacket.STREAM_CODEC.encode(buffer, packet);
                assertEquals(packet, ShipAiActionPacket.STREAM_CODEC.decode(buffer));
                assertThrows(IllegalArgumentException.class, () -> new ShipAiActionPacket(4, 12, action, com.starboundmc.story.NovaTask.values().length));
            } finally { buffer.release(); }
        }
        assertEquals(-1, new ShipAiActionPacket(4, 12, ShipAiActionPacket.Action.TRACK_TASK, -1).argument());
        assertThrows(IllegalArgumentException.class, () ->
                new ShipAiActionPacket(4, 12, ShipAiActionPacket.Action.CLAIM_TASK_REWARD, -1));
    }
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
    void decoderRejectsRetiredAndUnknownActionIds()
    {
        for (int id : new int[]{4, 255})
        {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try
            {
                buffer.writeVarInt(4);
                buffer.writeVarLong(1L);
                buffer.writeByte(id);
                buffer.writeVarInt(0);
                assertThrows(IllegalArgumentException.class,
                        () -> ShipAiActionPacket.STREAM_CODEC.decode(buffer));
            }
            finally
            {
                buffer.release();
            }
        }
    }

    @Test
    void currentActionsKeepTheirExplicitWireIds()
    {
        var actions = java.util.Map.of(
                0, ShipAiActionPacket.Action.BEGIN_CORE_REBOOT,
                1, ShipAiActionPacket.Action.CONFIRM_IDENTITY,
                2, ShipAiActionPacket.Action.MARK_SITUATION_READ,
                3, ShipAiActionPacket.Action.ACTIVATE_SURFACE_MISSION,
                5, ShipAiActionPacket.Action.CLAIM_TASK_REWARD,
                6, ShipAiActionPacket.Action.TRACK_TASK);
        actions.forEach((id, action) -> {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                int argument = action == ShipAiActionPacket.Action.MARK_SITUATION_READ ? SituationTopic.INCIDENT.mask() : 0;
                var expected = new ShipAiActionPacket(4, 1L, action, argument);
                ShipAiActionPacket.STREAM_CODEC.encode(buffer, expected);
                assertEquals(4, buffer.readVarInt());
                assertEquals(1L, buffer.readVarLong());
                assertEquals(id.intValue(), buffer.readUnsignedByte());
                buffer.readerIndex(0);
                assertEquals(expected, ShipAiActionPacket.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        });
    }

    @Test
    void requestIdMustBePositive()
    {
        assertThrows(IllegalArgumentException.class, () ->
                new ShipAiActionPacket(4, 0L,
                        ShipAiActionPacket.Action.BEGIN_CORE_REBOOT, 0));
    }
}
