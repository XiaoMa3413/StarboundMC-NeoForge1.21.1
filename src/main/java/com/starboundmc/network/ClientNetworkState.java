package com.starboundmc.network;

/** Connection-scoped fuel projection shared by the client UIs. */
public final class ClientNetworkState {
    private static int fuel;
    private static int maxFuel = 1;

    private ClientNetworkState() {}

    public static void resetConnectionState() {
        fuel = 0;
        maxFuel = 1;
    }

    static void apply(SyncFuelPacket payload) {
        fuel = payload.fuel();
        maxFuel = payload.maxFuel();
    }

    public static int fuel() { return fuel; }
    public static int maxFuel() { return maxFuel; }
}
