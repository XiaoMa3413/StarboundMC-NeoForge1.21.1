// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import com.starboundmc.network.EppSnapshotPacket;

public final class EppClientState {
    public static EppSnapshotPacket snapshot;
    private EppClientState() { }
    public static void reset() { snapshot = null; OxygenHudLayer.INSTANCE.reset(); }
}
