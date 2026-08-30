package com.starboundmc.story;

import java.util.Locale;

/** Persisted repair state for one ship propulsion system. */
public enum EngineState
{
    DAMAGED("damaged"),
    ONLINE("online");

    private final String id;

    EngineState(String id)
    {
        this.id = id;
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
}
