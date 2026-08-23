package com.starboundmc.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client visited destinations and current star-map entry. */
public record SyncStarStatePacket(List<String> visited, String currentEntryId)
        implements CustomPacketPayload {
    public static final Type<SyncStarStatePacket> TYPE = PayloadSupport.type("sync_star_state");
    public static final StreamCodec<FriendlyByteBuf, SyncStarStatePacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncStarStatePacket::write, SyncStarStatePacket::new);

    public SyncStarStatePacket {
        if (visited == null || visited.size() > PayloadSupport.MAX_LIST_ENTRIES) {
            throw new IllegalArgumentException("visited list is null or too large");
        }
        List<String> copy = new ArrayList<>(visited.size());
        for (String entryId : visited) {
            copy.add(PayloadSupport.requireString(
                    entryId, PayloadSupport.MAX_ID_LENGTH, "visited entry"));
        }
        visited = List.copyOf(copy);
        if (currentEntryId != null) {
            currentEntryId = PayloadSupport.requireString(
                    currentEntryId, PayloadSupport.MAX_ID_LENGTH, "currentEntryId");
        }
    }

    private SyncStarStatePacket(FriendlyByteBuf buffer) {
        this(readVisited(buffer), readNullableId(buffer));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(visited.size());
        for (String entryId : visited) {
            buffer.writeUtf(entryId, PayloadSupport.MAX_ID_LENGTH);
        }
        buffer.writeBoolean(currentEntryId != null);
        if (currentEntryId != null) {
            buffer.writeUtf(currentEntryId, PayloadSupport.MAX_ID_LENGTH);
        }
    }

    private static List<String> readVisited(FriendlyByteBuf buffer) {
        int count = PayloadSupport.readCount(
                buffer, PayloadSupport.MAX_LIST_ENTRIES, "visited");
        List<String> visited = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            visited.add(buffer.readUtf(PayloadSupport.MAX_ID_LENGTH));
        }
        return visited;
    }

    private static String readNullableId(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readUtf(PayloadSupport.MAX_ID_LENGTH) : null;
    }

    @Override
    public Type<SyncStarStatePacket> type() {
        return TYPE;
    }
}
