package com.starboundmc.network;

import com.starboundmc.client.ClientTeleporterState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> Client: the destination list for the open teleporter UI.
 * Each entry is {type, key, label}: type 0 = ship, 1 = planet surface, 2 = named teleporter.
 */
public class TeleporterListPacket
{
    private final List<String[]> entries;
    private final String currentName;

    public TeleporterListPacket(List<String[]> entries, String currentName)
    {
        this.entries = entries;
        this.currentName = currentName;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(entries.size());
        for (String[] entry : entries)
        {
            buf.writeByte(Integer.parseInt(entry[0]));
            buf.writeUtf(entry[1]);
            buf.writeUtf(entry[2]);
        }
        buf.writeUtf(currentName);
    }

    public static TeleporterListPacket decode(FriendlyByteBuf buf)
    {
        int size = buf.readVarInt();
        List<String[]> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            entries.add(new String[] { Integer.toString(buf.readByte()), buf.readUtf(), buf.readUtf() });
        }
        return new TeleporterListPacket(entries, buf.readUtf());
    }

    public static void handle(TeleporterListPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> ClientTeleporterState.receive(msg.entries, msg.currentName));
        ctx.get().setPacketHandled(true);
    }
}
