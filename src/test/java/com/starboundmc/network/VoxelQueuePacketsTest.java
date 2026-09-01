package com.starboundmc.network;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VoxelQueuePacketsTest {
    private static final BlockPos POS = new BlockPos(9, 72, -4);
    private static final ResourceLocation RECIPE =
            ResourceLocation.fromNamespaceAndPath("starboundmc", "print_matter_manipulator_module");

    @Test
    void startPrintPreservesRequestedQuantity() {
        assertRoundTrip(new StartPrintPacket(POS, RECIPE, 64),
                StartPrintPacket.STREAM_CODEC);
    }

    @Test
    void cancellationRoundTrips() {
        assertRoundTrip(new CancelPrintQueuePacket(POS, UUID.randomUUID()),
                CancelPrintQueuePacket.STREAM_CODEC);
    }

    @Test
    void stopRefinementPreservesMachinePosition() {
        assertRoundTrip(new StopRefinementPacket(POS), StopRefinementPacket.STREAM_CODEC);
    }

    @Test
    void publicQueueSnapshotPreservesRequesterAndOrder() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ResourceLocation result = ResourceLocation.fromNamespaceAndPath(
                "starboundmc", "matter_manipulator_module");
        SyncPrintQueuePacket packet = new SyncPrintQueuePacket(POS, List.of(
                new SyncPrintQueuePacket.Entry(UUID.randomUUID(), first, "Ada", result, 2, 1, true),
                new SyncPrintQueuePacket.Entry(UUID.randomUUID(), second, "Lin", result, 2, 5, false)));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SyncPrintQueuePacket.STREAM_CODEC.encode(buffer, packet);
            SyncPrintQueuePacket decoded = SyncPrintQueuePacket.STREAM_CODEC.decode(buffer);
            assertEquals(packet, decoded);
            assertEquals(6, decoded.outstandingCrafts());
            assertEquals(first, decoded.entries().get(0).requesterId());
            assertEquals(second, decoded.entries().get(1).requesterId());
        } finally {
            buffer.release();
        }
    }

    @Test
    void queueSnapshotRejectsInvalidCraftCountsAndOversizedLists() {
        ResourceLocation result = ResourceLocation.withDefaultNamespace("stone");
        assertThrows(IllegalArgumentException.class, () -> new SyncPrintQueuePacket.Entry(
                UUID.randomUUID(), UUID.randomUUID(), "Player", result, 1, 0, false));
        SyncPrintQueuePacket.Entry entry = new SyncPrintQueuePacket.Entry(
                UUID.randomUUID(), UUID.randomUUID(), "Player", result, 1, 1, false);
        assertThrows(IllegalArgumentException.class, () -> new SyncPrintQueuePacket(
                POS, java.util.Collections.nCopies(SyncPrintQueuePacket.MAX_ENTRIES + 1, entry)));
    }

    private static <T> void assertRoundTrip(
            T packet, net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, T> codec) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buffer, packet);
            assertEquals(packet, codec.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
