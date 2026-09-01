package com.starboundmc.network;

import com.starboundmc.client.ClientVoxelMachineState;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyncVoxelMachinePacketTest {
    private static final BlockPos POS = new BlockPos(4, 65, -2);

    @BeforeEach
    void resetClientMachineState() {
        ClientVoxelMachineState.reset();
    }

    @Test
    void encodeDecodePreservesSnapshot() {
        SyncVoxelMachinePacket original = new SyncVoxelMachinePacket(POS, 37, 100, 15,
                ResourceLocation.fromNamespaceAndPath("starboundmc", "matter_manipulator_module"), 2);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SyncVoxelMachinePacket.STREAM_CODEC.encode(buffer, original);
            SyncVoxelMachinePacket decoded = SyncVoxelMachinePacket.STREAM_CODEC.decode(buffer);
            assertEquals(original, decoded);
            assertEquals(original.type(), decoded.type());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsInvalidProgressSnapshots() {
        assertThrows(IllegalArgumentException.class,
                () -> new SyncVoxelMachinePacket(POS, 101, 100, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SyncVoxelMachinePacket(POS, 0, 100, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new SyncVoxelMachinePacket(POS, 0, 100, 0,
                        ResourceLocation.withDefaultNamespace("air"), -1));
    }

    @Test
    void completedSnapshotClearsVisibleJob() {
        ClientVoxelMachineState.apply(new SyncVoxelMachinePacket(POS, 10, 20, 5));
        assertEquals(10, ClientVoxelMachineState.jobAt(POS).progress());

        ClientVoxelMachineState.apply(new SyncVoxelMachinePacket(POS, 0, 20, 5));
        assertNull(ClientVoxelMachineState.jobAt(POS));
        assertEquals(5, ClientVoxelMachineState.snapshotAt(POS).voxels());

        ClientVoxelMachineState.apply(new SyncVoxelMachinePacket(POS, 0, 20, 0));
        assertEquals(0, ClientVoxelMachineState.snapshotAt(POS).voxels());
    }
}
