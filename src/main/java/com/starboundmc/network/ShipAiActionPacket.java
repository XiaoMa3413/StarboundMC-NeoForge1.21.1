package com.starboundmc.network;

import com.starboundmc.story.SituationTopic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client intent sent while a specific ship AI terminal menu is open. */
public record ShipAiActionPacket(int containerId, long requestId,
                                 Action action, int argument)
        implements CustomPacketPayload
{
    public static final Type<ShipAiActionPacket> TYPE = PayloadSupport.type("ship_ai_action");
    public static final StreamCodec<FriendlyByteBuf, ShipAiActionPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ShipAiActionPacket::write, ShipAiActionPacket::new);

    public ShipAiActionPacket
    {
        if (containerId < 0)
            throw new IllegalArgumentException("containerId must be non-negative");
        if (requestId <= 0L)
            throw new IllegalArgumentException("requestId must be positive");
        if (action == null)
            throw new IllegalArgumentException("action cannot be null");
        if (action == Action.MARK_SITUATION_READ)
            SituationTopic.fromMask(argument);
        else if (argument != 0)
            throw new IllegalArgumentException(action + " does not accept an argument");
    }

    private ShipAiActionPacket(FriendlyByteBuf buffer)
    {
        this(buffer.readVarInt(), buffer.readVarLong(),
                Action.fromWireId(buffer.readUnsignedByte()), buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(containerId);
        buffer.writeVarLong(requestId);
        buffer.writeByte(action.wireId());
        buffer.writeVarInt(argument);
    }

    public static ShipAiActionPacket beginCoreReboot(int containerId, long requestId)
    {
        return new ShipAiActionPacket(containerId, requestId, Action.BEGIN_CORE_REBOOT, 0);
    }

    public static ShipAiActionPacket confirmIdentity(int containerId, long requestId)
    {
        return new ShipAiActionPacket(containerId, requestId, Action.CONFIRM_IDENTITY, 0);
    }

    public static ShipAiActionPacket markSituationRead(int containerId, long requestId,
                                                       SituationTopic topic)
    {
        return new ShipAiActionPacket(containerId, requestId,
                Action.MARK_SITUATION_READ, topic.mask());
    }

    public static ShipAiActionPacket activateSurfaceMission(int containerId, long requestId)
    {
        return new ShipAiActionPacket(containerId, requestId,
                Action.ACTIVATE_SURFACE_MISSION, 0);
    }

    public SituationTopic situationTopic()
    {
        return action == Action.MARK_SITUATION_READ
                ? SituationTopic.fromMask(argument) : null;
    }

    @Override
    public Type<ShipAiActionPacket> type()
    {
        return TYPE;
    }

    public enum Action
    {
        BEGIN_CORE_REBOOT(0),
        CONFIRM_IDENTITY(1),
        MARK_SITUATION_READ(2),
        ACTIVATE_SURFACE_MISSION(3);

        private final int wireId;

        Action(int wireId)
        {
            this.wireId = wireId;
        }

        public int wireId()
        {
            return wireId;
        }

        public static Action fromWireId(int wireId)
        {
            for (Action action : values())
            {
                if (action.wireId == wireId)
                    return action;
            }
            throw new IllegalArgumentException("Unknown ship AI action id " + wireId);
        }
    }
}
