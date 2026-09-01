package com.starboundmc.client;

import com.starboundmc.network.SyncVoxelMachinePacket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side mirror of voxel machine snapshots keyed by block position. */
public final class ClientVoxelMachineState {
    private static final Map<Long, SyncVoxelMachinePacket> JOBS = new ConcurrentHashMap<>();

    private ClientVoxelMachineState() {
    }

    public static void apply(SyncVoxelMachinePacket packet) {
        JOBS.put(packet.pos().asLong(), packet);
    }

    public static SyncVoxelMachinePacket jobAt(net.minecraft.core.BlockPos pos) {
        SyncVoxelMachinePacket snapshot = snapshotAt(pos);
        if (snapshot == null || snapshot.progress() <= 0) {
            return null;
        }
        return snapshot;
    }

    /** Includes completed refinery output even when no job is currently running. */
    public static SyncVoxelMachinePacket snapshotAt(net.minecraft.core.BlockPos pos) {
        return JOBS.get(pos.asLong());
    }

    public static void reset() {
        JOBS.clear();
    }
}
