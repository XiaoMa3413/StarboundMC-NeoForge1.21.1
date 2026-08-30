package com.starboundmc.story;

import java.util.Locale;

/** Shared state of the introductory planetary-surface objective. */
public enum SurfaceMissionState
{
    LOCKED(0, "locked"),
    ACTIVE(1, "active"),
    COMPLETE(2, "complete");

    private final int networkId;
    private final String id;

    SurfaceMissionState(int networkId, String id)
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

    public static SurfaceMissionState fromId(String id, SurfaceMissionState fallback)
    {
        if (id == null)
            return fallback;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (SurfaceMissionState state : values())
        {
            if (state.id.equals(normalized))
                return state;
        }
        return fallback;
    }

    public static SurfaceMissionState fromNetworkId(int networkId)
    {
        for (SurfaceMissionState state : values())
        {
            if (state.networkId == networkId)
                return state;
        }
        throw new IllegalArgumentException("Unknown surface mission state id " + networkId);
    }
}
