package com.starboundmc.client;

import com.starboundmc.network.SyncVoxelMachinePacket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side mirror of voxel machine snapshots keyed by block position. */
public final class ClientVoxelMachineState {
    private static final Map<Long, SyncVoxelMachinePacket> JOBS = new ConcurrentHashMap<>();
    private static final Map<Long, Long> RECEIVED_GAME_TICKS = new ConcurrentHashMap<>();

    private ClientVoxelMachineState() {
    }

    public static void apply(SyncVoxelMachinePacket packet) {
        apply(packet, Long.MIN_VALUE);
    }

    public static void apply(SyncVoxelMachinePacket packet, long receivedGameTick) {
        JOBS.put(packet.pos().asLong(), packet);
        RECEIVED_GAME_TICKS.put(packet.pos().asLong(), receivedGameTick);
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

    /** Smoothly advances a sparse server snapshot without repeating partial-tick sawteeth. */
    public static float interpolatedRemainingTicksAt(
            net.minecraft.core.BlockPos pos, long currentGameTick, float partialTick) {
        SyncVoxelMachinePacket snapshot = snapshotAt(pos);
        if (snapshot == null || snapshot.progress() <= 0) {
            return 0.0F;
        }
        long receivedGameTick = RECEIVED_GAME_TICKS.getOrDefault(pos.asLong(), currentGameTick);
        if (receivedGameTick == Long.MIN_VALUE) {
            return snapshot.progress();
        }
        float ticksSinceSnapshot = Math.max(0L, currentGameTick - receivedGameTick)
                + Math.max(0.0F, partialTick);
        return Math.max(0.0F, snapshot.progress() - ticksSinceSnapshot);
    }

    public static void reset() {
        JOBS.clear();
        RECEIVED_GAME_TICKS.clear();
    }
}
