package com.starboundmc;

import com.starboundmc.item.MatterManipulatorUpgrades;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DataComponentContractTest {
    @Test
    void persistentCodecRoundTripsAllTracks() {
        MatterManipulatorUpgrades original = new MatterManipulatorUpgrades(3, 2, 1, 3);
        var encoded = MatterManipulatorUpgrades.CODEC
                .encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        assertEquals(original, MatterManipulatorUpgrades.CODEC
                .parse(NbtOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void streamCodecRoundTripsAllTracks() {
        MatterManipulatorUpgrades original = new MatterManipulatorUpgrades(1, 2, 2, 3);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            MatterManipulatorUpgrades.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, MatterManipulatorUpgrades.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void clampsEveryTrackAtItsPublishedBoundary() {
        assertEquals(new MatterManipulatorUpgrades(0, 3, 2, 0),
                new MatterManipulatorUpgrades(-4, 99, 8, -1));
    }

}
