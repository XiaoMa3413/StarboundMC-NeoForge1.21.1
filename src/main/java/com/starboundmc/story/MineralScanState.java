package com.starboundmc.story;

import java.util.Locale;

/** Persisted phase of the shared introductory orbital mineral survey. */
public enum MineralScanState
{
    LOCKED("locked"),
    PENDING("pending"),
    SCANNING("scanning"),
    RESULT_REPORTED("result_reported"),
    COMPLETE("complete");

    private final String id;

    MineralScanState(String id)
    {
        this.id = id;
    }

    public String id()
    {
        return id;
    }

    public boolean isInProgress()
    {
        return this == PENDING || this == SCANNING || this == RESULT_REPORTED;
    }

    public static MineralScanState fromId(String id, MineralScanState fallback)
    {
        if (id == null)
            return fallback;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (MineralScanState state : values())
        {
            if (state.id.equals(normalized))
                return state;
        }
        return fallback;
    }
}
