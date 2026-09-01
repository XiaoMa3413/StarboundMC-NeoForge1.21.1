package com.starboundmc.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client public snapshot of a printing station's active and queued work. */
public record SyncPrintQueuePacket(BlockPos pos, List<Entry> entries)
        implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 65;
    public static final Type<SyncPrintQueuePacket> TYPE = PayloadSupport.type("sync_print_queue");
    public static final StreamCodec<FriendlyByteBuf, SyncPrintQueuePacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncPrintQueuePacket::write, SyncPrintQueuePacket::new);

    public SyncPrintQueuePacket {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many printing queue entries: " + entries.size());
        }
    }

    private SyncPrintQueuePacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), readEntries(buffer));
    }

    private static List<Entry> readEntries(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid printing queue snapshot size: " + size);
        }
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(buffer.readUUID(), buffer.readUUID(), buffer.readUtf(64),
                    buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readBoolean()));
        }
        return entries;
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buffer.writeUUID(entry.id());
            buffer.writeUUID(entry.requesterId());
            buffer.writeUtf(entry.requesterName(), 64);
            buffer.writeResourceLocation(entry.resultItemId());
            buffer.writeVarInt(entry.resultCount());
            buffer.writeVarInt(entry.crafts());
            buffer.writeBoolean(entry.active());
        }
    }

    public int outstandingCrafts() {
        return entries.stream().mapToInt(Entry::crafts).sum();
    }

    @Override
    public Type<SyncPrintQueuePacket> type() {
        return TYPE;
    }

    public record Entry(UUID id, UUID requesterId, String requesterName,
                        ResourceLocation resultItemId, int resultCount, int crafts, boolean active) {
        public Entry {
            if (id == null || requesterId == null || requesterName == null
                    || requesterName.length() > 64 || resultItemId == null
                    || resultCount <= 0 || crafts <= 0 || crafts > 64) {
                throw new IllegalArgumentException("Invalid printing queue entry");
            }
        }
    }
}
