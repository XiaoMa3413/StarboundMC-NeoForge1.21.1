// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RelayPacketsTest {
    @Test void everyEncounterAnnouncementCanBeEncodedAndLocalized() throws Exception {
        var source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/starboundmc/encounter/RelayEncounter.java"));
        var prefix = java.util.regex.Pattern.compile("new NovaBroadcastPacket\\(\"([^\"]+)\" \\+ key\\)").matcher(source);
        assertTrue(prefix.find(), "Missing relay broadcast construction");
        var calls = java.util.regex.Pattern.compile("announce\\(level, \"([^\"]+)\"\\)").matcher(source);
        int count = 0;
        while (calls.find()) {
            var packet = new NovaBroadcastPacket(prefix.group(1) + calls.group(1));
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                NovaBroadcastPacket.STREAM_CODEC.encode(buffer, packet);
                assertEquals(packet, NovaBroadcastPacket.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
            for (var language : java.util.List.of("en_us", "zh_cn")) {
                var json = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(
                        "src/main/resources/assets/starboundmc/lang/" + language + ".json"))).getAsJsonObject();
                assertTrue(json.has(packet.translationKey()), packet.translationKey());
            }
            count++;
        }
        assertTrue(count >= 8, "Expected all relay lifecycle announcements");
    }
    @Test void snapshotAndMenuBoundActionsRoundTrip() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var state = new RelaySnapshotPacket(5, new BlockPos(43, 94, 70), 160, "sys1:lush", true, false, 2);
            RelaySnapshotPacket.STREAM_CODEC.encode(buffer, state);
            assertEquals(state, RelaySnapshotPacket.STREAM_CODEC.decode(buffer));
            var action = new RelayActionPacket(7, true);
            RelayActionPacket.STREAM_CODEC.encode(buffer, action);
            assertEquals(action, RelayActionPacket.STREAM_CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally { buffer.release(); }
    }
}
