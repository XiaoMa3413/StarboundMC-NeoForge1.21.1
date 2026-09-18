// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.space;
import com.starboundmc.encounter.RelayData;
import com.starboundmc.network.RelaySnapshotPacket;
public final class RelayClientState {
    public static RelaySnapshotPacket snapshot;
    public static long receivedTick;
    private RelayClientState() { }
    public static void reset() { snapshot = null; receivedTick = 0; }
    public static boolean local() {
        return snapshot != null && (snapshot.phase() == RelayData.Phase.APPROACHING.ordinal() || snapshot.phase() == RelayData.Phase.ACTIVE.ordinal());
    }
}
