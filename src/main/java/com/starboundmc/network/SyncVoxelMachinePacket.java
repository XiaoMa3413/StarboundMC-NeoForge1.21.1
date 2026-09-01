package com.starboundmc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client machine job snapshot for voxel machines without a full
 * container data layer: current progress, total duration and pending voxels
 * for the block entity at the given position.
 */
public record SyncVoxelMachinePacket(BlockPos pos, int progress, int totalTicks, int voxels,
                                     ResourceLocation resultItemId, int resultCount)
        implements CustomPacketPayload {
    private static final ResourceLocation NO_RESULT =
            ResourceLocation.fromNamespaceAndPath("minecraft", "air");
    public static final Type<SyncVoxelMachinePacket> TYPE = PayloadSupport.type("sync_voxel_machine");
    public static final StreamCodec<FriendlyByteBuf, SyncVoxelMachinePacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncVoxelMachinePacket::write, SyncVoxelMachinePacket::new);

    public SyncVoxelMachinePacket {
        if (totalTicks < 0 || progress < 0 || progress > Math.max(totalTicks, 1)
                || voxels < 0 || resultItemId == null || resultCount < 0) {
            throw new IllegalArgumentException("Invalid voxel machine snapshot " + progress + "/" + totalTicks);
        }
    }

    public SyncVoxelMachinePacket(BlockPos pos, int progress, int totalTicks, int voxels) {
        this(pos, progress, totalTicks, voxels, NO_RESULT, 0);
    }

    private SyncVoxelMachinePacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readResourceLocation(), buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(progress);
        buffer.writeVarInt(totalTicks);
        buffer.writeVarInt(voxels);
        buffer.writeResourceLocation(resultItemId);
        buffer.writeVarInt(resultCount);
    }

    @Override
    public Type<SyncVoxelMachinePacket> type() {
        return TYPE;
    }
}
