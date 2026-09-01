package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client request to play one localized N.O.V.A. chat transmission. */
public record NovaBroadcastPacket(String translationKey) implements CustomPacketPayload
{
    private static final int MAX_TRANSLATION_KEY_LENGTH = 160;
    private static final String REQUIRED_PREFIX = "message.starboundmc.nova.";

    public static final Type<NovaBroadcastPacket> TYPE =
            PayloadSupport.type("nova_broadcast");
    public static final StreamCodec<FriendlyByteBuf, NovaBroadcastPacket> STREAM_CODEC =
            CustomPacketPayload.codec(NovaBroadcastPacket::write, NovaBroadcastPacket::new);

    public NovaBroadcastPacket
    {
        translationKey = PayloadSupport.requireString(
                translationKey, MAX_TRANSLATION_KEY_LENGTH, "translationKey");
        if (!translationKey.startsWith(REQUIRED_PREFIX))
            throw new IllegalArgumentException("Unsupported N.O.V.A. translation key " + translationKey);
    }

    private NovaBroadcastPacket(FriendlyByteBuf buffer)
    {
        this(buffer.readUtf(MAX_TRANSLATION_KEY_LENGTH));
    }

    private void write(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(translationKey, MAX_TRANSLATION_KEY_LENGTH);
    }

    @Override
    public Type<NovaBroadcastPacket> type()
    {
        return TYPE;
    }
}
