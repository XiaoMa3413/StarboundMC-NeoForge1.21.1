package com.starboundmc.story;

import java.util.Locale;

/** Persisted repair state for one ship propulsion system. */
public enum EngineState
{
    DAMAGED(0, "damaged"),
    ONLINE(1, "online");

    private final int networkId;
    private final String id;

    EngineState(int networkId, String id)
    {
        this.networkId = networkId;
        this.id = id;
    }

    public int networkId()
    {
        return networkId;
    }

    public String id()
    {
        return id;
    }

    public static EngineState fromId(String id, EngineState fallback)
    {
        if (id == null)
            return fallback;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (EngineState state : values())
        {
            if (state.id.equals(normalized))
                return state;
        }
        return fallback;
    }

    public static EngineState fromNetworkId(int networkId)
    {
        for (EngineState state : values())
        {
            if (state.networkId == networkId)
                return state;
        }
        throw new IllegalArgumentException("Unknown engine state id " + networkId);
    }
}
