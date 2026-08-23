package com.starboundmc.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client destination list for the currently open teleporter. */
public record TeleporterListPacket(List<Entry> entries, String currentName)
        implements CustomPacketPayload {
    public static final Type<TeleporterListPacket> TYPE = PayloadSupport.type("teleporter_list");
    public static final StreamCodec<FriendlyByteBuf, TeleporterListPacket> STREAM_CODEC =
            CustomPacketPayload.codec(TeleporterListPacket::write, TeleporterListPacket::new);

    public TeleporterListPacket {
        if (entries == null || entries.size() > PayloadSupport.MAX_LIST_ENTRIES) {
            throw new IllegalArgumentException("teleporter entries are null or too large");
        }
        entries = List.copyOf(entries);
        currentName = PayloadSupport.requireString(
                currentName, PayloadSupport.MAX_NAME_LENGTH, "currentName");
    }

    private TeleporterListPacket(FriendlyByteBuf buffer) {
        this(readEntries(buffer), buffer.readUtf(PayloadSupport.MAX_NAME_LENGTH));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buffer.writeByte(entry.type);
            buffer.writeUtf(entry.key, PayloadSupport.MAX_ID_LENGTH);
            buffer.writeUtf(entry.label, PayloadSupport.MAX_NAME_LENGTH);
        }
        buffer.writeUtf(currentName, PayloadSupport.MAX_NAME_LENGTH);
    }

    private static List<Entry> readEntries(FriendlyByteBuf buffer) {
        int count = PayloadSupport.readCount(
                buffer, PayloadSupport.MAX_LIST_ENTRIES, "teleporter entries");
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new Entry(buffer.readByte(),
                    buffer.readUtf(PayloadSupport.MAX_ID_LENGTH),
                    buffer.readUtf(PayloadSupport.MAX_NAME_LENGTH)));
        }
        return entries;
    }

    @Override
    public Type<TeleporterListPacket> type() {
        return TYPE;
    }

    /** type: 0 = ship, 1 = planet surface, 2 = named teleporter. */
    public record Entry(byte type, String key, String label) {
        public Entry {
            if (type < 0 || type > 2) {
                throw new IllegalArgumentException("Unknown teleporter destination type " + type);
            }
            key = PayloadSupport.requireString(key, PayloadSupport.MAX_ID_LENGTH, "key");
            label = PayloadSupport.requireString(label, PayloadSupport.MAX_NAME_LENGTH, "label");
        }

        public Entry(int type, String key, String label) {
            this((byte) type, key, label);
        }
    }
}
