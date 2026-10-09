package com.starboundmc.warp;

/** High-level flight phases with explicit protocol ids independent of declaration order. */
public enum FlightPhase {
    DOCKED(0), TURN(1), ACCELERATE(2), HYPERSPACE(3), CRUISE(4), DECELERATE(5), ARRIVE(6);

    private final int networkId;
    FlightPhase(int networkId) { this.networkId = networkId; }
    public int networkId() { return networkId; }

    public static FlightPhase byNetworkId(int id) {
        return switch (id) {
            case 0 -> DOCKED;
            case 1 -> TURN;
            case 2 -> ACCELERATE;
            case 3 -> HYPERSPACE;
            case 4 -> CRUISE;
            case 5 -> DECELERATE;
            case 6 -> ARRIVE;
            default -> throw new IllegalArgumentException("Unknown flight phase id " + id);
        };
    }
}
