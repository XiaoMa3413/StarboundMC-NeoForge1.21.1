package com.starboundmc.story;

import java.util.Locale;

/** Shared core-power state for the single world ship. */
public enum CoreState
{
    OFFLINE("offline"),
    REBOOTING("rebooting"),
    ONLINE("online");

    private final String id;

    CoreState(String id)
    {
        this.id = id;
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
}
