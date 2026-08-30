package com.starboundmc.story;

import java.util.Locale;

/** Shared state of the introductory planetary-surface objective. */
public enum SurfaceMissionState
{
    LOCKED("locked"),
    ACTIVE("active"),
    COMPLETE("complete");

    private final String id;

    SurfaceMissionState(String id)
    {
        this.id = id;
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
}
