package com.starboundmc.client.shipai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NovaPortraitActivityTest {
    @Test
    void broadcastKeysMapToSemanticPortraitStates() {
        assertEquals(NovaPortraitActivity.WARNING, NovaPortraitActivity.fromBroadcastKey(
                "message.starboundmc.nova.prologue.emergency"));
        assertEquals(NovaPortraitActivity.WARNING, NovaPortraitActivity.fromBroadcastKey(
                "message.starboundmc.nova.prologue.locate_terminal"));
        assertEquals(NovaPortraitActivity.SCANNING, NovaPortraitActivity.fromBroadcastKey(
                "message.starboundmc.nova.prologue.mineral_scan_started"));
        assertEquals(NovaPortraitActivity.CONFIRMATION, NovaPortraitActivity.fromBroadcastKey(
                "message.starboundmc.nova.prologue.core_online"));
        assertEquals(NovaPortraitActivity.CONFIRMATION, NovaPortraitActivity.fromBroadcastKey(
                "message.starboundmc.nova.prologue.mineral_scan_result"));
        assertEquals(NovaPortraitActivity.IDLE, NovaPortraitActivity.fromBroadcastKey(
                "message.starboundmc.nova.prologue.mineral_scan_sublight_hint"));
        assertEquals(NovaPortraitActivity.IDLE, NovaPortraitActivity.fromBroadcastKey(null));
    }
}
