package com.starboundmc.story;

import java.util.Locale;

/** Shared core-power state for the single world ship. */
public enum CoreState
{
    OFFLINE(0, "offline"),
    REBOOTING(1, "rebooting"),
    ONLINE(2, "online");

    private final int networkId;
    private final String id;

    CoreState(int networkId, String id)
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

    public static CoreState fromId(String id, CoreState fallback)
    {
        if (id == null)
            return fallback;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (CoreState state : values())
        {
            if (state.id.equals(normalized))
                return state;
        }
        return fallback;
    }

    public static CoreState fromNetworkId(int networkId)
    {
        for (CoreState state : values())
        {
            if (state.networkId == networkId)
                return state;
        }
        throw new IllegalArgumentException("Unknown core state id " + networkId);
    }
}
