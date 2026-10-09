// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RelayPacketsTest {
    @Test void everyEncounterAnnouncementCanBeEncodedAndLocalized() throws Exception {
        for (var key : java.util.List.of("discovered", "recovered", "completed", "approaching",
                "no_space", "interrupted", "arrived", "departed")) {
            var packet = new NovaBroadcastPacket("message.starboundmc.nova.relay." + key);
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
        }
    }
    @Test void stablePhaseIdsAndUnknownWireValuesAreStrict() {
        var phases = java.util.List.of(com.starboundmc.encounter.RelayData.Phase.UNDISCOVERED,
                com.starboundmc.encounter.RelayData.Phase.AVAILABLE, com.starboundmc.encounter.RelayData.Phase.ROUTING,
                com.starboundmc.encounter.RelayData.Phase.APPROACHING, com.starboundmc.encounter.RelayData.Phase.MATERIALIZING,
                com.starboundmc.encounter.RelayData.Phase.ACTIVE, com.starboundmc.encounter.RelayData.Phase.LEAVING,
                com.starboundmc.encounter.RelayData.Phase.ERROR);
        for (int id = 0; id < phases.size(); id++) {
            assertEquals(id, phases.get(id).networkId());
            assertEquals(phases.get(id), com.starboundmc.encounter.RelayData.Phase.byNetworkId(id));
        }
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            RelaySnapshotPacket.STREAM_CODEC.encode(buffer, new RelaySnapshotPacket(phases.get(0), BlockPos.ZERO, 0, "", false, false, 0));
            buffer.setByte(0, 99);
            assertThrows(IllegalArgumentException.class, () -> RelaySnapshotPacket.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void snapshotAndMenuBoundActionsRoundTrip() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var state = new RelaySnapshotPacket(com.starboundmc.encounter.RelayData.Phase.ACTIVE, new BlockPos(43, 94, 70), 160, "sys1:lush", true, false, 2);
            RelaySnapshotPacket.STREAM_CODEC.encode(buffer, state);
            assertEquals(state, RelaySnapshotPacket.STREAM_CODEC.decode(buffer));
            var action = new RelayActionPacket(7, true);
            RelayActionPacket.STREAM_CODEC.encode(buffer, action);
            assertEquals(action, RelayActionPacket.STREAM_CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally { buffer.release(); }
    }
}
