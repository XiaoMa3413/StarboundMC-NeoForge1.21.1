package com.starboundmc.client;

/** Available space-rendering presets. Custom delegates to the user's feature toggles. */
public enum SpaceVisualQuality
{
    PERFORMANCE,
    BALANCED,
    HIGH,
    ULTRA,
    CUSTOM;

    public boolean cloudsEnabled(boolean customValue)
    {
        return switch (this)
        {
            case PERFORMANCE -> false;
            case BALANCED, HIGH, ULTRA -> true;
            case CUSTOM -> customValue;
        };
    }

    public boolean atmosphereEnabled(boolean customValue)
    {
        return switch (this)
        {
            case PERFORMANCE, BALANCED, HIGH, ULTRA -> true;
            case CUSTOM -> customValue;
        };
    }

    public boolean stellarViewStarsEnabled(boolean customValue)
    {
        return switch (this)
        {
            case PERFORMANCE -> false;
            case BALANCED, HIGH, ULTRA -> true;
            case CUSTOM -> customValue;
        };
    }
}
