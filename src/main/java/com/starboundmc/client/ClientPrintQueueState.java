package com.starboundmc.client;

import com.starboundmc.network.SyncPrintQueuePacket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

/** Client-side mirror of public printing queue snapshots keyed by station position. */
public final class ClientPrintQueueState {
    private static final Map<Long, SyncPrintQueuePacket> QUEUES = new ConcurrentHashMap<>();

    private ClientPrintQueueState() {
    }

    public static void apply(SyncPrintQueuePacket packet) {
        QUEUES.put(packet.pos().asLong(), packet);
    }

    public static SyncPrintQueuePacket snapshotAt(BlockPos pos) {
        return QUEUES.get(pos.asLong());
    }

    public static void reset() {
        QUEUES.clear();
    }
}
