package com.starboundmc.warp;

/** Clear high-level phases of the simplified virtual flight. */
public enum FlightPhase
{
    DOCKED,
    TURN,
    ACCELERATE,
    HYPERSPACE,
    CRUISE,
    DECELERATE,
    ARRIVE;

    public static FlightPhase byNetworkId(int id)
    {
        FlightPhase[] values=values();
        return id>=0&&id<values.length?values[id]:DOCKED;
    }
}
