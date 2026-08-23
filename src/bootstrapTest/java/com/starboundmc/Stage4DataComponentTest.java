package com.starboundmc;

import com.starboundmc.item.MatterManipulatorItem;
import com.starboundmc.item.MatterManipulatorUpgrades;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class Stage4DataComponentTest {
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

    @Test
    void migratesAndRemovesLegacyFieldsOnce() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(MatterManipulatorItem.NBT_LEGACY_UPGRADES, 2);
        tag.putInt(MatterManipulatorItem.NBT_RANGE_UPGRADES, 3);
        tag.putInt(MatterManipulatorItem.NBT_MINING_UPGRADES, 7);
        tag.putInt(MatterManipulatorItem.NBT_FORTUNE_UPGRADES, 2);
        tag.put("Enchantments", StringTag.valueOf("legacy"));
        tag.putString("Unrelated", "keep");

        assertEquals(new MatterManipulatorUpgrades(2, 3, 2, 2),
                MatterManipulatorItem.migrateLegacyTag(tag));
        assertFalse(tag.contains(MatterManipulatorItem.NBT_LEGACY_UPGRADES));
        assertFalse(tag.contains(MatterManipulatorItem.NBT_RANGE_UPGRADES));
        assertFalse(tag.contains(MatterManipulatorItem.NBT_MINING_UPGRADES));
        assertFalse(tag.contains(MatterManipulatorItem.NBT_FORTUNE_UPGRADES));
        assertFalse(tag.contains("Enchantments"));
        assertEquals("keep", tag.getString("Unrelated"));
    }
}
