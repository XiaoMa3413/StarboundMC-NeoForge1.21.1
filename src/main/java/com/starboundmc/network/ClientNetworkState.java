package com.starboundmc.network;

import java.util.List;

/** Main-thread client mirror populated exclusively by clientbound sync payloads. */
public final class ClientNetworkState {
    private static String planetId = "lush";
    private static String warpTargetId;
    private static String warpEntryId;
    private static int warpDurationTicks;
    private static int fuel;
    private static int maxFuel = 1;
    private static List<String> visited = List.of();
    private static String currentEntryId;
    private static TeleporterListPacket teleporterList = new TeleporterListPacket(List.of(), "");
    private static SyncFlightPacket flight;

    private ClientNetworkState() {
    }

    public static void resetConnectionState() {
        planetId = "lush";
        warpTargetId = null;
        warpEntryId = null;
        warpDurationTicks = 0;
        fuel = 0;
        maxFuel = 1;
        visited = List.of();
        currentEntryId = null;
        teleporterList = new TeleporterListPacket(List.of(), "");
        flight = null;
    }

    static void apply(SyncPlanetPacket payload) {
        planetId = payload.planetId();
    }

    static void apply(WarpStartPacket payload) {
        warpTargetId = payload.planetId();
        warpEntryId = payload.entryId().isEmpty() ? null : payload.entryId();
        warpDurationTicks = payload.durationTicks();
    }

    static void apply(SyncFuelPacket payload) {
        fuel = payload.fuel();
        maxFuel = payload.maxFuel();
    }

    static void apply(SyncStarStatePacket payload) {
        visited = payload.visited();
        currentEntryId = payload.currentEntryId();
    }

    static void apply(TeleporterListPacket payload) {
        teleporterList = payload;
    }

    static void apply(SyncFlightPacket payload) {
        if (flight == null || payload.revision() >= flight.revision()) {
            flight = payload;
        }
    }

    public static String planetId() {
        return planetId;
    }

    public static String warpTargetId() {
        return warpTargetId;
    }

    public static String warpEntryId() {
        return warpEntryId;
    }

    public static int warpDurationTicks() {
        return warpDurationTicks;
    }

    public static int fuel() {
        return fuel;
    }

    public static int maxFuel() {
        return maxFuel;
    }

    public static List<String> visited() {
        return visited;
    }

    public static String currentEntryId() {
        return currentEntryId;
    }

    public static TeleporterListPacket teleporterList() {
        return teleporterList;
    }

    public static SyncFlightPacket flight() {
        return flight;
    }
}
